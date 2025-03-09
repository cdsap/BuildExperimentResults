package io.github.cdsap.compare.model

data class OpenAiRequest(
    val model: String,
    val messages: Array<Role>,
    val temperature: Double
)

data class Role(
    val role: String,
    val content: String
)
