package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.measurements.BuildWithResourceUsageProvider
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.CustomValue
import io.github.cdsap.geapi.client.model.PerformanceMetrics
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.github.cdsap.geapi.client.model.Metric as GeMetric

class ChartGeneratorDiagnosticsTest {
    @Test
    fun generatesSectionsAndScriptsOnlyForPresentDiagnosticMetrics() {
        val report =
            Report(
                taskPathReport = false,
                taskTypeReport = false,
                kotlinBuildReport = false,
                processesReport = false,
                buildReport = false,
                resourceUsageReport = false,
                isProfile = false,
                gcReport = false,
                warmupsToDiscard = 0,
                variants = listOf("variant-a"),
                onlyCacheableOutcome = false,
                thresholdTaskDuration = -1,
            )
        val measurements =
            mapOf(
                "variant-a" to
                    listOf(
                        SingleMeasurement(
                            category = "cpu",
                            name = "vmstat.us",
                            variantMean = 10.0,
                            variantP50 = 9.0,
                            variantP90 = 12.0,
                            qualifier = "%",
                            metric = Metric.CPU_DIAGNOSTICS,
                        ),
                    ),
            )

        val html =
            ChartGenerator(report).generateCharts(
                variants = mapOf("variant-a" to listOf(build())),
                header = Header(task = "assemble", numberOfBuilds = listOf(1), url = "https://ge/"),
                measurements = measurements,
            )

        assertTrue(html.contains("<h2>CPU Diagnostics</h2>"))
        assertTrue(html.contains("id=\"cpuDiagnosticsChart\""))
        assertTrue(html.contains("vmstat.us"))
        assertTrue(html.contains("type: 'bar'"))
        assertTrue(html.contains("text: 'Metric'"))
        assertTrue(!html.contains("<h2>JFR Diagnostics</h2>"))
    }

    @Test
    fun resourceUsageChartsUseIncludedScansWhenEveryVariantHasATotal() {
        val metrics = BuildWithResourceUsageProvider()
        val html =
            ChartGenerator(report().copy(resourceUsageReport = true)).generateCharts(
                variants =
                    mapOf(
                        "variant-a" to listOf(build(), build()),
                        "variant-b" to listOf(build(), metrics.withoutTotal(build())),
                    ),
                header = Header(task = "assemble", numberOfBuilds = listOf(2, 2), url = "https://ge/"),
            )

        assertTrue(html.contains("id=\"buildProcessMemoryChart\""))
        assertTrue(html.contains("getElementById('buildProcessMemoryChart')"))
        val memoryScript = html.substringAfter("getElementById('buildProcessMemoryChart')")
        assertTrue(memoryScript.contains("data: [50.0,50.0]"))
        assertTrue(memoryScript.contains("data: [50.0]"), memoryScript.take(800))
    }

    @Test
    fun resourceUsageChartsAreOmittedWhenAVariantHasNoTotals() {
        val metrics = BuildWithResourceUsageProvider()
        val html =
            ChartGenerator(report().copy(resourceUsageReport = true)).generateCharts(
                variants =
                    mapOf(
                        "variant-a" to listOf(build(), build()),
                        "variant-b" to listOf(metrics.withoutTotal(build()), metrics.withoutTotal(build())),
                    ),
                header = Header(task = "assemble", numberOfBuilds = listOf(2, 2), url = "https://ge/"),
            )

        assertTrue(!html.contains("id=\"buildProcessMemoryChart\""))
        assertTrue(!html.contains("id=\"buildChildProcessMemoryChart\""))
    }

    @Test
    fun doesNotEmitGcChartScriptsWhenGcReportIsDisabled() {
        val html =
            ChartGenerator(report()).generateCharts(
                variants = mapOf("variant-a" to listOf(buildWithGcCustomValues())),
                header = Header(task = "assemble", numberOfBuilds = listOf(1), url = "https://ge/"),
                measurements = cpuMeasurements(),
            )

        assertTrue(html.contains("id=\"cpuDiagnosticsChart\""))
        assertTrue(html.contains("getElementById('cpuDiagnosticsChart')"))
        assertTrue(!html.contains("getElementById('gc-gradle_gc.log')"))
        assertTrue(!html.contains("<canvas id=\"gc-gradle_gc.log\""))
    }

    private fun report() =
        Report(
            taskPathReport = false,
            taskTypeReport = false,
            kotlinBuildReport = false,
            processesReport = false,
            buildReport = false,
            resourceUsageReport = false,
            isProfile = false,
            gcReport = false,
            warmupsToDiscard = 0,
            variants = listOf("variant-a"),
            onlyCacheableOutcome = false,
            thresholdTaskDuration = -1,
        )

    private fun cpuMeasurements() =
        mapOf(
            "variant-a" to
                listOf(
                    SingleMeasurement(
                        category = "cpu",
                        name = "vmstat.us",
                        variantMean = 10.0,
                        variantP50 = 9.0,
                        variantP90 = 12.0,
                        qualifier = "%",
                        metric = Metric.CPU_DIAGNOSTICS,
                    ),
                ),
        )

    private fun buildWithGcCustomValues() =
        build(
            values =
                arrayOf(
                    CustomValue("gc-gradle_gc.log-total-collections", "10"),
                ),
        )

    private fun build(values: Array<CustomValue> = emptyArray()): BuildWithResourceUsage =
        BuildWithResourceUsage(
            id = "build-1",
            builtTool = "gradle",
            taskExecution = arrayOf(Task("compile", ":app:compile", "executed", 10, 10)),
            goalExecution = emptyArray(),
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
            tags = arrayOf("variant-a"),
            requestedTask = arrayOf("assemble"),
            buildDuration = 10L,
            buildStartTime = 1L,
            projectName = "demo",
            configuration = 5L,
            execution = metrics(),
            nonExecution = metrics(),
            total = metrics(),
            totalMemory = 0L,
            values = values,
        )

    private fun metrics() =
        PerformanceMetrics(
            buildProcessCpu = metric(),
            allProcessesCpu = metric(),
            allProcessesMemory = metric(),
            buildChildProcessesCpu = metric(),
            buildChildProcessesMemory = metric(),
            buildProcessMemory = metric(),
            diskReadThroughput = metric(),
            diskWriteThroughput = metric(),
            networkUploadThroughput = metric(),
            networkDownloadThroughput = metric(),
        )

    private fun metric() = GeMetric(50L, 51L, 100L, 25L, 75L, 95L)
}
