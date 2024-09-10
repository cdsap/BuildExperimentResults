package io.github.cdsap.compare.report.measurements

import com.google.gson.Gson
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader

class MeasurementsByReportTest {
    private val BuildWithResourceUsageProvider = BuildWithResourceUsageProvider()

    @Test
    fun allMetricsAreReturned() {
        val builds = builds()
        val report =
            Report(
                taskPathReport = true,
                taskTypeReport = true,
                kotlinBuildReport = true,
                processesReport = true,
                buildReport = true,
                resourceUsageReport = false,
                isProfile = false,
                warmupsToDiscard = 2,
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                experimentId = "154",
                onlyCacheableOutcome = false
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertTrue(measurements.isNotEmpty())
        assertTrue(measurements.any { it.metric == Metric.KOTLIN_BUILD_REPORT })
        assertTrue(measurements.any { it.metric == Metric.BUILD })
        assertTrue(measurements.any { it.metric == Metric.TASK_TYPE })
        assertTrue(measurements.any { it.metric == Metric.TASK_PATH })
        assertTrue(measurements.any { it.metric == Metric.PROCESS })
        assertTrue(measurements.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT })
    }

    @Test
    fun allMetricsAreReturnedExceptKotlinBuildReport() {
        val builds = builds()
        val report =
            Report(
                taskPathReport = true,
                taskTypeReport = true,
                kotlinBuildReport = false,
                processesReport = true,
                buildReport = true,
                isProfile = false,
                resourceUsageReport = false,
                warmupsToDiscard = 2,
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                experimentId = "154",
                onlyCacheableOutcome = false
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertTrue(measurements.isNotEmpty())
        assertFalse(measurements.any { it.metric == Metric.KOTLIN_BUILD_REPORT })
        assertTrue(measurements.any { it.metric == Metric.BUILD })
        assertTrue(measurements.any { it.metric == Metric.TASK_TYPE })
        assertTrue(measurements.any { it.metric == Metric.TASK_PATH })
        assertTrue(measurements.any { it.metric == Metric.PROCESS })
        assertFalse(measurements.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT })
    }

    @Test
    fun allMetricsAreReturnedExceptTaskPath() {
        val builds = builds()
        val report =
            Report(
                taskPathReport = false,
                taskTypeReport = true,
                kotlinBuildReport = true,
                processesReport = true,
                buildReport = true,
                isProfile = false,
                resourceUsageReport = false,
                warmupsToDiscard = 2,
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                experimentId = "154",
                onlyCacheableOutcome = false
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertTrue(measurements.isNotEmpty())
        assertTrue(measurements.any { it.metric == Metric.KOTLIN_BUILD_REPORT })
        assertTrue(measurements.any { it.metric == Metric.BUILD })
        assertTrue(measurements.any { it.metric == Metric.TASK_TYPE })
        assertFalse(measurements.any { it.metric == Metric.TASK_PATH })
        assertTrue(measurements.any { it.metric == Metric.PROCESS })
        assertTrue(measurements.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT })
    }

    @Test
    fun allMetricsAreReturnedExceptBuildReport() {
        val builds = builds()
        val report =
            Report(
                taskPathReport = true,
                taskTypeReport = true,
                kotlinBuildReport = true,
                processesReport = true,
                buildReport = false,
                isProfile = false,
                resourceUsageReport = false,
                warmupsToDiscard = 2,
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                experimentId = "154",
                onlyCacheableOutcome = false
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertTrue(measurements.isNotEmpty())
        assertTrue(measurements.any { it.metric == Metric.KOTLIN_BUILD_REPORT })
        assertFalse(measurements.any { it.metric == Metric.BUILD })
        assertTrue(measurements.any { it.metric == Metric.TASK_TYPE })
        assertTrue(measurements.any { it.metric == Metric.TASK_PATH })
        assertTrue(measurements.any { it.metric == Metric.PROCESS })
        assertTrue(measurements.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT })
    }

    @Test
    fun allMetricsAreReturnedExceptTaskTypeReport() {
        val builds = builds()
        val report =
            Report(
                taskPathReport = true,
                taskTypeReport = false,
                kotlinBuildReport = true,
                processesReport = true,
                buildReport = true,
                isProfile = false,
                resourceUsageReport = false,
                warmupsToDiscard = 2,
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                experimentId = "154",
                onlyCacheableOutcome = false
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertTrue(measurements.isNotEmpty())
        assertTrue(measurements.any { it.metric == Metric.KOTLIN_BUILD_REPORT })
        assertTrue(measurements.any { it.metric == Metric.BUILD })
        assertFalse(measurements.any { it.metric == Metric.TASK_TYPE })
        assertTrue(measurements.any { it.metric == Metric.TASK_PATH })
        assertTrue(measurements.any { it.metric == Metric.PROCESS })
        assertTrue(measurements.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT })
    }

    @Test
    fun allMetricsAreReturnedExceptProcess() {
        val builds = builds()
        val report =
            Report(
                taskPathReport = true,
                taskTypeReport = true,
                kotlinBuildReport = true,
                processesReport = false,
                buildReport = true,
                isProfile = false,
                resourceUsageReport = false,
                warmupsToDiscard = 2,
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                experimentId = "154",
                onlyCacheableOutcome = false
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertTrue(measurements.isNotEmpty())
        assertTrue(measurements.any { it.metric == Metric.KOTLIN_BUILD_REPORT })
        assertTrue(measurements.any { it.metric == Metric.BUILD })
        assertTrue(measurements.any { it.metric == Metric.TASK_TYPE })
        assertTrue(measurements.any { it.metric == Metric.TASK_PATH })
        assertFalse(measurements.any { it.metric == Metric.PROCESS })
        assertTrue(measurements.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT })
    }

    private fun builds(): List<BuildWithResourceUsage> {
        val builds: List<BuildWithResourceUsage> =
            Gson().fromJson(
                BufferedReader(InputStreamReader(javaClass.classLoader.getResourceAsStream("outcome.json"))).readText(),
                Array<BuildWithResourceUsage>::class.java
            ).toList()
        return builds
    }
}
