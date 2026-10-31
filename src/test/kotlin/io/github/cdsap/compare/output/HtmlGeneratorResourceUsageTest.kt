package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.report.measurements.BuildWithResourceUsageProvider
import io.github.cdsap.compare.report.measurements.MeasurementsByReport
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HtmlGeneratorResourceUsageTest {
    private val metrics = BuildWithResourceUsageProvider()

    @Test
    fun fullCoverageShowsZeroOmittedAndResourceUsageTable() {
        val variants =
            mapOf(
                "variant-a" to listOf(usageBuild(), usageBuild()),
                "variant-b" to listOf(usageBuild(), usageBuild()),
            )
        val html = summaryHtml(variants)

        assertTrue(html.contains("2 included / 0 omitted"))
        assertTrue(html.contains("All processes cpu"))
    }

    @Test
    fun missingTotalsStillShowCoverageAndOmitResourceUsageTable() {
        val variants =
            mapOf(
                "variant-a" to listOf(usageBuild(), usageBuild()),
                "variant-b" to listOf(metrics.withoutTotal(usageBuild()), metrics.withoutTotal(usageBuild())),
            )
        val html = summaryHtml(variants)

        assertTrue(html.contains("2 included / 0 omitted"))
        assertTrue(html.contains("0 included / 2 omitted"))
        assertFalse(html.contains("All processes cpu"))
    }

    private fun summaryHtml(variants: Map<String, List<BuildWithResourceUsage>>): String {
        val report = report()
        val measurements = MeasurementsByReport(report).get(variants)
        return HtmlGenerator(report, chartGenerator = ChartGenerator(report)).generateTableSummary(
            measurements,
            variants.keys.toList(),
            Header(task = "assemble", numberOfBuilds = listOf(2, 2)),
            variants,
        )
    }

    private fun report() =
        Report(
            taskPathReport = false,
            taskTypeReport = false,
            kotlinBuildReport = false,
            processesReport = false,
            buildReport = false,
            resourceUsageReport = true,
            isProfile = false,
            gcReport = false,
            warmupsToDiscard = 0,
            variants = listOf("variant-a", "variant-b"),
            onlyCacheableOutcome = false,
            thresholdTaskDuration = -1,
        )

    private fun usageBuild(): BuildWithResourceUsage =
        BuildWithResourceUsage(
            builtTool = "gradle",
            taskExecution = arrayOf(Task("compile", ":app:compile", "executed", 10, 10)),
            goalExecution = emptyArray(),
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
            buildDuration = 10L,
            execution = metrics.get(),
            nonExecution = metrics.get(),
            total = metrics.get(),
            totalMemory = 0L,
        )
}
