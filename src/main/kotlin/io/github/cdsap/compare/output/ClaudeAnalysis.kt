package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.ClaudeMessage
import io.github.cdsap.compare.model.ClaudeRequest
import io.github.cdsap.compare.model.ClaudeResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.runBlocking
import java.io.File

class ClaudeAnalysis(
    private val csvFile: File,
    private val claudeKey: String?,
) {
    private val model = "claude-sonnet-4-20250514"
    private val maxTokens = 4096
    private val temperature = 0.3
    private val client =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                gson()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
            }
        }

    fun request(): String {
        if (claudeKey.isNullOrEmpty()) {
            throw IllegalStateException("claudeKey is not set")
        }

        val claudeRequest =
            ClaudeRequest(
                model = model,
                max_tokens = maxTokens,
                system = PromptAnalysis.prompt,
                messages =
                    arrayOf(
                        ClaudeMessage("user", csvFile.readText()),
                    ),
                temperature = temperature,
            )

        return runBlocking {
            try {
                val response =
                    client.post("https://api.anthropic.com/v1/messages") {
                        contentType(ContentType.Application.Json)
                        header("x-api-key", claudeKey)
                        header("anthropic-version", "2023-06-01")
                        setBody(claudeRequest)
                    }

                if (response.status.isSuccess()) {
                    val claudeResponse: ClaudeResponse = response.body()
                    claudeResponse.content
                        .firstOrNull { it.type == "text" }
                        ?.text
                        ?: throw IllegalStateException("No response content received from Claude")
                } else {
                    throw IllegalStateException("API request failed with status: ${response.status}")
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to get analysis: ${e.message}", e)
            } finally {
                client.close()
            }
        }
    }
}
