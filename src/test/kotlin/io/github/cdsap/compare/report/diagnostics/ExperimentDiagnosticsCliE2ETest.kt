package io.github.cdsap.compare.report.diagnostics

import io.github.cdsap.compare.model.Report
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
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

class ExperimentDiagnosticsCliE2ETest {
    private val metrics = BuildWithResourceUsageProvider()

    @TempDir
    lateinit var tempDir: File

    @Test
    fun releasedCliPackageIsImportedIntoExperimentReport() {
        assumeTrue(Runtime.version().feature() >= 21) {
            "ExperimentDiagnosticsReport v0.1.0 requires JDK 21"
        }
        val cli = resolveCli()
        assumeTrue(cli != null && cli.canExecute()) { "ExperimentDiagnosticsReport CLI is not available" }

        val artifactsDir = artifactsFixtureDir()
        val packageFile = File(tempDir, "diagnostics-package.json")
        val javaHome = System.getProperty("java.home")
        val processBuilder =
            ProcessBuilder(
                cli!!.absolutePath,
                "report",
                "--artifacts-dir",
                artifactsDir.absolutePath,
                "--output",
                packageFile.absolutePath,
                "--force",
            ).directory(tempDir)
                .redirectErrorStream(true)
        val environment = processBuilder.environment()
        environment["JAVA_HOME"] = javaHome
        environment["PATH"] = "$javaHome/bin${File.pathSeparator}${environment["PATH"]}"
        val output = processBuilder.start()
        val finished = output.waitFor(2, TimeUnit.MINUTES)
        val logs = output.inputStream.bufferedReader().readText()
        assertTrue(finished && output.exitValue() == 0) {
            "CLI failed (finished=$finished exit=${if (finished) output.exitValue() else "timeout"}): $logs"
        }
        assertTrue(packageFile.isFile) { "CLI did not write ${packageFile.path}\n$logs" }

        val dto = DiagnosticsPackageLoader().load(packageFile)
        assertEquals("1.0", dto.schemaVersion)
        val variantNames = dto.variants.map { it.variant }.toSet()
        assertEquals(setOf("cpu2", "cpu_test"), variantNames)
        assertTrue(dto.variants.any { it.measurements.any { m -> m.metric == "CPU_DIAGNOSTICS" } })
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
        } finally {
            File(".")
                .listFiles()
                ?.filter { it.name.startsWith("experiment_results") && it.absolutePath !in before }
                ?.forEach { it.delete() }
        }
    }

    private fun artifactsFixtureDir(): File {
        val url = javaClass.classLoader.getResource("diagnostics-cli/artifacts")
        assertTrue(url != null) { "Missing diagnostics-cli/artifacts fixture" }
        return File(url!!.toURI())
    }

    private fun resolveCli(): File? {
        val configured =
            System.getProperty("experimentDiagnosticsCli")
                ?: System.getenv("EXPERIMENT_DIAGNOSTICS_CLI")
        if (!configured.isNullOrBlank()) {
            return File(configured)
        }
        return downloadReleasedCli()
    }

    private fun downloadReleasedCli(): File? {
        val dest = File(tempDir, "experiment-diagnostics-report")
        return try {
            URI(CLI_RELEASE_URL).toURL().openStream().use { input ->
                Files.copy(input, dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            dest.setExecutable(true)
            dest
        } catch (exception: Exception) {
            val required = System.getenv("GITHUB_ACTIONS") == "true"
            if (required) {
                throw AssertionError("Failed to download diagnostics CLI from $CLI_RELEASE_URL", exception)
            }
            null
        }
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

    companion object {
        private const val CLI_RELEASE_URL =
            "https://github.com/cdsap/ExperimentDiagnosticsReport/releases/download/v0.1.0/experiment-diagnostics-report"
    }
}
