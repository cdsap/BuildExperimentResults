package io.github.cdsap.compare.report.diagnostics

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.output.ExperimentView
import io.github.cdsap.compare.report.BuildDataProvider
import io.github.cdsap.compare.report.ExperimentReport
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
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ExperimentDiagnosticsCliE2ETest {
    private val metrics = BuildWithResourceUsageProvider()

    @TempDir
    lateinit var tempDir: File

    @Test
    fun cliPackageJsonIsImportedIntoExperimentReport() {
        val packageFile = copyPackageFixture()
        val dto = DiagnosticsPackageLoader().load(packageFile)
        assertEquals("1.0", dto.schemaVersion)
        assertEquals("39341370865", dto.experimentRunId)
        assertEquals(setOf("cpu2", "cpu_test"), dto.variants.map { it.variant }.toSet())
        assertTrue(dto.variants.any { it.measurements.any { m -> m.metric == "CPU_DIAGNOSTICS" } })
        assertTrue(dto.variants.any { it.measurements.any { m -> m.metric == "JFR" } })
        assertTrue(
            dto.variants
                .single { it.variant == "cpu2" }
                .measurements
                .any { it.name == "vmstat.us" },
        )

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
                    filter =
                        Filter(
                            maxBuilds = 10,
                            tags = listOf("cpu2", "cpu_test"),
                            exclusiveTags = false,
                            clientType = ClientType.CLI,
                        ),
                    buildDataProvider =
                        object : BuildDataProvider {
                            override suspend fun getBuilds(filter: Filter): List<BuildWithResourceUsage> =
                                listOf(
                                    usageBuild("build-a", arrayOf("cpu2"), 10L),
                                    usageBuild("build-b", arrayOf("cpu_test"), 20L),
                                )
                        },
                    report =
                        report(listOf("cpu2", "cpu_test"))
                            .copy(diagnosticsPackagePath = packageFile.absolutePath),
                    output = ExperimentView(report(listOf("cpu2", "cpu_test"))),
                ).process()
            }
            val csv =
                File(".")
                    .listFiles()
                    ?.filter { it.name.startsWith("experiment_results") && it.name.endsWith(".csv") && it.absolutePath !in before }
                    ?.maxByOrNull { it.lastModified() }
            assertTrue(csv != null && csv.isFile) { "Experiment report CSV was not generated" }
            val csvText = csv!!.readText()
            assertTrue(csvText.contains("vmstat.us")) { csvText }
            assertTrue(csvText.contains("cpu,")) { csvText }
            assertTrue(csvText.contains("recording.size_bytes")) { csvText }
        } finally {
            File(".")
                .listFiles()
                ?.filter { it.name.startsWith("experiment_results") && it.absolutePath !in before }
                ?.forEach { it.delete() }
        }
    }

    private fun copyPackageFixture(): File {
        val dest = File(tempDir, "diagnostics-package.json")
        val source =
            javaClass.classLoader.getResource("diagnostics-cli/diagnostics-package.json")
        assertTrue(source != null) { "Missing diagnostics-cli/diagnostics-package.json fixture" }
        source!!.openStream().use { input ->
            Files.copy(input, dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        return dest
    }

    private fun report(variants: List<String>) =
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
            variants = variants,
            onlyCacheableOutcome = false,
            thresholdTaskDuration = -1,
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
