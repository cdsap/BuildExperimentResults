package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.CustomValuesPerVariant
import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.MetricKotlin
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class KotlinReportsByTaskPath(private val kotlinBuildReportsParserCustomValues: CustomValuesPerVariant) :
    KotlinBuildReports() {

    fun get(excludedList: List<String>): List<MeasurementWithPercentiles> {
        val measurements = mutableListOf<MeasurementWithPercentiles>()
        val tasksWithPathA = aggregateBuilds2(kotlinBuildReportsParserCustomValues.variantA)
        val tasksWithPathB = aggregateBuilds2(kotlinBuildReportsParserCustomValues.variantB)
        tasksWithPathA.forEach {
            val key = it.key
            if (tasksWithPathB.contains(it.key)) {
                val variantBBuilds = tasksWithPathB[it.key]

                it.value.filter { !excludedList.contains(it.key) }
                    .forEach {
                        val buildsVariantB = variantBBuilds!![it.key]
                        if (buildsVariantB != null && it.value.size == buildsVariantB.size) {
                            val buildsA = it.value.map { format(it) }
                            val buildsB = buildsVariantB.map { format(it) }
                            var qualifier = ""
                            var medianA: Number
                            var medianB: Number
                            if (itHasQualifier(it)) {
                                qualifier = getQualifier(it.value.first())
                                medianA =
                                    ((buildsA.sumOf { it.toDouble() } / buildsA.size) * 100.0).roundToInt() / 100.0
                                medianB =
                                    ((buildsB.sumOf { it.toDouble() } / buildsB.size) * 100.0).roundToInt() / 100.0
                            } else {
                                medianA = (buildsA.sumOf { it.toLong() } / buildsA.size).toDouble().roundToLong()
                                medianB = buildsB.sumOf { it.toLong() } / buildsB.size
                            }
                            measurements.add(
                                insertMeasurement(
                                    "$key",
                                    buildsA,
                                    buildsB,
                                    it.key,
                                    medianA,
                                    medianB,
                                    qualifier,
                                    Metric.TASK_KOTLIN_BUILD_REPORT
                                )
                            )
                        }
                    }
            }
        }
        return measurements
    }

    private fun aggregateBuilds2(builds: Map<String, Map<String, MutableList<MetricKotlin>>>): MutableMap<String, MutableMap<String, MutableList<String>>> {
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
