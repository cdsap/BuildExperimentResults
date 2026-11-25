package io.github.cdsap.compare.output

import io.github.cdsap.compare.output.ai.AiHttpClient
import io.github.cdsap.compare.output.ai.OpenAiRequest
import io.github.cdsap.compare.output.ai.OpenAiResponse
import io.github.cdsap.compare.output.ai.Role
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking

class OpenAiAnalysis(
    private val csvContent: String,
    private val openAiKey: String?,
) {
    private val model = "gpt-4-turbo"
    private val temperature = 0.3
    private val client = AiHttpClient.create()

    fun request(): String {
        if (openAiKey.isNullOrEmpty()) {
            throw IllegalStateException("openAiKey is not set")
        }

        val openAiRequest =
            OpenAiRequest(
                model = model,
                messages =
                    arrayOf(
                        Role("system", PromptAnalysis.prompt),
                        Role("user", csvContent),
                    ),
                temperature = temperature,
            )

        return runBlocking {
            try {
                val response =
                    client.post("https://api.openai.com/v1/chat/completions") {
                        contentType(ContentType.Application.Json)
                        header("Authorization", "Bearer $openAiKey")
                        setBody(openAiRequest)
                    }

                if (response.status.isSuccess()) {
                    val openAiResponse: OpenAiResponse = response.body()
                    openAiResponse.choices
                        .firstOrNull()
                        ?.message
                        ?.content
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
