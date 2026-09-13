package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class TasksTypeMeasurements(
    private val variant: List<BuildWithResourceUsage>,
    private val report: Report,
) {
    fun get(): List<SingleMeasurement> = getTaskTypeMeasurements()

    private fun getTaskTypeMeasurements(): List<SingleMeasurement> {
        val measurements = mutableListOf<SingleMeasurement>()
        val variantAggregatedTaskType =
            TaskExecutionAggregator(report.onlyCacheableOutcome).aggregate(variant) { it.taskType }
        variantAggregatedTaskType.forEach {
            measurements.add(
                SingleMeasurement(
                    category = "Task Type",
                    name = it.key,
                    variantMean = "${it.value.sumOf { it.toDouble() }.roundToLong() / it.value.size}",
                    variantP50 = "${it.value.percentile(50.0).roundToLong()}",
                    variantP90 = "${it.value.percentile(90.0).roundToLong()}",
                    qualifier = "ms",
                    metric = Metric.TASK_TYPE,
                ),
            )
        }

        return measurements
    }
}
