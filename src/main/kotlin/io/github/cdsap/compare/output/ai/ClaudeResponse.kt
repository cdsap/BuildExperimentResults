package io.github.cdsap.compare.output.ai

data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    val stop_reason: String,
    val usage: ClaudeUsage,
)

data class ContentBlock(
    val type: String,
    val text: String,
)

data class ClaudeUsage(
    val input_tokens: Int,
    val output_tokens: Int,
)
