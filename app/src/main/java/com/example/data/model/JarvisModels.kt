package com.example.data.model

enum class JarvisTheme(val displayName: String, val hexAccent: String) {
    CYAN("Arc Reactor Cyan", "#00E5FF"),
    CRIMSON("Mark VII Crimson", "#FF1744"),
    EMERALD("Matrix Emerald", "#00E676"),
    VIOLET("Quantum Violet", "#B388FF"),
    AMBER("Solar Amber", "#FFAB00")
}

enum class JarvisMode(val title: String, val promptInstruction: String) {
    NORMAL("Core Protocol", "Default JARVIS protocol. Calm, witty, loyal, and concise."),
    STUDY("Study Mode", "Provide structured, crisp explanations with key concepts and optional quick comprehension quiz questions."),
    FOCUS("Focus Protocol", "Strictly concise updates, silent ambient timer management, zero distractions."),
    CHILL("Chill Protocol", "Relaxed, witty, casual banter, Iron Man style humour while remaining helpful.")
}

enum class AssistantState {
    STANDBY,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionSummary: String? = null
) {
    enum class Sender { USER, JARVIS, SYSTEM }
}

data class ConfirmationRequest(
    val actionType: ActionType,
    val title: String,
    val description: String,
    val payload: Map<String, String>,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
) {
    enum class ActionType {
        CALL,
        WHATSAPP,
        EMAIL,
        DELETE_DATA
    }
}
