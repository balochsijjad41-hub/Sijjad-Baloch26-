package com.example.service

import com.example.data.model.ServerEndpoint
import com.example.data.model.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class JarvisServerManager(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
) {
    private val defaultServers = listOf(
        ServerEndpoint(
            id = "asia-se1",
            name = "Stark Orbital Relay (Asia)",
            region = "Singapore (asia-se1)",
            pingUrl = "https://generativelanguage.googleapis.com"
        ),
        ServerEndpoint(
            id = "us-central1",
            name = "Stark Tower Mainframe (US)",
            region = "Iowa (us-central1)",
            pingUrl = "https://www.google.com/generate_204"
        ),
        ServerEndpoint(
            id = "europe-west1",
            name = "Helicarrier Core (Europe)",
            region = "Belgium (europe-west1)",
            pingUrl = "https://connectivitycheck.gstatic.com/generate_204"
        ),
        ServerEndpoint(
            id = "global-edge",
            name = "Tactical Weather Satellite",
            region = "Global Edge",
            pingUrl = "https://api.open-meteo.com/v1/forecast?latitude=0&longitude=0"
        ),
        ServerEndpoint(
            id = "on-device-quantum",
            name = "On-Device Quantum Neural Core",
            region = "Local Hardware",
            pingUrl = "local",
            latencyMs = 2L,
            status = ServerStatus.OPTIMAL
        )
    )

    private val _servers = MutableStateFlow(defaultServers)
    val servers: StateFlow<List<ServerEndpoint>> = _servers.asStateFlow()

    private val _activeServer = MutableStateFlow(defaultServers.first())
    val activeServer: StateFlow<ServerEndpoint> = _activeServer.asStateFlow()

    private val _isTestingServers = MutableStateFlow(false)
    val isTestingServers: StateFlow<Boolean> = _isTestingServers.asStateFlow()

    var autoSwitchEnabled: Boolean = true

    suspend fun benchmarkAllServers(): ServerEndpoint = withContext(Dispatchers.IO) {
        _isTestingServers.value = true

        val updated = _servers.value.map { server ->
            if (server.pingUrl == "local") {
                server.copy(latencyMs = 2L, status = ServerStatus.OPTIMAL)
            } else {
                val ping = testPing(server.pingUrl)
                val status = when {
                    ping < 0 -> ServerStatus.OFFLINE
                    ping < 120 -> ServerStatus.OPTIMAL
                    ping < 300 -> ServerStatus.STABLE
                    else -> ServerStatus.HIGH_LATENCY
                }
                server.copy(latencyMs = if (ping < 0) 999L else ping, status = status)
            }
        }

        _servers.value = updated

        // Select best server (lowest latency among online servers)
        val onlineServers = updated.filter { it.status != ServerStatus.OFFLINE && it.pingUrl != "local" }
        val best = onlineServers.minByOrNull { it.latencyMs } ?: updated.first()

        if (autoSwitchEnabled) {
            _activeServer.value = best.copy(isAutoSelected = true)
        }

        _isTestingServers.value = false
        best
    }

    fun manuallySelectServer(server: ServerEndpoint) {
        _activeServer.value = server.copy(isAutoSelected = false)
    }

    private fun testPing(urlStr: String): Long {
        return try {
            val start = System.currentTimeMillis()
            val request = Request.Builder().url(urlStr).head().build()
            val response = client.newCall(request).execute()
            val end = System.currentTimeMillis()
            response.close()
            maxOf(end - start, 8L)
        } catch (_: Exception) {
            -1L
        }
    }
}
