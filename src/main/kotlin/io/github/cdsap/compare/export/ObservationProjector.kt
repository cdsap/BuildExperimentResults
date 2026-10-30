package io.github.cdsap.compare.export

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.output.chart.KotlinBuildReportsConstants
import io.github.cdsap.compare.report.measurements.ResourceUsageCoverage
import io.github.cdsap.compare.report.measurements.resourceUsageTotalOrNull
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.median
import java.time.Instant

class ObservationProjector {
    fun project(
        report: Report,
        buildsByVariantId: Map<String, List<BuildWithResourceUsage>>,
    ): DatasetsDto {
        if (!report.includeObservations) {
            return emptyDatasets()
        }
        return DatasetsDto(
            builds = projectBuilds(report.url, buildsByVariantId),
            taskSeries = projectTaskSeries(buildsByVariantId),
            resourceSeries = resourceSeries(report, buildsByVariantId),
            processSeries = if (report.processesReport) projectProcessSeries(buildsByVariantId) else emptyMap(),
            gcSeries = if (report.gcReport) projectGcSeries(buildsByVariantId) else emptyMap(),
            kotlinBuildReportSeries =
                if (report.kotlinBuildReport) {
                    projectKotlinBuildReportSeries(buildsByVariantId)
                } else {
                    emptyMap()
                },
        )
    }

    private fun emptyDatasets() =
        DatasetsDto(
            builds = emptyMap(),
            taskSeries = emptyMap(),
            resourceSeries = emptyMap(),
            processSeries = emptyMap(),
            gcSeries = emptyMap(),
            kotlinBuildReportSeries = emptyMap(),
        )

    private fun projectBuilds(
        develocityUrl: String?,
        buildsByVariantId: Map<String, List<BuildWithResourceUsage>>,
    ): Map<String, List<BuildObservationDto>> {
        val result = linkedMapOf<String, List<BuildObservationDto>>()
        buildsByVariantId.forEach { (variantId, builds) ->
            result[variantId] =
                builds.mapIndexed { index, build ->
                    BuildObservationDto(
                        observationId = observationId(build.id, index),
                        sequence = index + 1,
                        durationMs = build.buildDuration.toDouble(),
                        configurationMs = build.configuration.toDouble(),
                        startedAt = startedAt(build.buildStartTime),
                        scanUrl = scanUrl(develocityUrl, build.id),
                    )
                }
        }
        return result
    }

    private fun projectTaskSeries(
        buildsByVariantId: Map<String, List<BuildWithResourceUsage>>,
    ): Map<String, TaskSeriesDto> {
        val commonPaths = commonTaskPaths(buildsByVariantId)
        val result = linkedMapOf<String, TaskSeriesDto>()
        commonPaths.forEach { path ->
            val type =
                buildsByVariantId.values
                    .flatten()
                    .flatMap { it.taskExecution.toList() }
                    .firstOrNull { it.taskPath == path }
                    ?.taskType
            val observations = linkedMapOf<String, List<TaskObservationDto>>()
            buildsByVariantId.forEach { (variantId, builds) ->
                observations[variantId] =
                    builds.mapIndexed { index, build ->
                        val task = build.taskExecution.firstOrNull { it.taskPath == path }
                        TaskObservationDto(
                            observationId = observationId(build.id, index),
                            sequence = index + 1,
                            durationMs = task?.duration?.toDouble() ?: 0.0,
                            outcome = task?.avoidanceOutcome,
                        )
                    }
            }
            result["task.path.${MetricIdentity.slug(path)}"] =
                TaskSeriesDto(path = path, taskType = type, observations = observations)
        }
        return result
    }

    private fun resourceSeries(
        report: Report,
        buildsByVariantId: Map<String, List<BuildWithResourceUsage>>,
    ): Map<String, NamedSeriesDto> {
        if (!report.resourceUsageReport) return emptyMap()
        val coverage = ResourceUsageCoverage.of(buildsByVariantId)
        if (!coverage.comparisonEligible) return emptyMap()
        return projectResourceSeries(coverage.byVariant.mapValues { it.value.included })
    }

    private fun projectResourceSeries(
        buildsByVariantId: Map<String, List<BuildWithResourceUsage>>,
    ): Map<String, NamedSeriesDto> {
        return linkedMapOf(
            "resource.build-process-memory-max" to
                namedBytesSeries("Build process memory max", buildsByVariantId) {
                    resourceUsageTotalOrNull(it)!!.buildProcessMemory.max.toDouble()
                },
            "resource.build-child-process-memory-max" to
                namedBytesSeries("Build child processes memory max", buildsByVariantId) {
                    resourceUsageTotalOrNull(it)!!.buildChildProcessesMemory.max.toDouble()
                },
        )
    }

    private fun namedBytesSeries(
        name: String,
        buildsByVariantId: Map<String, List<BuildWithResourceUsage>>,
        selector: (BuildWithResourceUsage) -> Double,
    ): NamedSeriesDto =
        NamedSeriesDto(
            name = name,
            unit = "bytes",
            observations = numericObservations(buildsByVariantId, selector),
        )

