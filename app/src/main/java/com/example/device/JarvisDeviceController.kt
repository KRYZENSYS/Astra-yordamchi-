package com.example.device

import android.app.ActivityManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import com.example.model.DeviceTelemetrySnapshot
import com.example.service.AstrafolioBackgroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val TAG = "JarvisDeviceController"

class JarvisDeviceController(private val context: Context) : SensorEventListener {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _mediaVolumePercent = MutableStateFlow(getCurrentVolumePercent())
    val mediaVolumePercent: StateFlow<Int> = _mediaVolumePercent.asStateFlow()

    private val _telemetry = MutableStateFlow(readSystemSnapshot(0f, 0f, 9.8f, 0f))
    val telemetry: StateFlow<DeviceTelemetrySnapshot> = _telemetry.asStateFlow()

    private val _lastExecutedDeviceAction = MutableStateFlow<String?>(null)
    val lastExecutedDeviceAction: StateFlow<String?> = _lastExecutedDeviceAction.asStateFlow()

    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 9.8f
    private var lastAzimuth = 0f

    init {
        registerSensors()
        refreshTelemetry()
    }

    private fun registerSensors() {
        try {
            val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val mag = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            if (accel != null) {
                sensorManager?.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
            }
            if (mag != null) {
                sensorManager?.registerListener(this, mag, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sensor registration failed", e)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccelX = event.values[0]
                lastAccelY = event.values[1]
                lastAccelZ = event.values[2]
                val mag = sqrt(
                    lastAccelX * lastAccelX +
                        lastAccelY * lastAccelY +
                        lastAccelZ * lastAccelZ
                )
                _telemetry.value = _telemetry.value.copy(
                    accelX = lastAccelX,
                    accelY = lastAccelY,
                    accelZ = lastAccelZ,
                    gForceMagnitude = mag
                )
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val mx = event.values[0]
                val my = event.values[1]
                val deg = ((Math.toDegrees(atan2(my.toDouble(), mx.toDouble())) + 360.0) % 360.0).toFloat()
                lastAzimuth = deg
                _telemetry.value = _telemetry.value.copy(azimuthDeg = deg)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun refreshTelemetry() {
        _telemetry.value = readSystemSnapshot(lastAccelX, lastAccelY, lastAccelZ, lastAzimuth)
        _mediaVolumePercent.value = getCurrentVolumePercent()
    }

    private fun readSystemSnapshot(ax: Float, ay: Float, az: Float, azimuth: Float): DeviceTelemetrySnapshot {
        var batteryPct = 100
        var tempC = 29.0f
        var voltage = 4100
        var charging = false

        try {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryPct = ((level * 100f) / scale).roundToInt()
                }
                val rawTemp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 290)
                tempC = rawTemp / 10f
                voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4100)
                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading battery telemetry", e)
        }

        var usedRam = 2048L
        var totalRam = 4096L
        var ramPct = 50
        try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            if (memInfo.totalMem > 0) {
                totalRam = memInfo.totalMem / (1024 * 1024)
                val availRam = memInfo.availMem / (1024 * 1024)
                usedRam = (totalRam - availRam).coerceAtLeast(0L)
                ramPct = ((usedRam * 100f) / totalRam).roundToInt().coerceIn(1, 100)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading memory telemetry", e)
        }

        var freeGb = 24f
        var totalGb = 64f
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val availBytes = stat.availableBlocksLong * stat.blockSizeLong
            totalGb = totalBytes / (1024f * 1024f * 1024f)
            freeGb = availBytes / (1024f * 1024f * 1024f)
        } catch (e: Exception) {
            Log.w(TAG, "Error reading storage telemetry", e)
        }

        val gForce = sqrt(ax * ax + ay * ay + az * az)
        return DeviceTelemetrySnapshot(
            batteryPercent = batteryPct,
            temperatureCelsius = tempC,
            voltageMv = voltage,
            isCharging = charging,
            usedRamMb = usedRam,
            totalRamMb = totalRam,
            ramUsagePercent = ramPct,
            freeStorageGb = freeGb,
            totalStorageGb = totalGb,
            accelX = ax,
            accelY = ay,
            accelZ = az,
            gForceMagnitude = gForce,
            azimuthDeg = azimuth
        )
    }

