package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurements
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurementSource
import io.github.cdsap.compare.report.measurements.BuildWithResourceUsageProvider
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.ClientType
import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.model.Task
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.reflect.full.primaryConstructor

class ExperimentReportTest {
    private val metrics = BuildWithResourceUsageProvider()

    @Test
    fun dependsOnBuildDataProviderPort() {
        val constructorParameters = ExperimentReport::class.primaryConstructor!!.parameters
        val buildDataProviderParameter = constructorParameters.single { it.name == "buildDataProvider" }

        assertEquals(BuildDataProvider::class, buildDataProviderParameter.type.classifier)
    }

    @Test
    fun dependsOnDiagnosticsMeasurementSourcePort() {
        val constructorParameters = ExperimentReport::class.primaryConstructor!!.parameters
        val diagnosticsSourceParameter = constructorParameters.single { it.name == "diagnosticsMeasurementsSource" }

        assertEquals(DiagnosticsMeasurementSource::class, diagnosticsSourceParameter.type.classifier)
    }

    @Test
    fun processThrowsWhenVariantsAreEmptyWithoutGeClient() {
        val fakeBuildDataProvider =
            object : BuildDataProvider {
                override suspend fun getBuilds(filter: Filter): List<BuildWithResourceUsage> = emptyList()
            }

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    ExperimentReport(
                        filter = filter(),
                        buildDataProvider = fakeBuildDataProvider,
                        report = report(variants = emptyList()),
                    ).process()
                }
            }

        assertEquals("At least one variant is required", exception.message)
    }

    @Test
    fun processThrowsWhenAllVariantResultsAreEmpty() {
        var buildDataProviderInvoked = false
        val fakeBuildDataProvider =
            object : BuildDataProvider {
                override suspend fun getBuilds(filter: Filter): List<BuildWithResourceUsage> {
                    buildDataProviderInvoked = true
                    return emptyList()
                }
            }

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    ExperimentReport(
                        filter = filter(),
                        buildDataProvider = fakeBuildDataProvider,
                        report = report(variants = listOf("variant-a")),
                    ).process()
                }
            }

        assertTrue(buildDataProviderInvoked)
        assertEquals("All variants have empty lists. Please check your input data.", exception.message)
    }

    @Test
    fun processContinuesWithNonEmptyVariantsUsingFakeBuildDataProvider() {
        var buildDataProviderInvoked = false
        val fakeBuildDataProvider =
            object : BuildDataProvider {
                override suspend fun getBuilds(filter: Filter): List<BuildWithResourceUsage> {
                    buildDataProviderInvoked = true
                    return listOf(
                        usageBuild(id = "build-1", tags = arrayOf("variant-a"), buildDuration = 10L),
                        usageBuild(id = "build-2", tags = arrayOf("variant-a"), buildDuration = 20L),
                        usageBuild(id = "build-3", tags = arrayOf("variant-a"), buildDuration = 30L),
                        usageBuild(id = "build-ignored", tags = arrayOf("other"), buildDuration = 40L),
                    )
                }
            }

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
                    buildDataProvider = fakeBuildDataProvider,
                    report =
                        report(
                            variants = listOf("variant-a", "variant-empty"),
                            buildReport = true,
                            taskPathReport = false,
                            taskTypeReport = false,
                        ),
                ).process()
            }
        } finally {
            File(".")
                .listFiles()
                ?.filter { it.name.startsWith("experiment_results") && it.absolutePath !in before }
                ?.forEach { it.delete() }
        }

        assertTrue(buildDataProviderInvoked)
    }

    @Test
    fun processUsesInjectedDiagnosticsMeasurementSourceWithoutPackageFile() {
        var sourceInvoked = false
        val fakeBuildDataProvider =
            object : BuildDataProvider {
                override suspend fun getBuilds(filter: Filter): List<BuildWithResourceUsage> =
                    listOf(usageBuild(id = "build-1", tags = arrayOf("variant-a"), buildDuration = 10L))
            }
        val fakeDiagnosticsSource =
            object : DiagnosticsMeasurementSource {
                override fun load(
                    diagnosticsPackagePath: String?,
                    reportVariants: List<String>,
                ): DiagnosticsMeasurements {
                    sourceInvoked = true
                    assertEquals("does-not-exist.json", diagnosticsPackagePath)
                    assertEquals(listOf("variant-a"), reportVariants)
                    return DiagnosticsMeasurements(
                        measurements =
                            mapOf(
                                "variant-a" to
                                    listOf(
                                        SingleMeasurement(
                                            category = "diagnostics",
                                            name = "cpu",
                                            variantMean = 1.0,
                                            variantP50 = 1.0,
                                            variantP90 = 1.0,
                                            qualifier = "",
                                            metric = Metric.CPU_DIAGNOSTICS,
                                        ),
                                    ),
                            ),
                    )
                }
            }

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
                    buildDataProvider = fakeBuildDataProvider,
                    diagnosticsMeasurementsSource = fakeDiagnosticsSource,
                    report =
                        report(
                            variants = listOf("variant-a"),
                            buildReport = true,
                            taskPathReport = false,
                            taskTypeReport = false,
                        ).copy(diagnosticsPackagePath = "does-not-exist.json"),
                ).process()
            }
        } finally {
            File(".")
                .listFiles()
                ?.filter { it.name.startsWith("experiment_results") && it.absolutePath !in before }
                ?.forEach { it.delete() }
        }

        assertTrue(sourceInvoked)
    }

    private fun filter() =
        Filter(
            maxBuilds = 10,
            tags = listOf("variant-a"),
            exclusiveTags = false,
            clientType = ClientType.CLI,
        )

    private fun report(
        variants: List<String>,
        buildReport: Boolean = true,
        taskPathReport: Boolean = true,
        taskTypeReport: Boolean = true,
    ) = Report(
        taskPathReport = taskPathReport,
        taskTypeReport = taskTypeReport,
        kotlinBuildReport = false,
        processesReport = false,
        buildReport = buildReport,
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
