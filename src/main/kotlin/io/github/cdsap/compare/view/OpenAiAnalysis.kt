package io.github.cdsap.compare.view

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
                Role(
                    "system",
                    """Analyze this Gradle build performance comparison data focusing on these aspects:

1. Build Time Comparison:
   - Compare the overall build times between variants (mean, P50, P90)
   - Express times > 1000ms in seconds
   - Calculate percentage differences between variants

2. Task Type Differences Between Variants:
   - For each variant, identify its top 3 most time-consuming tasks
   - Compare how these top tasks perform in the other variant
   - Calculate the percentage difference in execution times between variants for these tasks
   - Show mean, P50, and P90 values for each task in both variants

3. Statistical Patterns:
   - Identify tasks with significant timing variations (>10% difference between variants)
   - Compare P50 and P90 measurements between variants for the same tasks
   - Highlight which variant performs better for specific task types

4. CPU and Memory Usage Analysis:

   - Compare CPU and memory usage between variants for:
     - All processes (overall system usage)
     - Build process (main Gradle process)
     - Build child processes
   - Highlight significant differences (≥10%) between variants in CPU and memory usage.
   - Express memory values in GB and CPU values as percentages (%).

Requirement: The summary should contain markdown formatting for better readability, additionally for tasks and tasks types put quotes.
Additional requirement: Before the detailed report, provide a short introductory summary (3-5 sentences) that describes the results of the experiment.
This short introductory is not included in the markdown formatting requirement and has to had clear separator from the detailed report.
This summary should explain the key focus of the comparison, highlight the most significant findings, and be suitable for display on an overview page.

Note: This data has been pre-filtered to only include tasks where P90 >= 1000ms in at least one variant.
Present only factual observations and numerical comparisons, focusing on the differences between variants."""
                ),
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
