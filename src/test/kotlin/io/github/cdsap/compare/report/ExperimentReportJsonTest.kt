package io.github.cdsap.compare.report

import com.google.gson.JsonParser
import io.github.cdsap.compare.export.JsonExperimentResultOutput
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.output.ExperimentView
import io.github.cdsap.compare.report.measurements.BuildWithResourceUsageProvider
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.ClientType
import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.model.Task
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ExperimentReportJsonTest {
    private val metrics = BuildWithResourceUsageProvider()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun processWritesCanonicalJsonWithoutChangingHtmlCsvDefaults() {
        val jsonPath = tempDir.resolve("experiment-result.json")
        val before =
            File(".")
                .listFiles()
                ?.filter { it.name.startsWith("experiment_results") }
                ?.map { it.absolutePath }
                ?.toSet()
                .orEmpty()

        try {
            runBlocking {
                ExperimentReport(
                    filter = filter(),
                    buildDataProvider =
                        object : BuildDataProvider {
                            override suspend fun getBuilds(filter: Filter): List<BuildWithResourceUsage> =
                                listOf(
                                    usageBuild("build-1", arrayOf("cpu_test"), 10L),
                                    usageBuild("build-2", arrayOf("cpu2"), 20L),
                                )
                        },
                    report =
                        report(jsonPath.toString()).copy(
                            variants = listOf("cpu_test", "cpu2"),
                            diagnosticsPackagePath =
                                javaClass.classLoader.getResource("diagnostics-cli/diagnostics-package.json")!!.path,
                        ),
                    output = ExperimentView(report(jsonPath.toString())),
                    resultOutput = JsonExperimentResultOutput(),
                ).process()
            }

            assertTrue(jsonPath.toFile().isFile)
            val json = JsonParser.parseString(jsonPath.toFile().readText()).asJsonObject
            assertEquals("1.0.0", json.get("schemaVersion").asString)
            assertTrue(json.getAsJsonArray("metrics").size() > 0)
            assertTrue(
                json.getAsJsonArray("metrics").any { metric ->
                    metric.asJsonObject.get("id").asString == "build.duration"
                },
            )
            assertEquals("present", json.getAsJsonObject("diagnostics").get("status").asString)
            assertTrue(
                json.getAsJsonArray("metrics").any { metric ->
                    metric.asJsonObject.get("family").asString == "cpu-diagnostics"
                },
            )
            assertTrue(
                json.getAsJsonArray("metrics").any { metric ->
                    metric.asJsonObject.get("family").asString == "jfr"
                },
            )
            val created =
                File(".")
                    .listFiles()
                    ?.filter { it.name.startsWith("experiment_results") && it.absolutePath !in before }
                    .orEmpty()
            assertTrue(created.any { it.name.endsWith(".html") })
            assertTrue(created.any { it.name.endsWith(".csv") })
            assertTrue(created.any { it.name == "experiment_results_summary_gha" })
        } finally {
            File(".")
                .listFiles()
                ?.filter { it.name.startsWith("experiment_results") && it.absolutePath !in before }
                ?.forEach { it.delete() }
        }
    }

    private fun filter() =
        Filter(
            maxBuilds = 10,
            tags = listOf("cpu_test", "cpu2"),
            exclusiveTags = false,
            clientType = ClientType.CLI,
        )

    private fun report(outputJson: String) =
        Report(
            taskPathReport = false,
            taskTypeReport = false,
            kotlinBuildReport = false,
            processesReport = false,
            buildReport = true,
            resourceUsageReport = false,
            isProfile = false,
            gcReport = false,
            warmupsToDiscard = 0,
            variants = listOf("cpu_test", "cpu2"),
            onlyCacheableOutcome = false,
            thresholdTaskDuration = -1,
            experimentId = "cdsap-428",
            experimentRunId = "39505441898",
            reportKind = "caching",
            outputJson = outputJson,
        )

    private fun usageBuild(
        id: String,
        tags: Array<String>,
        buildDuration: Long,
    ): BuildWithResourceUsage =
        BuildWithResourceUsage(
            id = id,
            builtTool = "gradle",
            taskExecution =
                arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                ),
            goalExecution = emptyArray(),
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
            tags = tags,
            requestedTask = arrayOf("assemble"),
            buildDuration = buildDuration,
            buildStartTime = 1L,
            projectName = "demo",
            configuration = 5L,
            execution = metrics.get(),
            nonExecution = metrics.get(),
            total = metrics.get(),
            totalMemory = 0L,
        )
}
