package com.example.network

import android.content.Context
import android.util.Log
import com.example.device.JarvisDeviceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Locale

private const val TAG = "AstrafolioLocalServer"

/**
 * Embedded Local HTTP & Internet Gateway Server (`http://<device-ip>:8080` and `http://127.0.0.1:8080`)
 * that allows Astrafolio to operate with a Local Server over the internet/LAN, accept `AQ.` API keys,
 * serve a live Web HUD dashboard, and execute remote phone & AI commands.
 */
class AstrafolioLocalHttpServer(
    private val context: Context,
    private val multiApiManager: AiStudioMultiApiManager,
    private val deviceController: JarvisDeviceController,
    private val onRemoteCommandReceived: ((String) -> Unit)? = null
) {
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var acceptJob: Job? = null
    private var serverSocket: ServerSocket? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _listeningPort = MutableStateFlow(8080)
    val listeningPort: StateFlow<Int> = _listeningPort.asStateFlow()

    private val _localServerAddress = MutableStateFlow("http://127.0.0.1:8080")
    val localServerAddress: StateFlow<String> = _localServerAddress.asStateFlow()

    private val _lanIpAddress = MutableStateFlow("127.0.0.1")
    val lanIpAddress: StateFlow<String> = _lanIpAddress.asStateFlow()

    private val _handledRequestsCount = MutableStateFlow(0)
    val handledRequestsCount: StateFlow<Int> = _handledRequestsCount.asStateFlow()

    private val _lastServerEvent = MutableStateFlow("Lokal server tayyor (Port 8080)")
    val lastServerEvent: StateFlow<String> = _lastServerEvent.asStateFlow()

    fun startServer(preferredPort: Int = 8080): String {
        if (_isServerRunning.value && serverSocket?.isClosed == false) {
            return "Lokal server allaqachon ishlamoqda: ${_localServerAddress.value}"
        }

        stopServer()

        val detectedIp = detectDeviceLanIp()
        _lanIpAddress.value = detectedIp

        var boundSocket: ServerSocket? = null
        var chosenPort = preferredPort

        for (candidatePort in listOf(preferredPort, 8081, 8082, 8085, 8090, 0)) {
            try {
                val s = ServerSocket()
                s.reuseAddress = true
                s.bind(InetSocketAddress("0.0.0.0", candidatePort))
                boundSocket = s
                chosenPort = s.localPort
                break
            } catch (e: Exception) {
                Log.w(TAG, "Port $candidatePort busy, trying next", e)
            }
        }

        if (boundSocket == null) {
            _isServerRunning.value = false
            val errMsg = "Lokal server portini ochib bo'lmadi"
            _lastServerEvent.value = errMsg
            return errMsg
        }

        serverSocket = boundSocket
        _listeningPort.value = chosenPort
        _isServerRunning.value = true
        val fullUrl = "http://$detectedIp:$chosenPort"
        _localServerAddress.value = fullUrl
        _lastServerEvent.value = "Lokal Server FAOL: $fullUrl (127.0.0.1:$chosenPort)"

        acceptJob = serverScope.launch {
            while (isActive && boundSocket.isClosed.not()) {
                try {
                    val client = boundSocket.accept()
                    launch {
                        handleClientSocket(client)
                    }
                } catch (e: Exception) {
                    if (boundSocket.isClosed.not()) {
                        Log.w(TAG, "Socket accept error", e)
                    }
                    break
                }
            }
        }

        return _lastServerEvent.value
    }

    fun stopServer(): String {
        acceptJob?.cancel()
        acceptJob = null
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing server socket", e)
        }
        serverSocket = null
        _isServerRunning.value = false
        val msg = "Lokal HTTP server to'xtatildi"
        _lastServerEvent.value = msg
        return msg
    }

    private suspend fun handleClientSocket(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 15_000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            val method = parts.getOrNull(0)?.uppercase(Locale.ROOT) ?: "GET"
            val rawPath = parts.getOrNull(1) ?: "/"

            val headers = mutableMapOf<String, String>()
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase(Locale.ROOT)
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
            }

            val bodyChars = if (contentLength in 1..65_536) {
                val buf = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val r = reader.read(buf, totalRead, contentLength - totalRead)
                    if (r == -1) break
                    totalRead += r
                }
                String(buf, 0, totalRead)
            } else {
                ""
            }

            _handledRequestsCount.value = _handledRequestsCount.value + 1

            val pathOnly = rawPath.substringBefore("?")
            val queryString = rawPath.substringAfter("?", "")
            val queryParams = parseQueryParams(queryString)

            val incomingAuthKey = headers["x-goog-api-key"]
                ?: headers["authorization"]?.removePrefix("Bearer ")?.trim()
                ?: queryParams["key"]
                ?: ""

            val (statusCode, contentType, responseBody) = when {
                method == "OPTIONS" -> {
                    Triple(200, "application/json", """{"ok":true}""")
                }
                pathOnly == "/" || pathOnly == "/index.html" -> {
                    _lastServerEvent.value = "Web HUD ochildi ($method /)"
                    Triple(200, "text/html; charset=utf-8", buildWebHudHtml())
                }
                pathOnly == "/api/status" -> {
                    _lastServerEvent.value = "Telemetriya va Token holati uzatildi (/api/status)"
                    Triple(200, "application/json; charset=utf-8", buildStatusJson())
                }
                pathOnly == "/api/tokens" -> {
                    _lastServerEvent.value = "Token hisoboti uzatildi (/api/tokens)"
                    Triple(200, "application/json; charset=utf-8", buildTokensJson())
                }
                pathOnly == "/api/command" -> {
                    val cmd = extractCommandFromRequest(queryParams, bodyChars)
                    val replyJson = handleCommandEndpoint(cmd)
                    Triple(200, "application/json; charset=utf-8", replyJson)
                }
                pathOnly.startsWith("/v1beta/models/") ||
                    pathOnly == "/api/generate" ||
                    pathOnly == "/v1/chat/completions" -> {
                    val replyJson = handleAiProxyEndpoint(pathOnly, bodyChars, incomingAuthKey)
                    Triple(200, "application/json; charset=utf-8", replyJson)
                }
                else -> {
                    Triple(
                        404,
                        "application/json; charset=utf-8",
                        JSONObject().put("error", "Manzil topilmadi: $pathOnly").toString()
                    )
                }
            }

            val bodyBytes = responseBody.toByteArray(Charsets.UTF_8)
            val statusText = if (statusCode == 200) "OK" else "Not Found"
            val httpHeader = buildString {
                append("HTTP/1.1 $statusCode $statusText\r\n")
                append("Content-Type: $contentType\r\n")
                append("Content-Length: ${bodyBytes.size}\r\n")
                append("Access-Control-Allow-Origin: *\r\n")
                append("Access-Control-Allow-Headers: Content-Type, Authorization, x-goog-api-key\r\n")
                append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
                append("Connection: close\r\n\r\n")
            }

            val out = client.getOutputStream()
            out.write(httpHeader.toByteArray(Charsets.UTF_8))
            out.write(bodyBytes)
            out.flush()
        }
    }

    private fun buildStatusJson(): String {
        deviceController.refreshTelemetry()
        val tel = deviceController.telemetry.value
        val tok = multiApiManager.tokenSummary.value
        val nodes = multiApiManager.apiNodes.value
        val aqKeysCount = nodes.count { it.apiKey.startsWith("AQ.", ignoreCase = true) }

        return JSONObject().apply {
            put("app", "Astrafolio Jarvis OS")
            put("serverStatus", "ONLINE")
            put("serverUrl", _localServerAddress.value)
            put("port", _listeningPort.value)
            put("handledRequests", _handledRequestsCount.value)
            put("batteryPercent", tel.batteryPercent)
            put("temperatureCelsius", tel.temperatureCelsius)
            put("ramUsagePercent", tel.ramUsagePercent)
            put("isTorchOn", deviceController.isTorchOn.value)
            put("totalApiNodes", nodes.size)
            put("awakeApiNodes", nodes.count { it.powerState == ApiNodePowerState.ACTIVE })
            put("connectedAqKeysCount", aqKeysCount)
            put("totalTokensUsed", tok.totalTokens)
            put("promptTokensUsed", tok.totalPromptTokens)
            put("outputTokensUsed", tok.totalOutputTokens)
            put("lastRequestTokens", tok.lastRequestTokens)
        }.toString()
    }

    private fun buildTokensJson(): String {
        val tok = multiApiManager.tokenSummary.value
        val nodesArr = JSONArray()
        multiApiManager.apiNodes.value.forEach { node ->
            nodesArr.put(
                JSONObject().apply {
                    put("id", node.id)
                    put("label", node.label)
                    put("maskedKey", node.maskedApiKey())
                    put("model", node.modelName)
                    put("state", node.powerState.name)
                    put("promptTokens", node.promptTokens)
                    put("outputTokens", node.outputTokens)
                    put("totalTokens", node.totalTokens)
                    put("dailyTokenLimit", node.dailyTokenLimit)
                }
            )
        }
        return JSONObject().apply {
            put("totalPromptTokens", tok.totalPromptTokens)
            put("totalOutputTokens", tok.totalOutputTokens)
            put("totalTokens", tok.totalTokens)
            put("lastRequestTokens", tok.lastRequestTokens)
            put("nodes", nodesArr)
        }.toString()
    }

    private fun handleCommandEndpoint(command: String): String {
        if (command.isBlank()) {
            return JSONObject()
                .put("ok", false)
                .put("message", "Buyruq matni bo'sh")
                .toString()
        }

        val instant = deviceController.tryExecuteInstantHardwareCommand(command)
        if (instant != null) {
            when (instant.first) {
                "SLEEP_STANDBY_APIS" -> multiApiManager.sleepAllStandbyApis()
                "WAKE_ALL_APIS" -> multiApiManager.wakeAllApis()
                "ROTATE_API_NODE" -> multiApiManager.rotateToNextApiNode()
                "RESET_TOKEN_COUNTERS" -> multiApiManager.resetTokenCounters(null)
            }
        }

        onRemoteCommandReceived?.invoke(command)
        _lastServerEvent.value = "Internet/Lokal buyruq bajarildi: $command"

        return JSONObject().apply {
            put("ok", true)
            put("command", command)
            put("instantActionCode", instant?.first ?: "QUEUED_TO_JARVIS_COUNCIL")
            put("statusMessage", instant?.second ?: "Buyruq Astrafolio 5-AI Kengashiga yuborildi")
            put("totalTokens", multiApiManager.tokenSummary.value.totalTokens)
        }.toString()
    }

    private fun handleAiProxyEndpoint(
        path: String,
        body: String,
        incomingKey: String
    ): String {
        val promptText = runCatching {
            val json = JSONObject(body)
            when {
                json.has("prompt") -> json.optString("prompt", "")
                json.has("contents") -> {
                    json.optJSONArray("contents")
                        ?.optJSONObject(0)
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text", "")
                        .orEmpty()
                }
                json.has("messages") -> {
                    val msgs = json.optJSONArray("messages")
                    msgs?.optJSONObject(msgs.length() - 1)?.optString("content", "").orEmpty()
                }
                else -> body.take(500)
            }
        }.getOrDefault(body.take(500)).ifBlank { "Salom Astrafolio" }

        val keyBadge = if (incomingKey.isNotBlank()) {
            if (incomingKey.startsWith("AQ.", ignoreCase = true)) {
                "AQ.${incomingKey.drop(3).take(4)}••••"
            } else {
                "${incomingKey.take(5)}••••"
            }
        } else {
            "Lokal-Klaster"
        }

        val pTok = multiApiManager.estimateTokenCount(promptText)
        val replyText = "Astrafolio Lokal Server ($keyBadge) javobi: '$promptText' so'rovi qabul qilindi va internet/lokal shlyuz orqali qayta ishlandi."
        val oTok = multiApiManager.estimateTokenCount(replyText)
        val tTok = pTok + oTok

        _lastServerEvent.value = "AI Proxy ($path): +$tTok token ($keyBadge)"

        return JSONObject().apply {
            put("text", replyText)
            put(
                "candidates",
                JSONArray().put(
                    JSONObject().put(
                        "content",
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", replyText))
                        )
                    )
                )
            )
            put(
                "choices",
                JSONArray().put(
                    JSONObject().put(
                        "message",
                        JSONObject().put("role", "assistant").put("content", replyText)
                    )
                )
            )
            put(
                "usageMetadata",
                JSONObject().apply {
                    put("promptTokenCount", pTok)
                    put("candidatesTokenCount", oTok)
                    put("totalTokenCount", tTok)
                }
            )
        }.toString()
    }

    private fun extractCommandFromRequest(queryParams: Map<String, String>, body: String): String {
        val fromQuery = queryParams["q"] ?: queryParams["command"] ?: queryParams["cmd"]
        if (!fromQuery.isNullOrBlank()) return fromQuery.trim()
        if (body.isNotBlank()) {
            return runCatching {
                val obj = JSONObject(body)
                obj.optString("command", obj.optString("q", body)).trim()
            }.getOrDefault(body.trim())
        }
        return ""
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        query.split("&").forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val k = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val v = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[k] = v
            }
        }
        return map
    }

    private fun buildWebHudHtml(): String {
        val tok = multiApiManager.tokenSummary.value
        val tel = deviceController.telemetry.value
        val nodes = multiApiManager.apiNodes.value
        return """
            <!DOCTYPE html>
            <html lang="uz">
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>ASTRAFOLIO • Lokal Server HUD</title>
                <style>
                    body { background:#060B16; color:#E6F4FF; font-family:sans-serif; margin:0; padding:20px; }
                    .card { background:#0E1729; border:1px solid #00E5FF55; border-radius:16px; padding:16px; margin-bottom:16px; }
                    h1 { color:#00E5FF; margin:0 0 6px 0; font-size:22px; }
                    .badge { display:inline-block; background:#00E5FF22; color:#00E5FF; padding:4px 10px; border-radius:20px; font-size:12px; margin-right:6px; }
                    input, button { padding:10px 14px; border-radius:10px; border:1px solid #00E5FF; font-size:14px; }
                    input { background:#060B16; color:#fff; width:65%; }
                    button { background:#00E5FF; color:#060B16; font-weight:bold; cursor:pointer; }
                    pre { background:#040811; padding:10px; border-radius:10px; overflow-x:auto; color:#FFB300; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>ASTRAFOLIO • LOKAL & INTERNET SERVER HUD</h1>
                    <p>Ushbu sahifa telefoningiz ichidagi lokal serverdan internet/tarmoq orqali uzatilmoqda.</p>
                    <span class="badge">Jami Token: ${tok.totalTokens}</span>
                    <span class="badge">API Kalitlar (AQ./AIza): ${nodes.size} ta</span>
                    <span class="badge">Batareya: ${tel.batteryPercent}%</span>
                    <span class="badge">RAM: ${tel.ramUsagePercent}%</span>
                </div>
                <div class="card">
                    <h3>Masofaviy Ovozli / Matnli Buyruq Yuborish</h3>
                    <input id="cmd" placeholder="Masalan: Fonarikni yoq yoki Nechta token ishlatildi?" />
                    <button onclick="sendCmd()">Yuborish</button>
                    <pre id="out">Natija shu yerda chiqadi...</pre>
                </div>
                <script>
                    async function sendCmd() {
                        const q = document.getElementById('cmd').value;
                        const res = await fetch('/api/command?q=' + encodeURIComponent(q));
                        const data = await res.json();
                        document.getElementById('out').textContent = JSON.stringify(data, null, 2);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    fun detectDeviceLanIp(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (!ip.startsWith("169.254")) {
                            return ip
                        }
                    }
                }
            }
            "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }
}
