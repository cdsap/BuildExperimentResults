package io.github.cdsap.compare.report

import com.google.gson.JsonParser
import io.github.cdsap.compare.export.ExperimentResultAssembler
import io.github.cdsap.compare.export.JsonExperimentResultExporter
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.output.ExperimentView
import io.github.cdsap.compare.report.measurements.BuildWithResourceUsageProvider
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.ClientType
import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.model.Task
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class ExperimentReportJsonTest {
    private val metrics = BuildWithResourceUsageProvider()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun processWritesCanonicalJsonWithoutChangingHtmlCsvDefaults() {
        val jsonPath = tempDir.resolve("experiment-result.json")
        val artifactWriter = RecordingArtifactWriter()
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
                output = ExperimentView(report(jsonPath.toString()), artifactWriter),
                canonicalJsonExporter =
                    JsonExperimentResultExporter(
                        assembler = ExperimentResultAssembler(producerRevision = "test-revision"),
                    ),
            ).process()
        }

        assertTrue(jsonPath.toFile().isFile)
        val json = JsonParser.parseString(jsonPath.toFile().readText()).asJsonObject
        assertEquals("1.0.0", json.get("schemaVersion").asString)
        assertEquals("test-revision", json.getAsJsonObject("producer").get("revision").asString)
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
        assertEquals(
            setOf(artifactWriter.artifacts.html, artifactWriter.artifacts.csv, artifactWriter.artifacts.ghaSummary),
            artifactWriter.writes.keys,
        )
        assertTrue(artifactWriter.writes.getValue(artifactWriter.artifacts.html).isNotEmpty())
        assertTrue(artifactWriter.writes.getValue(artifactWriter.artifacts.csv).isNotEmpty())
        assertTrue(artifactWriter.writes.getValue(artifactWriter.artifacts.ghaSummary).isNotEmpty())
    }

    @Test
    fun llmCredentialsBelongToExperimentViewInsteadOfReport() {
        val reportProperties = Report::class.memberProperties.map { it.name }
        val viewParameters = ExperimentView::class.primaryConstructor!!.parameters.mapNotNull { it.name }

        assertFalse("openAiKey" in reportProperties)
        assertFalse("claudeKey" in reportProperties)
        assertTrue("openAiKey" in viewParameters)
        assertTrue("claudeKey" in viewParameters)
    }

    @Test
    fun claudeRequestWithoutApiKeyStillWritesFilteredCsv() {
        val report = report(tempDir.resolve("experiment-result.json").toString()).copy(claudeRequest = true)
        val artifactWriter = RecordingArtifactWriter()

        ExperimentView(report, artifactWriter).generateOutputs(
            measurement =
                mapOf(
                    "cpu_test" to
                        listOf(
                            SingleMeasurement("Build", "Build time", "10", "10", "10", "ms", Metric.BUILD),
                        ),
                ),
            buildsByVariant = mapOf("cpu_test" to listOf(usageBuild("build-1", arrayOf("cpu_test"), 10L))),
        )

        assertTrue(artifactWriter.writes.getValue(artifactWriter.artifacts.filteredCsv).isNotEmpty())
    }

    @Test
    fun openAiAnalysisReceivesThePersistedFilteredCsv() {
        val report = report(tempDir.resolve("experiment-result.json").toString()).copy(openAiRequest = true)
        val artifactWriter = RecordingArtifactWriter()
        var analyzedCsv: String? = null

        ExperimentView(
            report = report,
            artifactWriter = artifactWriter,
            openAiKey = "open-ai-key",
            claudeKey = null,
            openAiAnalysis = { csv ->
                analyzedCsv = csv
                "## Summary\nOpenAI analysis"
            },
            claudeAnalysis = { error("Claude analysis should not be requested") },
        ).generateOutputs(
            measurement = singleMeasurement(),
            buildsByVariant = singleBuild(),
        )

        assertEquals(artifactWriter.writes.getValue(artifactWriter.artifacts.filteredCsv), analyzedCsv)
    }

    @Test
    fun claudeAnalysisReceivesThePersistedFilteredCsv() {
        val report = report(tempDir.resolve("experiment-result.json").toString()).copy(claudeRequest = true)
        val artifactWriter = RecordingArtifactWriter()
        var analyzedCsv: String? = null

        ExperimentView(
            report = report,
            artifactWriter = artifactWriter,
            openAiKey = null,
            claudeKey = "claude-key",
            openAiAnalysis = { error("OpenAI analysis should not be requested") },
            claudeAnalysis = { csv ->
                analyzedCsv = csv
                "## Summary\nClaude analysis"
            },
        ).generateOutputs(
            measurement = singleMeasurement(),
            buildsByVariant = singleBuild(),
        )

        assertEquals(artifactWriter.writes.getValue(artifactWriter.artifacts.filteredCsv), analyzedCsv)
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

    private fun singleMeasurement() =
        mapOf(
            "cpu_test" to
                listOf(
                    SingleMeasurement("Build", "Build time", "10", "10", "10", "ms", Metric.BUILD),
                ),
        )

    private fun singleBuild() = mapOf("cpu_test" to listOf(usageBuild("build-1", arrayOf("cpu_test"), 10L)))

    private class RecordingArtifactWriter : ExperimentArtifactWriter {
        val artifacts = ExperimentArtifacts.timestamped("20261028203000")
        val writes = linkedMapOf<String, String>()

        override fun artifacts(): ExperimentArtifacts = artifacts

        override fun write(
            name: String,
            content: String,
        ) {
            writes[name] = content
        }

        override fun exists(name: String) = writes.containsKey(name)
    }

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