    private fun getCurrentVolumePercent(): Int {
        val am = audioManager ?: return 70
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((cur * 100f) / max).roundToInt().coerceIn(0, 100)
    }

    fun setMediaVolumePercent(percent: Int): String {
        val clamped = percent.coerceIn(0, 100)
        try {
            val am = audioManager
            if (am != null) {
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val targetIndex = ((clamped / 100f) * max).roundToInt().coerceIn(0, max)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set media volume", e)
        }
        _mediaVolumePercent.value = clamped
        val msg = "Media ovoz balandligi $clamped% ga sozlandi"
        _lastExecutedDeviceAction.value = msg
        return msg
    }

    fun setTorchEnabled(enabled: Boolean): String {
        return try {
            val cm = cameraManager
            val cameraId = cm?.cameraIdList?.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cm?.cameraIdList?.firstOrNull()

            if (cm != null && cameraId != null) {
                cm.setTorchMode(cameraId, enabled)
                _isTorchOn.value = enabled
                val msg = if (enabled) "Telefon fonarigi (Torch) YOQILDI" else "Telefon fonarigi (Torch) O'CHIRILDI"
                _lastExecutedDeviceAction.value = msg
                msg
            } else {
                _isTorchOn.value = enabled
                val msg = if (enabled) "Fonarik signali faollashtirildi (Simulator)" else "Fonarik signali o'chirildi"
                _lastExecutedDeviceAction.value = msg
                msg
            }
        } catch (e: Exception) {
            Log.w(TAG, "Torch toggle fallback", e)
            _isTorchOn.value = enabled
            val msg = if (enabled) "Fonarik rejimi yoqildi" else "Fonarik rejimi o'chirildi"
            _lastExecutedDeviceAction.value = msg
            msg
        }
    }

    /**
     * Fast local hardware & background command pre-detector so unambiguous Uzbek/English voice commands
     * trigger phone hardware immediately with 0ms latency while the AI Council prepares its full response.
     */
    fun tryExecuteInstantHardwareCommand(rawCommand: String): Pair<String, String>? {
        val lower = rawCommand.lowercase(Locale.ROOT)
        val mentionsTorch = lower.contains("fonarik") || lower.contains("chiroq") || lower.contains("torch") || lower.contains("flashlight")
        val mentionsBg = lower.contains("orqa fon") || lower.contains("background")
        val mentionsSecretary = lower.contains("avto-kotib") || lower.contains("avto kotib") || lower.contains("avtomatik javob")
        val isOffIntent = lower.contains("o'chir") || lower.contains("ochir") ||
            lower.contains("oʻchir") || lower.contains("o‘chir") || lower.contains("to'xtat") ||
            Regex("\\boff\\b").containsMatchIn(lower)
        val isOnIntent = lower.contains("yoq") || lower.contains("ishga tushir") || Regex("\\bon\\b").containsMatchIn(lower)

        val mentionsApi = lower.contains("api") || lower.contains("klaster") || lower.contains("ai studio") || lower.contains("kalit")
        val mentionsLocalServer = lower.contains("lokal server") || lower.contains("local server") || lower.contains("serverni")
        val mentionsSleep = lower.contains("uxla") || lower.contains("uyqu") || lower.contains("sleep")
        val mentionsWake = lower.contains("uyg'ot") || lower.contains("uygot") || lower.contains("uygʻot") || lower.contains("wake")
        val mentionsRotate = lower.contains("almashtir") || lower.contains("keyingi api") || lower.contains("rotate")
        val mentionsToken = lower.contains("token")
        val mentionsReset = lower.contains("tozala") || lower.contains("nolga") || lower.contains("0 ga") || lower.contains("reset")

        return when {
            mentionsTorch && isOffIntent -> "TORCH_OFF" to setTorchEnabled(false)
            mentionsTorch && isOnIntent -> "TORCH_ON" to setTorchEnabled(true)
            mentionsBg && isOffIntent -> "STOP_BACKGROUND_MODE" to stopBackgroundMode()
            mentionsBg && isOnIntent -> "START_BACKGROUND_MODE" to startBackgroundMode()
            mentionsSecretary && isOffIntent -> "DISABLE_AUTO_CALL_RESPONDER" to setAutoCallResponder(false)
            mentionsSecretary && isOnIntent -> "ENABLE_AUTO_CALL_RESPONDER" to setAutoCallResponder(true)
            mentionsLocalServer && isOffIntent -> {
                val msg = "Lokal HTTP va Internet Server to'xtatildi"
                _lastExecutedDeviceAction.value = msg
                "STOP_LOCAL_SERVER" to msg
            }
            mentionsLocalServer && isOnIntent -> {
                val msg = "Lokal HTTP va Internet Server (Port 8080) ishga tushirildi"
                _lastExecutedDeviceAction.value = msg
                "START_LOCAL_SERVER" to msg
            }
            mentionsToken && mentionsReset -> {
                val msg = "Barcha API kalitlarining token hisoblagichlari 0 ga tushirildi"
                _lastExecutedDeviceAction.value = msg
                "RESET_TOKEN_COUNTERS" to msg
            }
            mentionsToken -> {
                val msg = "Token sarfi hisoboti o'qib eshittirilmoqda"
                _lastExecutedDeviceAction.value = msg
                "SPEAK_TOKEN_REPORT" to msg
            }
            mentionsApi && mentionsSleep -> {
                val msg = "Qo'shimcha AI Studio API uzellari uyqu (standby) rejimiga o'tkazildi"
                _lastExecutedDeviceAction.value = msg
                "SLEEP_STANDBY_APIS" to msg
            }
            mentionsApi && mentionsWake -> {
                val msg = "Barcha AI Studio API uzellari uyqudan uyg'otildi"
                _lastExecutedDeviceAction.value = msg
                "WAKE_ALL_APIS" to msg
            }
            mentionsApi && mentionsRotate -> {
                val msg = "Keyingi AI Studio API uzeliga navbat berildi"
                _lastExecutedDeviceAction.value = msg
                "ROTATE_API_NODE" to msg
            }
            lower.contains("ovozni o'chir") || lower.contains("ovozni ochir") || lower.contains("ovozni oʻchir") || lower.contains("bezvuk") -> {
                "MUTE_AUDIO" to setMediaVolumePercent(0)
            }
            lower.contains("kamerani och") || lower.contains("rasmga ol") -> {
                "OPEN_CAMERA" to launchCamera()
            }
            else -> null
        }
    }

    fun executeAiDeviceAction(actionCode: String, parameter: String): String? {
        val normalized = actionCode.trim().uppercase(Locale.ROOT)
        if (normalized.isEmpty() || normalized == "NONE") return null

        return when (normalized) {
            "TORCH_ON" -> setTorchEnabled(true)
            "TORCH_OFF" -> setTorchEnabled(false)
            "VOLUME_UP" -> {
                val target = parameter.filter { it.isDigit() }.toIntOrNull()
                    ?: (_mediaVolumePercent.value + 25).coerceAtMost(100)
                setMediaVolumePercent(target)
            }
            "VOLUME_DOWN" -> {
                val target = parameter.filter { it.isDigit() }.toIntOrNull()
                    ?: (_mediaVolumePercent.value - 25).coerceAtLeast(10)
                setMediaVolumePercent(target)
            }
            "MUTE_AUDIO" -> setMediaVolumePercent(0)
            "MAX_AUDIO" -> setMediaVolumePercent(100)
            "SET_ALARM" -> launchAlarmIntent(parameter)
            "SET_TIMER" -> launchTimerIntent(parameter)
            "DIAL_NUMBER" -> launchDialer(parameter)
            "SEND_SMS" -> {
                val parts = parameter.split("|", limit = 2)
                val num = parts.getOrNull(0).orEmpty().ifBlank { "+998901234567" }
                val body = parts.getOrNull(1).orEmpty().ifBlank { "Assalomu alaykum! Astrafolio orqali yuborildi." }
                launchSmsComposer(num, body)
            }
            "OPEN_CAMERA" -> launchCamera()
            "OPEN_MAPS" -> launchMapsNavigation(parameter)
            "OPEN_WIFI" -> launchSystemSettings(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi sozlamalari ochildi")
            "OPEN_BLUETOOTH" -> launchSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth sozlamalari ochildi")
            "OPEN_DISPLAY" -> launchSystemSettings(Settings.ACTION_DISPLAY_SETTINGS, "Displey sozlamalari ochildi")
            "OPEN_SOUND_SETTINGS" -> launchSystemSettings(Settings.ACTION_SOUND_SETTINGS, "Ovoz sozlamalari ochildi")
            "OPEN_LOCATION_SETTINGS" -> launchSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Joylashuv (GPS) sozlamalari ochildi")
            "START_BACKGROUND_MODE" -> startBackgroundMode()
            "STOP_BACKGROUND_MODE" -> stopBackgroundMode()
            "ENABLE_AUTO_CALL_RESPONDER" -> setAutoCallResponder(true)
            "DISABLE_AUTO_CALL_RESPONDER" -> setAutoCallResponder(false)
            "WEB_SEARCH" -> launchWebSearch(parameter)
            else -> null
        }
    }

    fun startBackgroundMode(): String {
        val msg = AstrafolioBackgroundService.startService(context)
        _lastExecutedDeviceAction.value = msg
        return msg
    }

    fun stopBackgroundMode(): String {
        val msg = AstrafolioBackgroundService.stopService(context)
        _lastExecutedDeviceAction.value = msg
        return msg
    }

    fun setAutoCallResponder(enabled: Boolean): String {
        val msg = AstrafolioBackgroundService.setAutoCallResponderEnabled(enabled)
        _lastExecutedDeviceAction.value = msg
        return msg
    }

    fun launchDialer(phoneNumber: String): String {
        val cleanNum = phoneNumber.filter { it.isDigit() || it == '+' }.ifBlank { "+998" }
        try {
            val intent = Intent(Intent.ACTION_DIAL, "tel:$cleanNum".toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch dialer", e)
        }
        val msg = "Raqam terish oynasi ochildi: $cleanNum"
        _lastExecutedDeviceAction.value = msg
        return msg
    }

    fun launchSmsComposer(phoneNumber: String, messageBody: String): String {
        val cleanNum = phoneNumber.trim().ifBlank { "+998" }
        try {
            val intent = Intent(Intent.ACTION_SENDTO, "smsto:${Uri.encode(cleanNum)}".toUri()).apply {
                putExtra("sms_body", messageBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch SMS composer", e)
        }
        val msg = "$cleanNum raqamiga SMS javob oynasi ochildi"
        _lastExecutedDeviceAction.value = msg
        return msg
    }

    fun launchCamera(): String {
        try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch camera", e)
        }
        val msg = "Telefon kamerasi ishga tushirildi"
        _lastExecutedDeviceAction.value = msg
        return msg
    }

    fun launchMapsNavigation(destinationQuery: String): String {
        val query = destinationQuery.ifBlank { "Toshkent" }
        try {
            val intent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=${Uri.encode(query)}".toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch maps", e)
        }
        val msg = "Xarita va navigatsiya ochildi: $query"
        _lastExecutedDeviceAction.value = msg
        return msg
    }

    fun launchAlarmIntent(timeParam: String): String {
        val parts = timeParam.split(":")
        val hour = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull()?.coerceIn(0, 23) ?: 7
        val minute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()?.coerceIn(0, 59) ?: 30
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Astrafolio Jarvis Wake-Up")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch alarm activity", e)
        }
        val formatted = String.format(Locale.US, "%02d:%02d", hour, minute)
        val msg = "Soat $formatted ga budilnik o'rnatish buyrug'i bajarildi"
        _lastExecutedDeviceAction.value = msg
        return msg
    }

    fun launchTimerIntent(secondsParam: String): String {
        val seconds = secondsParam.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(10) ?: 300
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Astrafolio Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch timer activity", e)
        }
        val mins = seconds / 60
        val msg = "${if (mins > 0) "$mins daqiqalik" else "$seconds soniyalik"} taymer ishga tushirildi"
        _lastExecutedDeviceAction.value = msg
        return msg
    }

    fun launchSystemSettings(settingsAction: String, label: String): String {
        try {
            val intent = Intent(settingsAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open system settings: $settingsAction", e)
        }
        _lastExecutedDeviceAction.value = label
        return label
    }

    fun launchWebSearch(query: String): String {
        val cleanQuery = query.ifBlank { "AI technology news" }
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, cleanQuery)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    "https://www.google.com/search?q=${Uri.encode(cleanQuery)}".toUri()
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch web search", e)
        }
        val msg = "Brauzerda qidirildi: \"$cleanQuery\""
        _lastExecutedDeviceAction.value = msg
        return msg
    }

    fun release() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering sensors", e)
        }
    }
}
