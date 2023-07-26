package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.geapi.client.model.Build
import io.github.cdsap.geapi.client.model.OS
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class TasksMeasurements(
    private val variantA: List<Build>,
    private val variantB: List<Build>
) {
    fun get(): List<MeasurementWithPercentiles> {
        return getTaskTypeMeasurements() + getTaskPathMeasurements()
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
                            variantAMean = "${it.value.sumOf { it.toDouble() }.roundToLong() / it.value.size} ms",
                            variantBMean = "${x.sumOf { it.toDouble().roundToLong() } / x.size} ms",
                            variantAP50 = "${it.value.percentile(50.0).roundToLong()} ms",
                            variantBP50 = "${x.percentile(50.0).roundToLong()} ms",
                            variantAP90 = "${it.value.percentile(90.0).roundToLong()} ms",
                            variantBP90 = "${x.percentile(90.0).roundToLong()} ms",
                            OS = OS.Linux
                        )
                    )
                }

            }
        return measurementsP
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
                        variantAMean = "${it.value.sumOf { it.toDouble() }.roundToLong() / it.value.size} ms",
                        variantBMean = "${x.sumOf { it.toDouble().roundToLong() } / x.size} ms",
                        variantAP50 = "${it.value.percentile(50.0).roundToLong()} ms",
                        variantBP50 = "${x.percentile(50.0).roundToLong()} ms",
                        variantAP90 = "${it.value.percentile(90.0).roundToLong()} ms",
                        variantBP90 = "${x.percentile(90.0).roundToLong()} ms",
                        OS = OS.Linux
                    )
                )
            }
        }
        return measurementsP
    }

    fun getTasksByType(builds: List<Build>): Map<String, MutableList<Long>> {
        val variantAggregatedTaskType = mutableMapOf<String, MutableList<Long>>()
        builds.forEach {
            it.taskExecution.filter { (it.avoidanceOutcome == "executed_cacheable") }
                .forEach {
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

    fun getTasksByPath(builds: List<Build>): Map<String, MutableList<Long>> {
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
