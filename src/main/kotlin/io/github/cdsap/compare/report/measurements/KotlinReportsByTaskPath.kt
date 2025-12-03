package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.CustomValuesPerVariant
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.MetricKotlin
import io.github.cdsap.compare.model.SingleMeasurement
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class KotlinReportsByTaskPath(
    private val kotlinBuildReportsParserCustomValues: CustomValuesPerVariant,
) : KotlinBuildReports() {
    fun get(excludedList: List<String>): List<SingleMeasurement> {
        val measurements = mutableListOf<SingleMeasurement>()
        val tasksWithPathA = aggregateBuilds2(kotlinBuildReportsParserCustomValues.variant)
        tasksWithPathA.forEach {
            val key = it.key

            it.value
                .filter { !excludedList.contains(it.key) }
                .forEach {
                    val builds = it.value.map { format(it) }
                    var qualifier = ""
                    var median: Number
                    if (itHasQualifier(it)) {
                        qualifier = getQualifier(it.value.first())
                        median =
                            ((builds.sumOf { it.toDouble() } / builds.size) * 100.0).roundToInt() / 100.0
                    } else {
                        median = (builds.sumOf { it.toLong() } / builds.size).toDouble().roundToLong()
                    }
                    measurements.add(
                        insertMeasurement(
                            "$key",
                            builds,
                            it.key,
                            median,
                            qualifier,
                            Metric.TASK_KOTLIN_BUILD_REPORT,
                        ),
                    )
                }
        }

        return measurements
    }

    private fun aggregateBuilds2(
        builds: Map<String, Map<String, MutableList<MetricKotlin>>>,
    ): MutableMap<String, MutableMap<String, MutableList<String>>> {
        val valuesByTaskAggregated = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
        builds.forEach {
            it.value.forEach {
                if (!valuesByTaskAggregated.contains(it.key)) {
                    valuesByTaskAggregated[it.key] = mutableMapOf()
                }
                val metrics = valuesByTaskAggregated[it.key]
                it.value.forEach {
                    if (!metrics!!.contains(it.desc)) {
                        metrics[it.desc] = mutableListOf()
                        metrics[it.desc]?.add(it.value)
                    } else {
                        metrics[it.desc]?.add(it.value)
                    }
                }
            }
        }
        return valuesByTaskAggregated
    }
}
