package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.cloud.AstrafolioCloudRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "AstrafolioBgService"

class AstrafolioBackgroundService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            tts = TextToSpeech(applicationContext, this)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to init background TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val uzResult = engine.setLanguage(Locale.forLanguageTag("uz-UZ"))
            if (uzResult == TextToSpeech.LANG_MISSING_DATA || uzResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                val trResult = engine.setLanguage(Locale.forLanguageTag("tr-TR"))
                if (trResult == TextToSpeech.LANG_MISSING_DATA || trResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.US)
                }
            }
            isTtsReady = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_FOREGROUND -> {
                _isRunning.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_HANDLE_INCOMING_CALL -> {
                val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "Noma'lum raqam"
                val callerLabel = intent.getStringExtra(EXTRA_CALLER_LABEL) ?: "Kiruvchi qo'ng'iroq"
                handleIncomingCallAutomatically(callerNumber, callerLabel)
            }
            else -> {
                _isRunning.value = true
                startForeground(NOTIFICATION_ID, buildForegroundNotification())
            }
        }
        return START_STICKY
    }

    private fun handleIncomingCallAutomatically(callerNumber: String, callerLabel: String) {
        val statusPreset = _currentStatusPreset.value
        val replyMessage = buildDefaultUzbekAutoReply(callerNumber, statusPreset, _customReplyMessage.value)

        _screenedCallsCount.value = _screenedCallsCount.value + 1
        _lastScreenedCallInfo.value = "$callerNumber ($statusPreset): $replyMessage"

        if (isTtsReady) {
            tts?.speak(replyMessage, TextToSpeech.QUEUE_FLUSH, null, "call_reply_${System.currentTimeMillis()}")
        }

        // Update notification with the latest auto-answered call info
        if (_isRunning.value) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIFICATION_ID, buildForegroundNotification("Avto-javob berildi: $callerNumber"))
        }

        // Persist to Firestore if authenticated
        if (Firebase.auth.currentUser != null) {
            serviceScope.launch {
                try {
                    val repo = AstrafolioCloudRepository(applicationContext)
                    repo.createCallLog(
                        callerNumber = callerNumber,
                        callerLabel = callerLabel,
                        statusPreset = statusPreset,
                        aiGeneratedReply = replyMessage,
                        autoAnswered = true
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not log call to Firestore from background service", e)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Astrafolio 24/7 Orqa Fon & Avto-Kotib",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ovozli telefon boshqaruvi va kiruvchi qo'ng'iroqlarga AI avto-kotib xizmati"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(subStatus: String? = null): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AstrafolioBackgroundService::class.java).apply {
            action = ACTION_STOP_FOREGROUND
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val secretaryStatus = if (_autoCallResponderEnabled.value) {
            "AI Avto-Kotib YOQILGAN (${_currentStatusPreset.value})"
        } else {
            "Ovozli Qo'riqchi Faol"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_jarvis_hologram)
            .setContentTitle("Astrafolio 24/7 Orqa Fon Tizimi Faol")
            .setContentText(subStatus ?: "$secretaryStatus • Javob berilgan qo'ng'iroqlar: ${_screenedCallsCount.value}")
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, "HUD Ochish", openPending)
            .addAction(0, "To'xtatish", stopPending)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        _isRunning.value = false
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down service TTS", e)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "astrafolio_bg_channel"
        const val NOTIFICATION_ID = 4091
        const val ACTION_START_FOREGROUND = "com.example.action.START_ASTRAFOLIO_BG"
        const val ACTION_STOP_FOREGROUND = "com.example.action.STOP_ASTRAFOLIO_BG"
        const val ACTION_HANDLE_INCOMING_CALL = "com.example.action.HANDLE_INCOMING_CALL"
        const val EXTRA_CALLER_NUMBER = "extra_caller_number"
        const val EXTRA_CALLER_LABEL = "extra_caller_label"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _autoCallResponderEnabled = MutableStateFlow(true)
        val autoCallResponderEnabled: StateFlow<Boolean> = _autoCallResponderEnabled.asStateFlow()

        private val _currentStatusPreset = MutableStateFlow("Uchrashuvdaman")
        val currentStatusPreset: StateFlow<String> = _currentStatusPreset.asStateFlow()

        private val _customReplyMessage = MutableStateFlow(
            "Assalomu alaykum! Siz Astrafolio sun'iy intellekt kotibi bilan bog'landingiz. Xo'jayinim hozir band, bo'shashi bilan sizga qayta qo'ng'iroq qiladi."
        )
        val customReplyMessage: StateFlow<String> = _customReplyMessage.asStateFlow()

        private val _screenedCallsCount = MutableStateFlow(0)
        val screenedCallsCount: StateFlow<Int> = _screenedCallsCount.asStateFlow()

        private val _lastScreenedCallInfo = MutableStateFlow<String?>(null)
        val lastScreenedCallInfo: StateFlow<String?> = _lastScreenedCallInfo.asStateFlow()

        fun startService(context: Context): String {
            return try {
                val intent = Intent(context, AstrafolioBackgroundService::class.java).apply {
                    action = ACTION_START_FOREGROUND
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                _isRunning.value = true
                "Astrafolio 24/7 Orqa Fon Xizmati YOQILDI"
            } catch (e: Exception) {
                Log.w(TAG, "Could not start foreground service", e)
                _isRunning.value = true
                "Orqa fon rejimi faollashtirildi"
            }
        }

        fun stopService(context: Context): String {
            return try {
                val intent = Intent(context, AstrafolioBackgroundService::class.java).apply {
                    action = ACTION_STOP_FOREGROUND
                }
                context.stopService(intent)
                _isRunning.value = false
                "Astrafolio 24/7 Orqa Fon Xizmati TO'XTATILDI"
            } catch (e: Exception) {
                Log.w(TAG, "Could not stop foreground service", e)
                _isRunning.value = false
                "Orqa fon rejimi to'xtatildi"
            }
        }

        fun setAutoCallResponderEnabled(enabled: Boolean): String {
            _autoCallResponderEnabled.value = enabled
            return if (enabled) {
                "Qo'ng'iroqlarga AI Avto-Javobgar (Avto-Kotib) YOQILDI"
            } else {
                "Qo'ng'iroqlarga AI Avto-Javobgar O'CHIRILDI"
            }
        }

        fun setStatusPreset(preset: String) {
            _currentStatusPreset.value = preset
        }

        fun setCustomReplyMessage(message: String) {
            _customReplyMessage.value = message
        }

        fun recordAutoAnsweredCall(summary: String) {
            _screenedCallsCount.value = _screenedCallsCount.value + 1
            _lastScreenedCallInfo.value = summary
        }

        fun buildDefaultUzbekAutoReply(
            callerNumber: String,
            statusPreset: String,
            customTemplate: String
        ): String {
            if (customTemplate.isNotBlank() && statusPreset == "Maxsus AI Shablon") {
                return customTemplate
            }
            return when (statusPreset) {
                "Uchrashuvdaman" ->
                    "Assalomu alaykum! Siz Astrafolio sun'iy intellekt kotibi bilan bog'landingiz. Xo'jayinim hozir muhim uchrashuvda, uchrashuv tugashi bilan $callerNumber raqamiga qayta qo'ng'iroq qiladi."
                "Mashina haydayapman" ->
                    "Assalomu alaykum! Astrafolio AI kotibi javob bermoqda: Xo'jayinim hozir rulda yo'lda ketmoqda. Xavfsizlik uchun qo'ng'irog'ingiz saqlandi, manzilga yetgach siz bilan bog'lanadi."
                "Kod yozyapman" ->
                    "Assalomu alaykum! Xo'jayinim hozir chuqur kodlash va loyiha rejimida. Muhim xabaringiz bo'lsa SMS yuboring, Astrafolio AI buni darhol yetkazadi."
                "Dam olyapman" ->
                    "Assalomu alaykum! Xo'jayinim hozir dam olish vaqtida. Qo'ng'irog'ingiz Astrafolio bulut jurnaliga yozib olindi."
                else -> customTemplate
            }
        }
    }
}
