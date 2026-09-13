package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class TasksPathMeasurements(
    private val variant: List<BuildWithResourceUsage>,
    private val report: Report,
) {
    fun get(): List<SingleMeasurement> = getTaskPathMeasurements()

    private fun getTaskPathMeasurements(): List<SingleMeasurement> {
        val variantAAggregatedTaskPath =
            TaskExecutionAggregator(report.onlyCacheableOutcome).aggregate(variant) { it.taskPath }
        val measurementsP = mutableListOf<SingleMeasurement>()
        variantAAggregatedTaskPath
            .filter { it.value.sumOf { it } / it.value.size > report.thresholdTaskDuration }
            .forEach {
                measurementsP.add(
                    SingleMeasurement(
                        category = "Task Path",
                        name = it.key,
                        variantMean = "${it.value.sumOf { it.toDouble() }.roundToLong() / it.value.size}",
                        variantP50 = "${it.value.percentile(50.0).roundToLong()}",
                        variantP90 = "${it.value.percentile(90.0).roundToLong()}",
                        qualifier = "ms",
                        metric = Metric.TASK_PATH,
                    ),
                )
            }

        return measurementsP
    }
}
