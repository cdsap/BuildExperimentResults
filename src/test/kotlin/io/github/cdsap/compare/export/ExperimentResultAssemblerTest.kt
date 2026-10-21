package io.github.cdsap.compare.export

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurements
import io.github.cdsap.compare.report.measurements.BuildWithResourceUsageProvider
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.CustomValue
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ExperimentResultAssemblerTest {
    private val metrics = BuildWithResourceUsageProvider()
    private val assembler =
        ExperimentResultAssembler(
            clock = Clock.fixed(Instant.parse("2026-10-20T21:00:00Z"), ZoneOffset.UTC),
            producerRevision = "test-sha",
        )

    @Test
    fun exportsTypedBuildMetricsAndPerBuildObservations() {
        val report =
            report(
                variants = listOf("cdsap-410_varianta_cpu_test", "cdsap-410_variantb_cpu2"),
                experimentId = "cdsap-410",
                experimentRunId = "123456789",
                reportKind = "caching",
                runAttempt = 1,
            )
        val builds =
            mapOf(
                "cdsap-410_varianta_cpu_test" to
                    listOf(
                        usageBuild("b-001", arrayOf("cdsap-410_varianta_cpu_test"), 239100, 28000),
                        usageBuild("b-002", arrayOf("cdsap-410_varianta_cpu_test"), 241000, 29000),
                    ),
                "cdsap-410_variantb_cpu2" to
                    listOf(usageBuild("b-051", arrayOf("cdsap-410_variantb_cpu2"), 231900, 27200)),
            )
        val measurements =
            mapOf(
                "cdsap-410_varianta_cpu_test" to
                    listOf(
                        SingleMeasurement("Build", "Build time", "240050", "239100", "241000", "ms", Metric.BUILD),
                        SingleMeasurement("Build", "Configuration time", "28500", "28000", "29000", "ms", Metric.BUILD),
                    ),
                "cdsap-410_variantb_cpu2" to
                    listOf(
                        SingleMeasurement("Build", "Build time", "231900", "231900", "231900", "ms", Metric.BUILD),
                        SingleMeasurement("Build", "Configuration time", "27200", "27200", "27200", "ms", Metric.BUILD),
                    ),
            )

        val result = assembler.assemble(report, measurements, builds, DiagnosticsMeasurements())

        assertEquals(EXPERIMENT_RESULT_SCHEMA_VERSION, result.schemaVersion)
        assertEquals("2026-10-20T21:00:00Z", result.generatedAt)
        assertEquals("run-123456789-caching-attempt-1", result.report.id)
        assertEquals(listOf("cpu_test", "cpu2"), result.variants.map { it.id })
        val duration = result.metrics.first { it.id == "build.duration" }
        assertEquals(240050.0, duration.values.getValue("cpu_test").mean)
        assertEquals("build", duration.aggregation.sampleUnit)
        assertEquals(2, result.datasets.builds.getValue("cpu_test").size)
        assertEquals(239100.0, result.datasets.builds.getValue("cpu_test").first().durationMs)
        assertEquals(28000.0, result.datasets.builds.getValue("cpu_test").first().configurationMs)
        assertEquals("https://ge.example/s/b-001", result.datasets.builds.getValue("cpu_test").first().scanUrl)
        assertEquals(":app:compileDebugKotlin", result.datasets.taskSeries.values.first().path)
        assertEquals("DIAGNOSTICS_NOT_REQUESTED", result.warnings.single().code)
        assertEquals("not-requested", result.diagnostics.status)
        assertNull(result.diagnostics.source)
    }

    @Test
    fun preservesDiagnosticsAggregatesWithoutInventingATimeline() {
        val report = report(variants = listOf("owner-1_varianta_lint-4-1"))
        val builds =
            mapOf(
                "owner-1_varianta_lint-4-1" to listOf(usageBuild("b-1", arrayOf("owner-1_varianta_lint-4-1"), 10, 5)),
            )
        val measurements =
            mapOf(
                "owner-1_varianta_lint-4-1" to
                    listOf(
                        SingleMeasurement("cpu", "vmstat.us", 10.5, 9.0, 12.0, "system", Metric.CPU_DIAGNOSTICS),
                        SingleMeasurement("jfr", "recording.size_bytes", 100.0, 100.0, 100.0, "gradle", Metric.JFR),
                    ),
            )
        val diagnostics =
            DiagnosticsMeasurements(
                measurements = measurements,
                status = "present",
                sourceSchemaVersion = "1.0",
                sourceExperimentRunId = "39341370865",
                sourceRepo = "cdsap/Telltale",
                jobKeysByVariant = mapOf("owner-1_varianta_lint-4-1" to listOf("execution-caching-0")),
            )

        val result = assembler.assemble(report, measurements, builds, diagnostics)

        val cpu = result.metrics.first { it.family == "cpu-diagnostics" }
        assertEquals("diagnostic-sample", cpu.aggregation.sampleUnit)
        assertNull(cpu.values.values.single().count)
        assertTrue(cpu.values.values.single().mean is Double)
        assertEquals("present", result.diagnostics.status)
        assertEquals("1.0", result.diagnostics.source?.schemaVersion)
        assertEquals(
            listOf("execution-caching-0"),
            result.diagnostics.groups.first { it.id == "cpu" }.jobKeysByVariant.values.single(),
        )
        assertTrue(result.datasets.builds.isNotEmpty())
        assertTrue(result.warnings.none { it.code == "DIAGNOSTICS_NOT_REQUESTED" })
    }

    @Test
    fun recordsMissingVariantsAndSanitizesDiagnosticPaths() {
        val report = report(variants = listOf("variant-a", "variant-empty"))
        val builds = mapOf("variant-a" to listOf(usageBuild("b-1", arrayOf("variant-a"), 10, 5)))
        val measurements =
            mapOf(
                "variant-a" to listOf(SingleMeasurement("Build", "Build time", "10", "10", "10", "ms", Metric.BUILD)),
            )
        val diagnostics =
            DiagnosticsMeasurements(
                warnings = listOf("Diagnostics package not found: /tmp/private/missing.json; continuing without diagnostics"),
                status = "unavailable",
            )

        val result = assembler.assemble(report, measurements, builds, diagnostics)

        assertEquals("partial", result.report.status)
        assertEquals(listOf("variant-a"), result.completeness.observedVariantIds)
        assertTrue(result.warnings.any { it.code == "VARIANT_HAS_NO_BUILDS" && it.variantId == "variant-empty" })
        assertTrue(result.warnings.any { it.code == "PARTIAL_EXPERIMENT" })
        val missing = result.warnings.first { it.code == "DIAGNOSTICS_PACKAGE_MISSING" }
        assertTrue(missing.message.contains("missing.json"))
        assertTrue(!missing.message.contains("/tmp/private"))
    }

    @Test
    fun omitsObservationDatasetsWhenDisabled() {
        val report = report(variants = listOf("variant-a")).copy(includeObservations = false)
        val builds = mapOf("variant-a" to listOf(usageBuild("b-1", arrayOf("variant-a"), 10, 5)))
        val measurements =
            mapOf(
                "variant-a" to listOf(SingleMeasurement("Build", "Build time", "10", "10", "10", "ms", Metric.BUILD)),
            )

        val result = assembler.assemble(report, measurements, builds, DiagnosticsMeasurements())

        assertTrue(result.datasets.builds.isEmpty())
        assertTrue(result.datasets.taskSeries.isEmpty())
        assertEquals("build.duration", result.metrics.single().id)
    }

    @Test
    fun projectsProcessAndGcSeriesFromCustomValues() {
        val report =
            report(variants = listOf("variant-a")).copy(processesReport = true, gcReport = true)
        val builds =
            mapOf(
                "variant-a" to
                    listOf(
                        usageBuild(
                            id = "b-1",
                            tags = arrayOf("variant-a"),
                            buildDuration = 10,
                            configuration = 5,
                            values =
                                arrayOf(
                                    CustomValue("Gradle-Process-1-gcTime", "0.19 minutes"),
                                    CustomValue("gc-gradle_gc.log-total-collections", "10"),
                                ),
                        ),
                    ),
            )
        val measurements =
            mapOf(
                "variant-a" to listOf(SingleMeasurement("Build", "Build time", "10", "10", "10", "ms", Metric.BUILD)),
            )

        val result = assembler.assemble(report, measurements, builds, DiagnosticsMeasurements())

        assertEquals(
            0.19,
            result.datasets.processSeries.values.single().observations.getValue("variant-a").single().value,
        )
        assertEquals(
            10.0,
            result.datasets.gcSeries.values.single().observations.getValue("variant-a").single().value,
        )
    }

    private fun report(
        variants: List<String>,
        experimentId: String? = null,
        experimentRunId: String? = null,
        reportKind: String = "report",
        runAttempt: Int? = null,
    ) = Report(
        taskPathReport = true,
        taskTypeReport = false,
        kotlinBuildReport = false,
        processesReport = false,
        buildReport = true,
        resourceUsageReport = true,
        isProfile = false,
        gcReport = false,
        warmupsToDiscard = 0,
        variants = variants,
        experimentId = experimentId,
        onlyCacheableOutcome = false,
        thresholdTaskDuration = -1,
        repository = "cdsap/Telltale",
        url = "https://ge.example/",
        experimentRunId = experimentRunId,
        reportKind = reportKind,
        runAttempt = runAttempt,
    )

    private fun usageBuild(
        id: String,
        tags: Array<String>,
        buildDuration: Long,
        configuration: Long,
        values: Array<CustomValue> = emptyArray(),
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
            requestedTask = arrayOf(":app:assembleDebug"),
            buildDuration = buildDuration,
            buildStartTime = 1_000L,
            projectName = "demo",
            configuration = configuration,
            execution = metrics.get(),
            nonExecution = metrics.get(),
            total = metrics.get(),
            totalMemory = 0L,
            values = values,
        )
}
