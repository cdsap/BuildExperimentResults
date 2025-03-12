package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.CustomValuesPerVariant
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.MetricKotlin
import io.github.cdsap.compare.model.SingleMeasurement
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class KotlinReportsAggregated(private val kotlinBuildReportsParserCustomValues: CustomValuesPerVariant) :
    KotlinBuildReports() {

    fun get(excludedList: List<String>): List<SingleMeasurement> {
        val measurements = mutableListOf<SingleMeasurement>()
        val metricsAggregatedVariantA = aggregateBuilds(kotlinBuildReportsParserCustomValues.variant)

        metricsAggregatedVariantA.filter { !excludedList.contains(it.key) }
            .forEach {
                val buildsA = it.value.map { format(it) }
                var qualifier = ""
                var median: Number
                if (itHasQualifier(it)) {
                    qualifier = getQualifier(it.value.first())
                    median = ((buildsA.sumOf { it.toDouble() } / buildsA.size) * 100.0).roundToInt() / 100.0
                } else {
                    median = (buildsA.sumOf { it.toLong() } / buildsA.size).toDouble().roundToLong()
                }
                measurements.add(
                    insertMeasurement(
                        "Kotlin Build Reports",
                        buildsA,
                        it.key,
                        median,
                        qualifier,
                        Metric.KOTLIN_BUILD_REPORT
                    )
                )
            }

        return measurements
    }

    fun aggregateBuilds(builds: Map<String, Map<String, MutableList<MetricKotlin>>>):
        MutableMap<String, MutableList<String>> {
        val valuesByTaskAggregated = mutableMapOf<String, MutableList<String>>()
        builds.forEach {
            it.value.forEach {
                it.value.forEach {
                    if (!valuesByTaskAggregated.containsKey(it.desc)) {
                        valuesByTaskAggregated[it.desc] = mutableListOf()
                    }
                    valuesByTaskAggregated[it.desc]?.add(it.value)
                }
            }
        }
        return valuesByTaskAggregated
    }
}
