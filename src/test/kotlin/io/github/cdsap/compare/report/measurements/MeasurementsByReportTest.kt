package io.github.cdsap.compare.report.measurements

import com.google.gson.Gson
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.CustomValue
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader

class MeasurementsByReportTest {
    private val buildWithResourceUsageProvider = BuildWithResourceUsageProvider()

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
                onlyCacheableOutcome = false,
                thresholdTaskDuration = -1,
                gcReport = false,
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertTrue(measurements.isNotEmpty())
        assertTrue(measurements.values.any { it.any { it.metric == Metric.KOTLIN_BUILD_REPORT } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.BUILD } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_TYPE } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_PATH } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.PROCESS } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT } })
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
                onlyCacheableOutcome = false,
                thresholdTaskDuration = -1,
                gcReport = false,
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertFalse(measurements.values.any { it.any { it.metric == Metric.KOTLIN_BUILD_REPORT } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.BUILD } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_TYPE } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_PATH } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.PROCESS } })
        assertFalse(measurements.values.any { it.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT } })
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
                onlyCacheableOutcome = false,
                thresholdTaskDuration = -1,
                gcReport = false,
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertTrue(measurements.isNotEmpty())
        assertTrue(measurements.values.any { it.any { it.metric == Metric.KOTLIN_BUILD_REPORT } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.BUILD } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_TYPE } })
        assertFalse(measurements.values.any { it.any { it.metric == Metric.TASK_PATH } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.PROCESS } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT } })
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
                onlyCacheableOutcome = false,
                thresholdTaskDuration = -1,
                gcReport = false,
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertTrue(measurements.isNotEmpty())
        assertTrue(measurements.values.any { it.any { it.metric == Metric.KOTLIN_BUILD_REPORT } })
        assertFalse(measurements.values.any { it.any { it.metric == Metric.BUILD } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_TYPE } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_PATH } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.PROCESS } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT } })
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
                onlyCacheableOutcome = false,
                thresholdTaskDuration = -1,
                gcReport = false,
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertTrue(measurements.isNotEmpty())
        assertTrue(measurements.values.any { it.any { it.metric == Metric.KOTLIN_BUILD_REPORT } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.BUILD } })
        assertFalse(measurements.values.any { it.any { it.metric == Metric.TASK_TYPE } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_PATH } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.PROCESS } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT } })
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
                onlyCacheableOutcome = false,
                thresholdTaskDuration = -1,
                gcReport = false,
            )
        val measurements = MeasurementsByReport(report).get(FilterBuildsPerVariant(report).get(builds))

        assertTrue(measurements.isNotEmpty())
        assertTrue(measurements.values.any { it.any { it.metric == Metric.KOTLIN_BUILD_REPORT } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.BUILD } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_TYPE } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_PATH } })
        assertFalse(measurements.values.any { it.any { it.metric == Metric.PROCESS } })
        assertTrue(measurements.values.any { it.any { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT } })
    }

    @Test
    fun resourceUsageAndGcReportAreSelectedThroughProviderList() {
        val builds =
            listOf(
                BuildWithResourceUsage(
                    id = "1",
                    builtTool = "gradle",
                    taskExecution =
                        arrayOf(
                            Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                        ),
                    goalExecution = emptyArray(),
                    avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                    values =
                        arrayOf(
                            CustomValue("TestGC-total-collections", "5"),
                        ),
                    execution = buildWithResourceUsageProvider.get(),
                    nonExecution = buildWithResourceUsageProvider.get(),
                    total = buildWithResourceUsageProvider.get(),
                    totalMemory = 0L,
                ),
                BuildWithResourceUsage(
                    id = "2",
                    builtTool = "gradle",
                    taskExecution =
                        arrayOf(
                            Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1600, 10),
                        ),
                    goalExecution = emptyArray(),
                    avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                    values =
                        arrayOf(
                            CustomValue("TestGC-total-collections", "6"),
                        ),
                    execution = buildWithResourceUsageProvider.get(),
                    nonExecution = buildWithResourceUsageProvider.get(),
                    total = buildWithResourceUsageProvider.get(),
                    totalMemory = 0L,
                ),
            )
        val report =
            Report(
                taskPathReport = false,
                taskTypeReport = false,
                kotlinBuildReport = false,
                processesReport = false,
                buildReport = false,
                resourceUsageReport = true,
                isProfile = false,
                warmupsToDiscard = 0,
                variants = listOf("variant-a"),
                experimentId = "154",
                onlyCacheableOutcome = false,
                thresholdTaskDuration = -1,
                gcReport = true,
            )
        val measurements = MeasurementsByReport(report).get(mapOf("variant-a" to builds))

        assertTrue(measurements.isNotEmpty())
        val variantMeasurements = measurements.getValue("variant-a")
        assertTrue(variantMeasurements.any { it.metric == Metric.RESOURCE_USAGE })
        assertTrue(variantMeasurements.any { it.metric == Metric.GC_REPORT })
        assertFalse(variantMeasurements.any { it.metric == Metric.BUILD })
        assertFalse(variantMeasurements.any { it.metric == Metric.PROCESS })
        assertFalse(variantMeasurements.any { it.metric == Metric.TASK_TYPE })
        assertFalse(variantMeasurements.any { it.metric == Metric.TASK_PATH })
        assertFalse(variantMeasurements.any { it.metric == Metric.KOTLIN_BUILD_REPORT })
        val firstResourceIndex = variantMeasurements.indexOfFirst { it.metric == Metric.RESOURCE_USAGE }
        val firstGcIndex = variantMeasurements.indexOfFirst { it.metric == Metric.GC_REPORT }
        assertTrue(firstResourceIndex >= 0)
        assertTrue(firstGcIndex > firstResourceIndex)
    }

    private fun builds(): List<BuildWithResourceUsage> {
        val builds: List<BuildWithResourceUsage> =
            Gson()
                .fromJson(
                    BufferedReader(InputStreamReader(javaClass.classLoader.getResourceAsStream("outcome.json"))).readText(),
                    Array<BuildWithResourceUsage>::class.java,
                ).toList()
        return builds
    }
}
