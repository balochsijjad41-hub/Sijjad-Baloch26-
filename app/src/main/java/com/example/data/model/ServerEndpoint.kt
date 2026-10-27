package com.example.data.model

enum class ServerStatus {
    OPTIMAL,
    STABLE,
    HIGH_LATENCY,
    OFFLINE
}

data class ServerEndpoint(
    val id: String,
    val name: String,
    val region: String,
    val pingUrl: String,
    val latencyMs: Long = 0L,
    val status: ServerStatus = ServerStatus.STABLE,
    val isAutoSelected: Boolean = false
)
