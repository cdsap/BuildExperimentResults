package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.geapi.client.model.Build
import io.github.cdsap.geapi.client.model.OS
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class TasksPathMeasurements(
    private val variantA: List<Build>,
    private val variantB: List<Build>
) {
    fun get(): List<MeasurementWithPercentiles> {
        return getTaskPathMeasurements()
    }

    private fun getTaskPathMeasurements(): List<MeasurementWithPercentiles> {
        val variantAAggregatedTaskPath = getTasksByPath(variantA)
        val variantBAggregatedTaskPath = getTasksByPath(variantB)
        val measurementsP = mutableListOf<MeasurementWithPercentiles>()

        variantAAggregatedTaskPath
            .filter { it.value.sumOf { it } / it.value.size > 1000 }
            .forEach {
                val x = variantBAggregatedTaskPath[it.key]
                if (x != null) {
                    measurementsP.add(
                        MeasurementWithPercentiles(
                            category = "Task Path",
                            name = it.key,
                            variantAMean = "${it.value.sumOf { it.toDouble() }.roundToLong() / it.value.size}",
                            variantBMean = "${x.sumOf { it.toDouble().roundToLong() } / x.size}",
                            variantAP50 = "${it.value.percentile(50.0).roundToLong()}",
                            variantBP50 = "${x.percentile(50.0).roundToLong()}",
                            variantAP90 = "${it.value.percentile(90.0).roundToLong()}",
                            variantBP90 = "${x.percentile(90.0).roundToLong()}",
                            OS = OS.Linux,
                            qualifier = "ms",
                            metric = Metric.TASK_PATH
                        )
                    )
                }

            }
        return measurementsP
    }

    private fun getTasksByPath(builds: List<Build>): Map<String, MutableList<Long>> {
        val variantAggregatedTaskPath = mutableMapOf<String, MutableList<Long>>()
        builds.forEach {
            it.taskExecution.filter { (it.avoidanceOutcome == "executed_cacheable") }
                .forEach {

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
