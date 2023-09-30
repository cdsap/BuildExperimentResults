package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.CustomValuesPerVariant
import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.MetricKotlin
import io.github.cdsap.geapi.client.model.OS
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class KotlinReportsByTaskPath(val kotlinBuildReportsParserCustomValues: CustomValuesPerVariant) {

    fun get(excludedList: List<String>): List<MeasurementWithPercentiles> {
        val measurementsP = mutableListOf<MeasurementWithPercentiles>()
        val tasksWithPathA = aggregateBuilds2(kotlinBuildReportsParserCustomValues.variantA)
        val tasksWithPathB = aggregateBuilds2(kotlinBuildReportsParserCustomValues.variantB)
        tasksWithPathA.forEach {
            val key = it.key
            if (tasksWithPathB.contains(it.key)) {
                val xa = tasksWithPathB[it.key]

                it.value.filter { !excludedList.contains(it.key) }
                    .forEach {
                        val x = xa!![it.key]
                        if (x != null && it.value.size == x.size) {
                            if (it.value.filter {
                                    it.contains("ms") || it.contains("GB") || it.contains("MB") || it.contains("KB") || it.contains(
                                        "B"
                                    )
                                }.isNotEmpty()) {

                                val valuesFormattedA =
                                    it.value.map { it.replace(",", "").replace("ms", "").split(" ")[0] }
                                val valuesFormattedB = x.map { it.replace(",", "").replace("ms", "").split(" ")[0] }
                                val qualifier =
                                    if (it.value.first().contains("ms")) "ms" else it.value.first().split(" ")[1]

                                val varianta =
                                    (((valuesFormattedA.sumOf { it.toDouble() } / valuesFormattedA.size) * 100.0).roundToInt() / 100.0)
                                val variantb =
                                    (((valuesFormattedB.sumOf { it.toDouble() } / valuesFormattedB.size) * 100.0).roundToInt() / 100.0)
                                val variantaP50 = valuesFormattedA.map { it.toDouble() }.percentile(50.0).roundToLong()
                                val variantbP50 = valuesFormattedB.map { it.toDouble() }.percentile(50.0).roundToLong()
                                val variantaP90 = valuesFormattedA.map { it.toDouble() }.percentile(90.0).roundToLong()
                                val variantbP90 = valuesFormattedB.map { it.toDouble() }.percentile(90.0).roundToLong()
                                if (varianta != variantb) {
                                    measurementsP.add(
                                        MeasurementWithPercentiles(
                                            category = "$key",
                                            name = it.key,
                                            variantAMean = "$varianta",
                                            variantBMean = "$variantb",
                                            variantAP50 = "$variantaP50",
                                            variantBP50 = "$variantbP50",
                                            variantAP90 = "$variantaP90",
                                            variantBP90 = "$variantbP90",
                                            OS = OS.Linux,
                                            qualifier = qualifier,
                                            metric = Metric.TASK_KOTLIN_BUILD_REPORT
                                        )
                                    )
                                }
                            } else {
                                val valuesFormattedA = it.value.map { it.replace(",", "").split(" ")[0] }
                                val valuesFormattedB = x.map { it.replace(",", "").split(" ")[0] }
                                val varianta =
                                    (valuesFormattedA.sumOf { it.toLong() } / valuesFormattedA.size).toDouble()
                                        .roundToLong()
                                val variantb = valuesFormattedB.sumOf { it.toLong() } / valuesFormattedB.size
                                val variantaP50 = valuesFormattedA.map { it.toDouble() }.percentile(50.0).roundToLong()
                                val variantbP50 = valuesFormattedB.map { it.toDouble() }.percentile(50.0).roundToLong()
                                val variantaP90 = valuesFormattedA.map { it.toDouble() }.percentile(90.0).roundToLong()
                                val variantbP90 = valuesFormattedB.map { it.toDouble() }.percentile(90.0).roundToLong()

                                if (varianta != variantb) {
                                    measurementsP.add(
                                        MeasurementWithPercentiles(
                                            category = "$key",
                                            name = it.key,
                                            variantAMean = "$varianta",
                                            variantBMean = "$variantb",
                                            variantAP50 = "$variantaP50",
                                            variantBP50 = "$variantbP50",
                                            variantAP90 = "$variantaP90",
                                            variantBP90 = "$variantbP90",
                                            OS = OS.Linux,
                                            qualifier = "",
                                            metric = Metric.TASK_KOTLIN_BUILD_REPORT
                                        )
                                    )
                                }
                            }
                        }

                    }
            }
        }
        return measurementsP
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
