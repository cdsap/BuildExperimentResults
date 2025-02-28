package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class TasksTypeMeasurements(
    private val variant: List<BuildWithResourceUsage>,
    private val report: Report
) {
    fun get(): List<SingleMeasurement> {
        return getTaskTypeMeasurements()
    }

    private fun getTaskTypeMeasurements(): List<SingleMeasurement> {
        val measurements = mutableListOf<SingleMeasurement>()
        val variantAggregatedTaskType = getTasksByType(variant)
        variantAggregatedTaskType.forEach {
            measurements.add(
                SingleMeasurement(
                    category = "Task Type",
                    name = it.key,
                    variantMean = "${it.value.sumOf { it.toDouble() }.roundToLong() / it.value.size}",
                    variantP50 = "${it.value.percentile(50.0).roundToLong()}",
                    variantP90 = "${it.value.percentile(90.0).roundToLong()}",
                    qualifier = "ms",
                    metric = Metric.TASK_TYPE
                )
            )
        }

        return measurements
    }

    private fun getTasksByType(builds: List<BuildWithResourceUsage>): Map<String, MutableList<Long>> {
        val variantAggregatedTaskType = mutableMapOf<String, MutableList<Long>>()
        builds.forEach {
            val tasksExecution = if (report.onlyCacheableOutcome) {
                it.taskExecution.filter { (it.avoidanceOutcome == "executed_cacheable") }
            } else {
                it.taskExecution.toList()
            }
            tasksExecution.forEach {
                if (variantAggregatedTaskType.contains(it.taskType)) {
                    variantAggregatedTaskType[it.taskType]?.add(it.duration)
                } else {
                    variantAggregatedTaskType[it.taskType] = mutableListOf()
                    variantAggregatedTaskType[it.taskType]?.add(it.duration)
                }
            }
        }
        return variantAggregatedTaskType
    }
}
