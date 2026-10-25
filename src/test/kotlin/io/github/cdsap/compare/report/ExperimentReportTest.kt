package io.github.cdsap.compare.report

import com.google.gson.JsonParser
import io.github.cdsap.compare.export.JsonExperimentResultOutput
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurements
import io.github.cdsap.compare.report.diagnostics.DiagnosticsPackageSource
import io.github.cdsap.compare.report.measurements.BuildWithResourceUsageProvider
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.ClientType
import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.model.Task
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

class ExperimentReportTest {
    private val metrics = BuildWithResourceUsageProvider()
    private val fakeOutput =
        object : ExperimentOutput {
            override fun generateOutputs(
                measurement: Map<String, List<SingleMeasurement>>,
                buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
            ) = Unit
        }
    private val fakeResultOutput =
        object : ExperimentResultOutput {
            override fun write(
                report: Report,
                measurements: Map<String, List<SingleMeasurement>>,
                buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
                diagnostics: DiagnosticsMeasurements,
                outputJson: String,
            ) = Unit
        }

    @Test
    fun dependsOnBuildDataProviderPort() {
        val constructorParameters = ExperimentReport::class.primaryConstructor!!.parameters
        val buildDataProviderParameter = constructorParameters.single { it.name == "buildDataProvider" }

        assertEquals(BuildDataProvider::class, buildDataProviderParameter.type.classifier)
    }

    @Test
    fun dependsOnDiagnosticsPackageSourcePort() {
        val constructorParameters = ExperimentReport::class.primaryConstructor!!.parameters
        val diagnosticsSourceParameter = constructorParameters.single { it.name == "diagnosticsMeasurementsSource" }

        assertEquals(DiagnosticsPackageSource::class, diagnosticsSourceParameter.type.classifier)
    }

    @Test
    fun dependsOnCanonicalResultOutputPort() {
        val constructorParameters = ExperimentReport::class.primaryConstructor!!.parameters
        val resultOutputParameter = constructorParameters.single { it.name == "resultOutput" }

        assertEquals(ExperimentResultOutput::class, resultOutputParameter.type.classifier)
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
                        output = fakeOutput,
                        resultOutput = fakeResultOutput,
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
                        output = fakeOutput,
                        resultOutput = fakeResultOutput,
                    ).process()
                }
            }

        assertTrue(buildDataProviderInvoked)
        assertEquals("All variants have empty lists. Please check your input data.", exception.message)
    }

    @Test
    fun processContinuesWithNonEmptyVariantsUsingFakeBuildDataProvider() {
        var buildDataProviderInvoked = false
        var outputInvoked = false
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
                output =
                    object : ExperimentOutput {
                        override fun generateOutputs(
                            measurement: Map<String, List<SingleMeasurement>>,
                            buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
                        ) {
                            outputInvoked = true
                        }
                    },
                resultOutput = fakeResultOutput,
            ).process()
        }

        assertTrue(buildDataProviderInvoked)
        assertTrue(outputInvoked)
    }

    @Test
    fun processUsesInjectedDiagnosticsPackageSourceWithoutPackageFile() {
        var sourceInvoked = false
        val fakeBuildDataProvider =
            object : BuildDataProvider {
                override suspend fun getBuilds(filter: Filter): List<BuildWithResourceUsage> =
                    listOf(usageBuild(id = "build-1", tags = arrayOf("variant-a"), buildDuration = 10L))
            }
        val fakeDiagnosticsSource =
            object : DiagnosticsPackageSource {
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

        val outputJson = java.io.File.createTempFile("experiment-report-test", ".json")
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
                    ).copy(
                        diagnosticsPackagePath = "does-not-exist.json",
                        outputJson = outputJson.toString(),
                    ),
                output =
                    object : ExperimentOutput {
                        override fun generateOutputs(
                            measurement: Map<String, List<SingleMeasurement>>,
                            buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
                        ) = Unit
                    },
                resultOutput = JsonExperimentResultOutput(),
            ).process()
        }

        assertTrue(sourceInvoked)
        val metrics = JsonParser.parseString(outputJson.readText()).asJsonObject.getAsJsonArray("metrics")
        assertTrue(metrics.any { it.asJsonObject.get("name").asString == "cpu" })
    }

    @Test
    fun processDeliversCanonicalResultToInjectedOutputWithoutFilesystemAccess() {
        var deliveredReport: Report? = null
        var deliveredMeasurements: Map<String, List<SingleMeasurement>>? = null
        var deliveredBuilds: Map<String, List<BuildWithResourceUsage>>? = null
        var deliveredDiagnostics: DiagnosticsMeasurements? = null
        var deliveredPath: String? = null
        val fakeBuildDataProvider =
            object : BuildDataProvider {
                override suspend fun getBuilds(filter: Filter): List<BuildWithResourceUsage> =
                    listOf(usageBuild(id = "build-1", tags = arrayOf("variant-a"), buildDuration = 10L))
            }
        val fakeResultOutput =
            object : ExperimentResultOutput {
                override fun write(
                    report: Report,
                    measurements: Map<String, List<SingleMeasurement>>,
                    buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
                    diagnostics: DiagnosticsMeasurements,
                    outputJson: String,
                ) {
                    deliveredReport = report
                    deliveredMeasurements = measurements
                    deliveredBuilds = buildsByVariant
                    deliveredDiagnostics = diagnostics
                    deliveredPath = outputJson
                }
            }

        runBlocking {
            ExperimentReport(
                filter = filter(),
                buildDataProvider = fakeBuildDataProvider,
                report =
                    report(
                        variants = listOf("variant-a"),
                        buildReport = true,
                        taskPathReport = false,
                        taskTypeReport = false,
                    ).copy(outputJson = "does-not-need-to-exist.json"),
                output = fakeOutput,
                resultOutput = fakeResultOutput,
            ).process()
        }

        assertNotNull(deliveredReport)
        assertNotNull(deliveredMeasurements)
        assertNotNull(deliveredBuilds)
        assertNotNull(deliveredDiagnostics)
        assertEquals("does-not-need-to-exist.json", deliveredPath)
        assertEquals(1, deliveredBuilds?.getValue("variant-a")?.size)
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
