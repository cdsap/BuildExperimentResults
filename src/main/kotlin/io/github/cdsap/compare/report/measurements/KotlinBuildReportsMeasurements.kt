package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.geapi.client.model.Build
import io.github.cdsap.geapi.client.model.OS
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class KotlinBuildReportsMeasurements(
    private val variantA: List<Build>,
    private val variantB: List<Build>
) {

    private val excludedList = listOf("Start time of werker execution","Start time of task action","Total memory usage at the end of build",
        "Finish gradle part of task execution","Worker submit time")
    fun get(): List<MeasurementWithPercentiles> {
        val measurementsP = mutableListOf<MeasurementWithPercentiles>()
        val valuesVariantA = extracted(variantA)
        val valuesVariantB = extracted(variantB)

        val valuesByTaskAggregated = aggregateBuilds(valuesVariantA)
        val valuesByTaskAggregatedB = aggregateBuilds(valuesVariantB)


        valuesByTaskAggregated.filter { !excludedList.contains(it.key)}
            .forEach {
                val x = valuesByTaskAggregatedB[it.key]
                if (x != null ) {
                    if (it.value.filter {
                            it.contains("ms") || it.contains("GB") || it.contains("MB") || it.contains("KB") || it.contains(
                                "B"
                            )
                        }.isNotEmpty()) {

                        val valuesFormattedA = it.value.map { it.replace(",", "").replace("ms", "").split(" ")[0] }
                        val valuesFormattedB = x.map { it.replace(",", "").replace("ms", "").split(" ")[0] }
                        val qualifierA = if (it.value.first().contains("ms")) "ms" else it.value.first().split(" ")[1]
                        val qualifierB = if (x.first().contains("ms")) "ms" else x.first().split(" ")[1]

                        val varianta =
                            (((valuesFormattedA.sumOf { it.toDouble() } / valuesFormattedA.size) * 100.0).roundToInt() / 100.0)
                        val variantb =
                            (((valuesFormattedB.sumOf { it.toDouble() } / valuesFormattedB.size) * 100.0).roundToInt() / 100.0)
                        val variantaP50 = valuesFormattedA.map { it.toDouble() }.percentile(50.0).roundToLong()
                        val variantbP50 = valuesFormattedB.map { it.toDouble() }.percentile(50.0).roundToLong()
                        val variantaP90 = valuesFormattedA.map { it.toDouble() }.percentile(90.0).roundToLong()
                        val variantbP90 = valuesFormattedB.map { it.toDouble() }.percentile(90.0).roundToLong()
                 //       if (varianta != variantb) {
                            measurementsP.add(
                                MeasurementWithPercentiles(
                                    category = "Kotlin Build Reports",
                                    name = it.key,
                                    variantAMean = "$varianta $qualifierA",
                                    variantBMean = "$variantb $qualifierB",
                                    variantAP50 = "$variantaP50 $qualifierA",
                                    variantBP50 = "$variantbP50 $qualifierB",
                                    variantAP90 = "$variantaP90 $qualifierA",
                                    variantBP90 = "$variantbP90 $qualifierB",
                                    OS = OS.Linux
                                )
                            )
                   //     }
                    } else {
                        val valuesFormattedA = it.value.map { it.replace(",", "").split(" ")[0] }
                        val valuesFormattedB = x.map { it.replace(",", "").split(" ")[0] }
                        val varianta =
                            (valuesFormattedA.sumOf { it.toLong() } / valuesFormattedA.size).toDouble().roundToLong()
                        val variantb = valuesFormattedB.sumOf { it.toLong() } / valuesFormattedB.size
                        val variantaP50 = valuesFormattedA.map { it.toDouble() }.percentile(50.0).roundToLong()
                        val variantbP50 = valuesFormattedB.map { it.toDouble() }.percentile(50.0).roundToLong()
                        val variantaP90 = valuesFormattedA.map { it.toDouble() }.percentile(90.0).roundToLong()
                        val variantbP90 = valuesFormattedB.map { it.toDouble() }.percentile(90.0).roundToLong()

                        if (varianta != variantb) {
                        measurementsP.add(
                            MeasurementWithPercentiles(
                                category = "Kotlin Build Reports",
                                name = it.key,
                                variantAMean = "$varianta",
                                variantBMean = "$variantb",
                                variantAP50 = "$variantaP50",
                                variantBP50 = "$variantbP50",
                                variantAP90 = "$variantaP90",
                                variantBP90 = "$variantbP90",
                                OS = OS.Linux
                            )
                        )
                    }
                    }
                }
            }


        val tasksWithPathA = aggregateBuilds2(valuesVariantA)
        val tasksWithPathB = aggregateBuilds2(valuesVariantB)
        tasksWithPathA.forEach {
            val key = it.key
            if (tasksWithPathB.contains(it.key)) {
                val xa = tasksWithPathB[it.key]

                it.value.filter { !excludedList.contains(it.key)}
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
                                val qualifierA =
                                    if (it.value.first().contains("ms")) "ms" else it.value.first().split(" ")[1]
                                val qualifierB = if (x.first().contains("ms")) "ms" else x.first().split(" ")[1]

                                val varianta =
                                    (((valuesFormattedA.sumOf { it.toDouble() } / valuesFormattedA.size) * 100.0).roundToInt() / 100.0)
                                val variantb =
                                    (((valuesFormattedB.sumOf { it.toDouble() } / valuesFormattedB.size) * 100.0).roundToInt() / 100.0)
                                val variantaP50 = valuesFormattedA.map { it.toDouble() }.percentile(50.0).roundToLong()
                                val variantbP50 = valuesFormattedB.map { it.toDouble() }.percentile(50.0).roundToLong()
                                val variantaP90 = valuesFormattedA.map { it.toDouble() }.percentile(90.0).roundToLong()
                                val variantbP90 = valuesFormattedB.map { it.toDouble() }.percentile(90.0).roundToLong()
                                if (varianta != variantb) {
//                                    val abs = if (variantaP50 == 0L) {
//                                        (abs(variantaP50 - variantbP50) * 100) / variantbP50
//                                    } else {
//                                        (abs(variantaP50 - variantbP50) * 100) / variantaP50
//                                    }
                                    //       if(abs > 25) {
                                    //     println(abs)
                                    measurementsP.add(
                                        MeasurementWithPercentiles(
                                            category = "$key",
                                            name = it.key,
                                            variantAMean = "$varianta $qualifierA",
                                            variantBMean = "$variantb $qualifierB",
                                            variantAP50 = "$variantaP50 $qualifierA",
                                            variantBP50 = "$variantbP50 $qualifierB",
                                            variantAP90 = "$variantaP90 $qualifierA",
                                            variantBP90 = "$variantbP90 $qualifierB",
                                            OS = OS.Linux
                                        )
                                    )
                                    //   }
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
                                            OS = OS.Linux
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

    private fun extracted(
        buildsPerVariant: List<Build>
    ): MutableMap<String, Map<String, MutableList<MetricKotlin>>> {
        val variantBuild = mutableMapOf<String, Map<String, MutableList<MetricKotlin>>>()
        buildsPerVariant.forEach {
            val buildId = it.id
            val buildsWithKotlinBuildReports =
                it.values.filter { it.name.contains("Kotlin") && it.value.contains("Non incremental build because") }
            val tasksWithMetrics = mutableMapOf<String, MutableList<MetricKotlin>>()
            if (buildsWithKotlinBuildReports.isNotEmpty()) {
                buildsWithKotlinBuildReports.forEach {
                    val key = it.name
                    val values = it.value.split("Performance: [")[1].split("]")[0].split(": ")
                    var auxCount = 0
                    while (auxCount < values.size - 1) {
                        var key2 = ""
                        var value = ""
                        if (auxCount == 0) {
                            key2 = values[auxCount]
                        } else {
                            key2 = values[auxCount].split(",").last()
                        }

                        if (values[auxCount + 1].split(",").count() > 2) {

                            // we found issues parsing lines like
                            // 1},2023-07-18T01:17:40,Number of times classpath snapshot is loaded
                            // to avoid that, we need to identify if the "," comes from a byte
                            value = values[auxCount + 1].split(",")[0].replace("}", "")
//                            if ((values[auxCount + 1].contains("GB") || values[auxCount + 1].contains("MB") || values[auxCount + 1].contains(
//                                    "KB"
//                                ) || values[auxCount + 1].contains("B"))
//                            ) {
//                                value =
//                                    values[auxCount + 1].split(",").dropLast(1).joinToString { "" }.replace("}", "")
//                            } else {
//                                value = values[auxCount + 1].split(",")[0].replace("}", "")
//                            }

                        } else {
                            value = values[auxCount + 1].split(",")[0].replace("}", "")
                        }
                        if (!tasksWithMetrics.containsKey(key)) {
                            tasksWithMetrics[key] = mutableListOf()
                        }
                        tasksWithMetrics[key]?.add(MetricKotlin(key2, value))
                        auxCount += 1

                    }
                }
                variantBuild[buildId] = tasksWithMetrics
            }
        }
        return variantBuild
    }

    private fun aggregateBuilds(builds: Map<String, Map<String, MutableList<MetricKotlin>>>): MutableMap<String, MutableList<String>> {
        val valuesByTaskAggregated = mutableMapOf<String, MutableList<String>>()
        builds.forEach {
            it.value.forEach {
                it.value.forEach {
                    if (!valuesByTaskAggregated.containsKey("${it.desc}")) {
                        valuesByTaskAggregated["${it.desc}"] = mutableListOf()
                    }
                    valuesByTaskAggregated["${it.desc}"]?.add(it.value)
                }
            }

        }
        return valuesByTaskAggregated
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

data class MetricKotlin(val desc: String, val value: String)

