package io.github.cdsap.compare.view

import com.google.cloud.ai.generativelanguage.v1.GenerativeServiceClient
import com.google.cloud.ai.generativelanguage.v1.GenerationConfig
import com.google.cloud.ai.generativelanguage.v1.Content
import com.google.cloud.ai.generativelanguage.v1.Part
import com.google.cloud.ai.generativelanguage.v1.GenerateContentRequest
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import java.io.File

class GeminiAnalysis(private val csvFile: File) {
    private val model = "gemini-pro"
    private val temperature = 0.3
    private val apiKey = "AIzaSyDKOKEhp-RE0BG-4unUUwM_drK4Wzy4T6c"// System.getenv("GEMINI_API_KEY")

    fun request(): String {
        if (apiKey.isNullOrEmpty()) {
            throw IllegalStateException("GEMINI_API_KEY environment variable is not set")
        }

        val credentials = GoogleCredentials.fromAccessToken(apiKey)
        val client = GenerativeServiceClient.create(
            GenerativeServiceClient.Settings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build()
        )

        val prompt = """Analyze this Gradle build performance comparison data focusing on these aspects:

1. Build Time Comparison:
   - Compare the overall build times between variants (mean, P50, P90)
   - Express times > 1000ms in seconds
   - Calculate percentage differences between variants

2. Task Execution Analysis:
   - List top 3 most time-consuming task types by mean duration
   - Show their respective P50 and P90 values
   - Calculate the variance between mean and P90 for these tasks

3. Statistical Patterns:
   - Identify any significant timing variations (>10% difference between variants)
   - Note any unusual patterns between P50 and P90 measurements
   - Highlight tasks with high variance between mean and P90

Present only factual observations and numerical comparisons, focusing on the data patterns and statistical relationships."""

        try {
            val request = GenerateContentRequest.newBuilder()
                .setModel("models/$model")
                .setContents(
                    Content.newBuilder()
                        .addParts(
                            Part.newBuilder().setText(prompt).build()
                        )
                        .addParts(
                            Part.newBuilder().setText(csvFile.readText()).build()
                        )
                )
                .setGenerationConfig(
                    GenerationConfig.newBuilder()
                        .setTemperature(temperature)
                        .build()
                )
                .build()

            val response = client.generateContent(request)
            return response.candidatesList.firstOrNull()
                ?.content
                ?.partsList
                ?.firstOrNull()
                ?.text
                ?: throw IllegalStateException("No response content received from Gemini")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to get analysis: ${e.message}", e)
        } finally {
            client.close()
        }
    }
}

// Example usage
fun main() {
    val csvFile = File("/Users/inakivillar/reports/buildscanexperiments/BuildExperimentResults/experiment_results_20250306023630.csv")
    println(csvFile.path)
    val analysis = GeminiAnalysis(csvFile)
    val result = analysis.request()
    println(result)
}
