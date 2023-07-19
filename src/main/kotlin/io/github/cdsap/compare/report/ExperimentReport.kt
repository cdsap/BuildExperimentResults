package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.domain.impl.GetBuildScansWithQueryImpl
import io.github.cdsap.geapi.client.model.*
import io.github.cdsap.geapi.client.repository.impl.GradleRepositoryImpl
import io.github.cdsap.geapi.domain.model.Task
import io.github.cdsap.compare.model.Measurement
import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.view.ExperimentView
import io.github.cdsap.geapi.client.domain.impl.GetCachePerformanceImpl
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToInt
import kotlin.math.roundToLong


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

            val buildsVariantA = outcome.filter { it.experiment == Experiment.VARIANT_A }.dropLast(2).size
            val buildsVariantB = outcome.filter { it.experiment == Experiment.VARIANT_B }.dropLast(2).size
            val measurements = get(outcome)
            if (measurements.isNotEmpty()) {
                ExperimentView().print(
                    measurements, variants[0], variants[1], Header(
                        task = filter.requestedTask.toString(),
                        numberOfBuildsForExperimentA = buildsVariantA,
                        numberOfBuildsForExperimentB = buildsVariantB,
                        experiment = filter.experimentId!!
                    )
                )
            }
        }
    }


    fun get(builds: List<Build>): List<MeasurementWithPercentiles> {
        return builds.groupBy { it.OS }.flatMap {
            javaMeasurements(
                it.value.filter { it.experiment == Experiment.VARIANT_A }.dropLast(2),
                it.value.filter { it.experiment == Experiment.VARIANT_B }.dropLast(2),
                it.key
            ) +
                builds.groupBy { it.OS }.flatMap {
                    kotlinBuildReportMeasurement(
                        it.value.filter { it.experiment == Experiment.VARIANT_A }.dropLast(2),
                        it.value.filter { it.experiment == Experiment.VARIANT_B }.dropLast(2),
                        it.key
                    )
                }
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
                        category = "Last Kotlin process state",
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
    ): List<MeasurementWithPercentiles> {

        val taskTypes = variantABuilds[0].taskExecution.filter {
            (it.avoidanceOutcome == "executed_cacheable")
        }.distinctBy { it.taskType }

        val variantAAggregatedTaskType = mutableMapOf<String, MutableList<Long>>()
        val variantBAggregatedTaskType = mutableMapOf<String, MutableList<Long>>()
        val variantAAggregatedTaskPath = mutableMapOf<String, MutableList<Long>>()
        val variantBAggregatedTaskPath = mutableMapOf<String, MutableList<Long>>()
        variantABuilds.forEach {
            it.taskExecution.filter { (it.avoidanceOutcome == "executed_cacheable") }
                .forEach {
                    if (variantAAggregatedTaskType.contains(it.taskType)) {
                        variantAAggregatedTaskType[it.taskType]?.add(it.duration)
                    } else {
                        variantAAggregatedTaskType[it.taskType] = mutableListOf()
                        variantAAggregatedTaskType[it.taskType]?.add(it.duration)
                    }
                }
        }
        variantBBuilds.forEach {
            it.taskExecution.filter { (it.avoidanceOutcome == "executed_cacheable") }
                .forEach {
                    if (variantBAggregatedTaskType.contains(it.taskType)) {
                        variantBAggregatedTaskType[it.taskType]?.add(it.duration)
                    } else {
                        variantBAggregatedTaskType[it.taskType] = mutableListOf()
                        variantBAggregatedTaskType[it.taskType]?.add(it.duration)
                    }
                }
        }
        variantABuilds.forEach {
            it.taskExecution.filter { (it.avoidanceOutcome == "executed_cacheable") }
                .forEach {
                    if (variantAAggregatedTaskPath.contains(it.taskPath)) {
                        variantAAggregatedTaskPath[it.taskPath]?.add(it.duration)
                    } else {
                        variantAAggregatedTaskPath[it.taskPath] = mutableListOf()
                        variantAAggregatedTaskPath[it.taskPath]?.add(it.duration)
                    }
                }
        }
        variantBBuilds.forEach {
            it.taskExecution.filter { (it.avoidanceOutcome == "executed_cacheable") }
                .forEach {
                    if (variantBAggregatedTaskPath.contains(it.taskPath)) {
                        variantBAggregatedTaskPath[it.taskPath]?.add(it.duration)
                    } else {
                        variantBAggregatedTaskPath[it.taskPath] = mutableListOf()
                        variantBAggregatedTaskPath[it.taskPath]?.add(it.duration)
                    }
                }
        }
        val measurements = mutableListOf<Measurement>()
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

        variantAAggregatedTaskPath.forEach {
            val x = variantBAggregatedTaskPath[it.key]
            if (x != null) {
//                measurementsP.add(
//                    MeasurementWithPercentiles(
//                        category = " Task Path",
//                        name = it.key,
//                        variantAMean = it.value.sumOf { it.toLong() } / it.value.size,
//                        variantBMean = x.sumOf { it.toLong() } / x.size,
//                        variantAP50 = it.value.percentile(50.0),
//                        variantBP50 = x.percentile(50.0),
//                        variantAP90 = it.value.percentile(90.0),
//                        variantBP90 = x.percentile(90.0),
//                        OS = OS.Linux
//                    )
//                )
            }

        }


        return measurementsP

    }

    private fun kotlinBuildReportMeasurement(
        variantABuilds: List<Build>,
        variantBBuilds: List<Build>, key: OS
    ): List<MeasurementWithPercentiles> {
        val measurements = mutableListOf<Measurement>()
        val measurementsP = mutableListOf<MeasurementWithPercentiles>()
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
                    }
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

                    //if (varianta != variantb) {
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

                    // }
                }

            }

        }
        return measurementsP


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
                                value =
                                    values[auxCount + 1].split(",").dropLast(1).joinToString { "" }.replace("}", "")
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

data class Header(
    val task: String,
    val numberOfBuildsForExperimentA: Int,
    val numberOfBuildsForExperimentB: Int,
    val experiment: String
)
