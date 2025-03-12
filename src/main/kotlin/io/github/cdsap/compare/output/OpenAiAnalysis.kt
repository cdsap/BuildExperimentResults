package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.OpenAiRequest
import io.github.cdsap.compare.model.OpenAiResponse
import io.github.cdsap.compare.model.Role
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

class OpenAiAnalysis(
    private val csvFile: File,
    private val openAiKey: String?
) {
    private val model = "gpt-4"
    private val temperature = 0.3
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
        }
    }

    fun request(): String {
        if (openAiKey.isNullOrEmpty()) {
            throw IllegalStateException("openAiKey is not set")
        }

        val openAiRequest = OpenAiRequest(
            model = model,
            messages = arrayOf(
                Role("system", PromptAnalysis.prompt),
                Role("user", csvFile.readText())
            ),
            temperature = temperature
        )

        return runBlocking {
            try {
                val response = client.post("https://api.openai.com/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $openAiKey")
                    setBody(openAiRequest)
                }

                if (response.status.isSuccess()) {
                    val openAiResponse: OpenAiResponse = response.body()
                    openAiResponse.choices.firstOrNull()?.message?.content
                        ?: throw IllegalStateException("No response content received from OpenAI")
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
