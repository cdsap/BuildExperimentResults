package io.github.cdsap.compare.output

import ConsoleGenerator
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

class ConsoleGeneratorResourceUsageTest {
    private val metrics = BuildWithResourceUsageProvider()

    @Test
    fun fullCoverageShowsZeroOmittedAndResourceUsageRows() {
        val variants =
            mapOf(
                "variant-a" to listOf(usageBuild(), usageBuild()),
                "variant-b" to listOf(usageBuild(), usageBuild()),
            )
        val console = console(variants)

        assertTrue(console.contains("2 included / 0 omitted"))
        assertTrue(console.contains("all processes cpu"))
    }

    @Test
    fun missingTotalsStillShowCoverageAndOmitResourceUsageRows() {
        val variants =
            mapOf(
                "variant-a" to listOf(usageBuild(), usageBuild()),
                "variant-b" to listOf(metrics.withoutTotal(usageBuild()), metrics.withoutTotal(usageBuild())),
            )
        val console = console(variants)

        assertTrue(console.contains("2 included / 0 omitted"))
        assertTrue(console.contains("0 included / 2 omitted"))
        assertFalse(console.contains("all processes cpu"))
    }

    private fun console(variants: Map<String, List<BuildWithResourceUsage>>): String {
        val report = report()
        val measurements = MeasurementsByReport(report).get(variants)
        return ConsoleGenerator(report).generate(
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
