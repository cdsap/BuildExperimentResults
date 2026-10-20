package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
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
        assertTrue(!html.contains("<h2>JFR Diagnostics</h2>"))
    }

    private fun build() =
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
