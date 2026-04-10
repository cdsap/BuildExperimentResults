package io.github.cdsap.compare.model

data class ClaudeRequest(
    val model: String,
    val max_tokens: Int,
    val system: String,
    val messages: Array<ClaudeMessage>,
    val temperature: Double,
)

data class ClaudeMessage(
    val role: String,
    val content: String,
)
