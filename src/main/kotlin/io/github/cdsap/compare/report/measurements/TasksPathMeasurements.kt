package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class TasksPathMeasurements(
    private val variant: List<BuildWithResourceUsage>,
    private val report: Report
) {
    fun get(): List<SingleMeasurement> {
        return getTaskPathMeasurements()
    }

    private fun getTaskPathMeasurements(): List<SingleMeasurement> {
        val variantAAggregatedTaskPath = getTasksByPath(variant)
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
                        metric = Metric.TASK_PATH
                    )
                )
            }

        return measurementsP
    }

    private fun getTasksByPath(builds: List<BuildWithResourceUsage>): Map<String, MutableList<Long>> {
        val variantAggregatedTaskPath = mutableMapOf<String, MutableList<Long>>()
        builds.forEach {
            val tasksExecution = if (report.onlyCacheableOutcome) {
                it.taskExecution.filter { (it.avoidanceOutcome == "executed_cacheable") }
            } else {
                it.taskExecution.toList()
            }

            tasksExecution.forEach {
                if (variantAggregatedTaskPath.contains(it.taskPath)) {
                    variantAggregatedTaskPath[it.taskPath]?.add(it.duration)
                } else {
                    variantAggregatedTaskPath[it.taskPath] = mutableListOf()
                    variantAggregatedTaskPath[it.taskPath]?.add(it.duration)
                }
            }
        }

        return variantAggregatedTaskPath
    }
}