    private fun projectProcessSeries(
        buildsByVariantId: Map<String, List<BuildWithResourceUsage>>,
    ): Map<String, NamedSeriesDto> {
        val names =
            buildsByVariantId.values
                .flatten()
                .flatMap { it.values.toList() }
                .map { it.name }
                .filter { PROCESS_VALUE.matches(it) }
                .distinct()
                .sorted()
        val result = linkedMapOf<String, NamedSeriesDto>()
        names.forEach { name ->
            result["process.${MetricIdentity.slug(name)}"] =
                NamedSeriesDto(
                    name = name,
                    unit = processUnit(name),
                    observations =
                        numericObservations(buildsByVariantId) { build ->
                            finiteNumber(build.values.firstOrNull { it.name == name }?.value)
                        },
                )
        }
        return result
    }

    private fun projectGcSeries(
        buildsByVariantId: Map<String, List<BuildWithResourceUsage>>,
    ): Map<String, NamedSeriesDto> {
        val names =
            buildsByVariantId.values
                .flatten()
                .flatMap { it.values.toList() }
                .map { it.name }
                .filter { it.contains("-total-collections") }
                .distinct()
                .sorted()
        val result = linkedMapOf<String, NamedSeriesDto>()
        names.forEach { name ->
            result["gc.${MetricIdentity.slug(name)}"] =
                NamedSeriesDto(
                    name = name,
                    unit = "count",
                    observations =
                        numericObservations(buildsByVariantId) { build ->
                            finiteNumber(build.values.firstOrNull { it.name == name }?.value)
                        },
                )
        }
        return result
    }

    private fun projectKotlinBuildReportSeries(
        buildsByVariantId: Map<String, List<BuildWithResourceUsage>>,
    ): Map<String, NamedSeriesDto> {
        val result = linkedMapOf<String, NamedSeriesDto>()
        KotlinBuildReportsConstants.ALLOWED_LIST.forEach { reportName ->
            result["kotlin.${MetricIdentity.slug(reportName)}"] =
                NamedSeriesDto(
                    name = reportName,
                    unit = "ms",
                    observations =
                        numericObservations(buildsByVariantId) { build ->
                            kotlinReportMedian(build, reportName)
                        },
                )
        }
        return result
    }

    private fun kotlinReportMedian(
        build: BuildWithResourceUsage,
        reportName: String,
    ): Double? {
        val values =
            build.values
                .filter { it.value.contains(reportName) }
                .mapNotNull { customValue ->
                    customValue.value
                        .substringAfter("Performance: [", missingDelimiterValue = "")
                        .substringBefore("]")
                        .split(",")
                        .find { it.contains("$reportName:") }
                        ?.substringAfter(":")
                        ?.replace("}", "")
                        ?.replace("ms", "")
                        ?.trim()
                        ?.toLongOrNull()
                        ?.toDouble()
                }
        return values.takeIf { it.isNotEmpty() }?.median()
    }

    private fun numericObservations(
        buildsByVariantId: Map<String, List<BuildWithResourceUsage>>,
        selector: (BuildWithResourceUsage) -> Double?,
    ): Map<String, List<NumericObservationDto>> {
        val observations = linkedMapOf<String, List<NumericObservationDto>>()
        buildsByVariantId.forEach { (variantId, builds) ->
            observations[variantId] =
                builds.mapIndexed { index, build ->
                    NumericObservationDto(
                        observationId = observationId(build.id, index),
                        sequence = index + 1,
                        value = selector(build)?.takeIf { it.isFinite() },
                    )
                }
        }
        return observations
    }

    private fun commonTaskPaths(buildsByVariantId: Map<String, List<BuildWithResourceUsage>>): List<String> {
        val perVariant =
            buildsByVariantId.values.map { builds ->
                builds.flatMap { it.taskExecution.map { task -> task.taskPath } }.toSet()
            }
        if (perVariant.isEmpty()) return emptyList()
        return perVariant.reduce { acc, set -> acc.intersect(set) }.sorted()
    }

    private fun observationId(
        buildId: String,
        index: Int,
    ): String = buildId.ifBlank { "observation-${index + 1}" }

    private fun startedAt(epochMillis: Long): String? =
        if (epochMillis > 0) Instant.ofEpochMilli(epochMillis).toString() else null

    private fun scanUrl(
        base: String?,
        buildId: String,
    ): String? {
        val url = base?.trim().orEmpty()
        if (buildId.isBlank()) return null
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        return "${url.trimEnd('/')}/s/$buildId"
    }

    private fun processUnit(name: String): String =
        when {
            name.endsWith("-gcTime") || name.endsWith("-uptime") -> "minutes"
            name.endsWith("-max") || name.endsWith("-usage") || name.endsWith("-capacity") -> "GB"
            else -> ""
        }

    private companion object {
        private val PROCESS_VALUE = Regex("^(Kotlin|Gradle)-Process-\\d+-.+")
    }
}
