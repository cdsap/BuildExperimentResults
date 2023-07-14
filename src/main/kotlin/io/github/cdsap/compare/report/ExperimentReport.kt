package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.domain.impl.GetBuildScansWithQueryImpl
import io.github.cdsap.geapi.client.model.*
import io.github.cdsap.geapi.client.repository.impl.GradleRepositoryImpl
import io.github.cdsap.geapi.domain.model.Task
import io.github.cdsap.compare.model.Measurement
import io.github.cdsap.compare.view.ExperimentView


class ExperimentReport(
    private val filter: Filter,
    private val repository: GradleRepositoryImpl
) {

    suspend fun process() {
        val getBuildScans = GetBuildScansWithQueryImpl(repository).get(filter)
        val buildsFiltered = mutableListOf<Build>()
        if (filter.variants == null) {
            throw IllegalArgumentException("Variants can not be null")
        } else {
            val variants = filter.variants!!.split(",")
            val variantA = variants[0].trim()
            val variantB = variants[1].trim()
            if (getBuildScans.isNotEmpty()) {
                println("Processing build scan cache performance")
                getBuildScans.map {
                    if (filter.experimentId != null) {
                        if (it.tags.contains(filter.experimentId) && it.tags.contains(variantB)) {
                            collectBuild(it, buildsFiltered, Experiment.VARIANT_B)
                        } else if (it.tags.contains(filter.experimentId) && it.tags.contains(variantA)) {
                            collectBuild(it, buildsFiltered, Experiment.VARIANT_A)
                        } else {

                        }
                    } else {
                        if (it.tags.contains("experiment") && it.tags.contains(variantB)) {
                            collectBuild(it, buildsFiltered, Experiment.VARIANT_B)
                        } else if (it.tags.contains("experiment") && it.tags.contains(variantA)) {
                            collectBuild(it, buildsFiltered, Experiment.VARIANT_A)
                        }
                    }
                }
            }
            ExperimentView().print(get(buildsFiltered))
        }
    }

    private suspend fun collectBuild(
        it: ScanWithAttributes,
        builds: MutableList<Build>,
        experiment: Experiment
    ) {
        var os = if (it.tags.contains("Mac OS X")) {
            OS.MAC
        } else if (it.tags.contains("Linux")) {
            OS.Linux
        } else {
            null
        }
        if (os != null) {
            val cachePerformance = repository.getBuildScanGradleCachePerformance(it.id)
            cachePerformance.experiment = experiment
            cachePerformance.id = it.id
            cachePerformance.requestedTask = it.requestedTasksGoals
            cachePerformance.tags = it.tags
            cachePerformance.buildDuration = it.buildDuration
            cachePerformance.OS = os
            cachePerformance.values = it.values
            builds.add(cachePerformance)
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
                        OS = key
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
                        OS = os
                    )
                )
            }
        }
        measurements.sortBy { it.name }
        return measurements

    }

    private fun filterByExecutionAndType(
        it: Build,
        task: Task
    ) = it.taskExecution.filter {
        (it.avoidanceOutcome == "executed_cacheable" || it.avoidanceOutcome == "executed_not_cacheable")
            && it.taskType == task.taskType
    }

}
