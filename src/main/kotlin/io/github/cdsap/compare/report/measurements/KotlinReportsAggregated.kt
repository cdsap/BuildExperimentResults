package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.CustomValuesPerVariant
import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.MetricKotlin
import io.github.cdsap.geapi.client.model.OS
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class KotlinReportsAggregated(private val kotlinBuildReportsParserCustomValues: CustomValuesPerVariant) :
    KotlinBuildReports() {

    fun get(excludedList: List<String>): List<MeasurementWithPercentiles> {
        val measurements = mutableListOf<MeasurementWithPercentiles>()
        val metricsAggregatedVariantA = aggregateBuilds(kotlinBuildReportsParserCustomValues.variantA)
        val metricsAggregatedVariantB = aggregateBuilds(kotlinBuildReportsParserCustomValues.variantB)

        metricsAggregatedVariantA.filter { !excludedList.contains(it.key) }
            .forEach {
                val metricVariantB = metricsAggregatedVariantB[it.key]
                if (metricVariantB != null) {
                    val buildsA = it.value.map { format(it) }
                    val buildsB = metricVariantB.map { format(it) }
                    var qualifier = ""
                    var medianA: Number
                    var medianB: Number
                    if (itHasQualifier(it)) {
                        qualifier = getQualifier(it.value.first())
                        medianA = ((buildsA.sumOf { it.toDouble() } / buildsA.size) * 100.0).roundToInt() / 100.0
                        medianB = ((buildsB.sumOf { it.toDouble() } / buildsB.size) * 100.0).roundToInt() / 100.0
                    } else {
                        medianA = (buildsA.sumOf { it.toLong() } / buildsA.size).toDouble().roundToLong()
                        medianB = buildsB.sumOf { it.toLong() } / buildsB.size
                    }
                    measurements.add(
                        insertMeasurement(
                            "Kotlin Build Reports",
                            buildsA,
                            buildsB,
                            it.key,
                            medianA,
                            medianB,
                            qualifier,
                            Metric.KOTLIN_BUILD_REPORT
                        )
                    )

                }

            }
        return measurements
    }

    private fun aggregateBuilds(builds: Map<String, Map<String, MutableList<MetricKotlin>>>):
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
