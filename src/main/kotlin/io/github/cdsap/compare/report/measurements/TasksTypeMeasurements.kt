package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.OS
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class TasksTypeMeasurements(
    private val variantA: List<BuildWithResourceUsage>,
    private val variantB: List<BuildWithResourceUsage>,
    private val report: Report
) {
    fun get(): List<MeasurementWithPercentiles> {
        return getTaskTypeMeasurements()
    }

    private fun getTaskTypeMeasurements(): List<MeasurementWithPercentiles> {
        val variantAAggregatedTaskType = getTasksByType(variantA)
        val variantBAggregatedTaskType = getTasksByType(variantB)

        val measurementsP = mutableListOf<MeasurementWithPercentiles>()

        variantAAggregatedTaskType.forEach {
            val x = variantBAggregatedTaskType[it.key]
            if (x != null) {
                measurementsP.add(
                    MeasurementWithPercentiles(
                        category = "Task Type",
                        name = it.key,
                        variantAMean = "${it.value.sumOf { it.toDouble() }.roundToLong() / it.value.size}",
                        variantBMean = "${x.sumOf { it.toDouble().roundToLong() } / x.size}",
                        variantAP50 = "${it.value.percentile(50.0).roundToLong()}",
                        variantBP50 = "${x.percentile(50.0).roundToLong()}",
                        variantAP90 = "${it.value.percentile(90.0).roundToLong()}",
                        variantBP90 = "${x.percentile(90.0).roundToLong()}",
                        qualifier = "ms",
                        metric = Metric.TASK_TYPE
                    )
                )
            }
        }
        return measurementsP
    }

    private fun getTasksByType(builds: List<BuildWithResourceUsage>): Map<String, MutableList<Long>> {
        val variantAggregatedTaskType = mutableMapOf<String, MutableList<Long>>()
        builds.forEach {
            val tasksExecution = if(report.onlyCacheableOutcome) {
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
