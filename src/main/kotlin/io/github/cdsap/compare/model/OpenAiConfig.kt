package io.github.cdsap.compare.model

data class OpenAiConfig(
    val model: String = "gpt-4",
    val temperature: Double = 0.3,
    val timeoutMillis: Long = 120_000,
    val apiKey: String
) {
    init {
        require(apiKey.isNotEmpty()) { "OpenAI API key cannot be empty" }
        require(temperature in 0.0..1.0) { "Temperature must be between 0.0 and 1.0" }
        require(timeoutMillis > 0) { "Timeout must be positive" }
    }
} 