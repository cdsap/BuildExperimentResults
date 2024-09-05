package io.github.cdsap.compare.report.measurements.parser

import io.github.cdsap.compare.model.CustomValuesPerVariant
import io.github.cdsap.compare.model.MetricKotlin
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class KotlinBuildReportsParserCustomValues(
    private val variantA: List<BuildWithResourceUsage>,
    private val variantB: List<BuildWithResourceUsage>
) {
    fun parse(): CustomValuesPerVariant {
        return CustomValuesPerVariant(extracted(variantA), extracted(variantB))
    }

    private fun extracted(
        buildsPerVariant: List<BuildWithResourceUsage>
    ): MutableMap<String, Map<String, MutableList<MetricKotlin>>> {
        val variantBuild = mutableMapOf<String, Map<String, MutableList<MetricKotlin>>>()
        buildsPerVariant.forEach {
            val buildId = it.id
            val buildsWithKotlinBuildReports =
                it.values.filter { it.name.contains("Kotlin") && it.value.contains("Kotlin language version") && it.value.contains("; Performance: [") }
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
}
