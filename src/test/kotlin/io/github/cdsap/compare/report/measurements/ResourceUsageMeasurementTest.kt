package io.github.cdsap.compare.report.measurements

import com.google.gson.Gson
import com.google.gson.JsonParser
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.PerformanceMetrics
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResourceUsageMeasurementTest {
    private val metrics = BuildWithResourceUsageProvider()

    @Test
    fun mixedNullTotalsInOneVariantDoNotThrowAndUseIncludedScansOnly() {
        val builds =
            listOf(
                usageBuild(total = metrics.get()),
                usageBuild(total = null),
            )

        val measurements = ResourceUsageMeasurement(builds).get()

        assertEquals(6, measurements.size)
        assertTrue(measurements.all { it.metric == Metric.RESOURCE_USAGE })
        assertEquals("100.0", measurements.first { it.category == "All processes cpu" }.variantMean)
    }

    @Test
    fun comparisonIsEmittedWhenEveryVariantHasAtLeastOneTotal() {
        val variants =
            mapOf(
                "variant-a" to listOf(usageBuild(total = metrics.get()), usageBuild(total = metrics.get())),
                "variant-b" to listOf(usageBuild(total = metrics.get()), usageBuild(total = null)),
            )

        val measurements = MeasurementsByReport(report()).get(variants)

        assertEquals(6, measurements.getValue("variant-a").count { it.metric == Metric.RESOURCE_USAGE })
        assertEquals(6, measurements.getValue("variant-b").count { it.metric == Metric.RESOURCE_USAGE })
        assertEquals(
            "100.0",
            measurements.getValue("variant-b").first { it.category == "All processes cpu" }.variantMean,
        )
    }

    @Test
    fun comparisonIsOmittedWhenAVariantHasNoTotals() {
        val variants =
            mapOf(
                "variant-a" to listOf(usageBuild(total = metrics.get()), usageBuild(total = metrics.get())),
                "variant-b" to listOf(usageBuild(total = null), usageBuild(total = null)),
            )

        val measurements = MeasurementsByReport(report()).get(variants)

        assertTrue(measurements.getValue("variant-a").none { it.metric == Metric.RESOURCE_USAGE })
        assertTrue(measurements.getValue("variant-b").none { it.metric == Metric.RESOURCE_USAGE })
    }

    @Test
    fun fullCoverageEmitsSixResourceUsageCategories() {
        val builds = listOf(usageBuild(total = metrics.get()), usageBuild(total = metrics.get()))

        val measurements = ResourceUsageMeasurement(builds).get()

        assertEquals(6, measurements.size)
        assertEquals(
            setOf(
                "All processes cpu",
                "All processes memory",
                "Build process cpu",
                "Build processes memory",
                "Build child processes cpu",
                "Build child processes memory",
            ),
            measurements.map { it.category }.toSet(),
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
            experimentId = "exp",
            onlyCacheableOutcome = false,
            thresholdTaskDuration = -1,
        )

    private fun usageBuild(total: PerformanceMetrics?): BuildWithResourceUsage {
        val withTotal =
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
        if (total != null) {
            return withTotal
        }
        val json = JsonParser.parseString(Gson().toJson(withTotal)).asJsonObject
        json.remove("total")
        val parsed = Gson().fromJson(json, BuildWithResourceUsage::class.java)
        val field = BuildWithResourceUsage::class.java.getDeclaredField("total")
        field.isAccessible = true
        if (field.get(parsed) != null) {
            field.set(parsed, null)
        }
        return parsed
    }
}
