package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.domain.impl.GetBuildScansWithQueryImpl
import io.github.cdsap.geapi.client.model.*
import io.github.cdsap.geapi.client.repository.impl.GradleRepositoryImpl
import io.github.cdsap.geapi.domain.model.Task
import io.github.cdsap.compare.model.Measurement
import io.github.cdsap.compare.view.ExperimentView
import io.github.cdsap.geapi.client.domain.impl.GetCachePerformanceImpl


class ExperimentReport(
    private val filter: Filter,
    private val repository: GradleRepositoryImpl
) {

    suspend fun process() {
        val getBuildScans = GetBuildScansWithQueryImpl(repository).get(filter)
        val getOutcome = GetCachePerformanceImpl(repository)
        val outcome = getOutcome.get(getBuildScans, filter).filter { it.tags.contains(filter.experimentId) }
        if (filter.variants == null) {
            throw IllegalArgumentException("Variants can not be null")
        } else {
            val variants = filter.variants!!.split(",")
            val variantA = "${filter.experimentId}_variant_experiment_${variants[0].trim()}"
            val variantB = "${filter.experimentId}_variant_experiment_${variants[1].trim()}"
            outcome.map {
                if (it.tags.contains("experiment") && it.tags.contains(variantA)) {
                    it.experiment = Experiment.VARIANT_A
                }
                if (it.tags.contains("experiment") && it.tags.contains(variantB)) {
                    it.experiment = Experiment.VARIANT_B
                }
            }

            val measurements = get(outcome)
            if (measurements.isNotEmpty()) {
                ExperimentView().print(get(outcome), variants[0], variants[1])
            }
        }
    }


    fun get(builds: List<Build>): List<Measurement> {
        return builds.groupBy { it.OS }.flatMap {
            javaMeasurements(
                it.value.filter { it.experiment == Experiment.VARIANT_A }.dropLast(2),
                it.value.filter { it.experiment == Experiment.VARIANT_B }.dropLast(2),
                it.key
            )

        } + builds.groupBy { it.OS }.flatMap {
            kotlinProcessMeasurement(
                it.value.filter { it.experiment == Experiment.VARIANT_A }.first(),
                it.value.filter { it.experiment == Experiment.VARIANT_B }.first(),
                it.key
            )
        } + builds.groupBy { it.OS }.flatMap {
            kotlinBuildReportMeasurement(
                it.value.filter { it.experiment == Experiment.VARIANT_A }.dropLast(2),
                it.value.filter { it.experiment == Experiment.VARIANT_B }.dropLast(2),
                it.key
            )
        }
    }

    private fun kotlinProcessMeasurement(first: Build, first1: Build, key: OS): List<Measurement> {

        val measurement = mutableListOf<Measurement>()
        val variantAValues = processKotlinValues(first.values)
        val variantBValues = processKotlinValues(first1.values)
        if (variantAValues.size == variantBValues.size) {
            variantAValues.forEach {
                val variantB = variantBValues[it.key]!!
                measurement.add(
                    Measurement(
                        name = it.key,
                        variantA = it.value,
                        variantB = variantB,
                        category = "Last Kotlin process",
                        OS = OS.Linux
                    )
                )
            }
            return measurement.toList()
        } else {
            return emptyList()
        }
    }

    private fun processKotlinValues(values: Array<CustomValue>): Map<String, String> {
        return if (values.filter { it.name.contains("Kotlin-Process") }.isNotEmpty()) {
            val measurements = mutableMapOf<String, String>()
            values.filter { it.name.contains("Kotlin-Process") }.forEach {
                val name = it.name.split("-").filterIndexed { index, _ ->
                    index != 2 // you can also specify more interesting filters here...
                }.joinToString("-")
                measurements[name] = it.value
            }
            measurements
        } else {
            emptyMap()
        }
    }

    private fun javaMeasurements(
        variantABuilds: List<Build>,
        variantBBuilds: List<Build>,
        os: OS
    ): List<Measurement> {

        val taskTypes = variantABuilds[0].taskExecution.filter {
            (it.avoidanceOutcome == "executed_cacheable" || it.avoidanceOutcome == "executed_not_cacheable")
        }.distinctBy { it.taskType }
        val measurements = mutableListOf<Measurement>()
        taskTypes.forEach { task ->

            val sumVariantA = variantABuilds.sumOf {
                filterByExecutionAndType(it, task)
                    .sumOf { it.duration }
            } / variantABuilds.sumOf {
                filterByExecutionAndType(it, task)
                    .count()
            }
            var process = true

            if (sumVariantA < 100L) {
                process = false
            }

            if (process) {
                measurements.add(
                    Measurement(
                        category = "Tasks Compiler",
                        name = task.taskType,
                        variantA = variantABuilds.sumOf {
                            filterByExecutionAndType(it, task)
                                .sumOf { it.duration }
                        } /
                            variantABuilds.sumOf {
                                filterByExecutionAndType(it, task)
                                    .count()
                            },
                        variantB = variantBBuilds.sumOf {
                            filterByExecutionAndType(it, task)
                                .sumOf { it.duration }
                        } /
                            variantBBuilds.sumOf {
                                filterByExecutionAndType(it, task)
                                    .count()
                            },
                        OS = OS.Linux
                    )
                )
            }
        }
        measurements.sortBy { it.name }
        return measurements

    }

    private fun kotlinBuildReportMeasurement(
        variantABuilds: List<Build>,
        variantBBuilds: List<Build>, key: OS
    ): List<Measurement> {
        val measurements = mutableListOf<Measurement>()
        val valuesVariantA = extracted(variantABuilds)
        val valuesVariantB = extracted(variantBBuilds)

        val valuesByTaskAggregated = aggregateBuilds(valuesVariantA)
        val valuesByTaskAggregatedB = aggregateBuilds(valuesVariantB)


        valuesByTaskAggregated.forEach {
            val x = valuesByTaskAggregatedB[it.key]
            if (x != null && it.value.size == x.size) {
                if (it.value.filter {
                        it.contains("ms") || it.contains("GB") || it.contains("MB") || it.contains("KB") || it.contains(
                            "B"
                        )
                    }.isNotEmpty()) {

                    val valuesFormattedA = it.value.map { it.replace(",", "").replace("ms", "").split(" ")[0] }
                    val valuesFormattedB = x.map { it.replace(",", "").replace("ms", "").split(" ")[0] }
                    val qualifierA = if (it.value.first().contains("ms")) "ms" else it.value.first().split(" ")[1]
                    val qualifierB = if (x.first().contains("ms")) "ms" else x.first().split(" ")[1]


                    val varianta = valuesFormattedA.sumOf { it.toDouble() } / valuesFormattedA.size
                    val variantb = valuesFormattedB.sumOf { it.toDouble() } / valuesFormattedB.size
                    if (varianta != variantb) {
                        measurements.add(
                            Measurement(
                                name = it.key,
                                variantA = "$varianta $qualifierA",
                                variantB = "$variantb $qualifierB",
                                category = "Kotlin Build Reports",
                                OS = OS.Linux
                            )
                        )
                    }
                } else {
                    val valuesFormattedA = it.value.map { it.replace(",", "").split(" ")[0] }
                    val valuesFormattedB = x.map { it.replace(",", "").split(" ")[0] }
                    val varianta = valuesFormattedA.sumOf { it.toLong() } / valuesFormattedA.size
                    val variantb = valuesFormattedB.sumOf { it.toLong() } / valuesFormattedB.size
                    if (varianta != variantb) {
                        measurements.add(
                            Measurement(
                                name = it.key,
                                variantA = "$varianta",
                                variantB = "$variantb",
                                category = "Kotlin Build Reports",
                                OS = OS.Linux
                            )
                        )
                    }
                }

            }

        }
        return measurements


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
                            if ((values[auxCount + 1].contains("GB") || values[auxCount + 1].contains("MB") || values[auxCount + 1].contains(
                                    "KB"
                                ) || values[auxCount + 1].contains("B"))
                            ) {
                                value = values[auxCount + 1].split(",").dropLast(1).joinToString { "" }.replace("}", "")
                            } else {
                                value = values[auxCount + 1].split(",")[0].replace("}", "")
                            }

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


    private fun filterByExecutionAndType(
        it: Build,
        task: Task
    ) = it.taskExecution.filter {
        (it.avoidanceOutcome == "executed_cacheable" || it.avoidanceOutcome == "executed_not_cacheable")
            && it.taskType == task.taskType
    }

}


data class MetricKotlin(val desc: String, val value: String)
