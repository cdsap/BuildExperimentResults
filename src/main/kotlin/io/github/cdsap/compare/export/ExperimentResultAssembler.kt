package io.github.cdsap.compare.export

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.output.removeExperimentId
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurements
import io.github.cdsap.compare.report.measurements.ResourceUsageCoverage
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import java.time.Clock
import java.time.temporal.ChronoUnit

class ExperimentResultAssembler(
    private val clock: Clock = Clock.systemUTC(),
    private val producerRevision: String = System.getenv("GITHUB_SHA") ?: "unknown",
    private val observationProjector: ObservationProjector = ObservationProjector(),
) {
    fun assemble(
        report: Report,
        measurements: Map<String, List<SingleMeasurement>>,
        buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
        diagnostics: DiagnosticsMeasurements,
    ): ExperimentResultDto {
        val variants = variantDtos(report, buildsByVariant)
        val sourceTagToId = variants.associate { it.sourceTag to it.id }
        val buildsByVariantId =
            variants.associate { variant ->
                variant.id to buildsByVariant[variant.sourceTag].orEmpty()
            }
        val usedMetricIds = mutableSetOf<String>()
        val metrics = metricDtos(variants, measurements, sourceTagToId, usedMetricIds)
        val warnings = warnings(variants, diagnostics, sourceTagToId)
        val diagnosticsDto = diagnosticsDto(diagnostics, metrics, sourceTagToId)
        val observedIds = variants.filter { it.buildCount > 0 }.map { it.id }
        val status = if (observedIds.size == variants.size) "complete" else "partial"
        val requestedTasks =
            buildsByVariant.values
                .flatten()
                .firstOrNull()
                ?.requestedTask
                ?.toList()
                .orEmpty()
        val kind = MetricIdentity.sanitizeId(report.reportKind, "report")
        return ExperimentResultDto(
            schemaVersion = EXPERIMENT_RESULT_SCHEMA_VERSION,
            generatedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS).toString(),
            producer =
                ProducerDto(
                    name = PRODUCER_NAME,
                    version = PRODUCER_VERSION,
                    revision = producerRevision,
                ),
            report =
                ReportIdentityDto(
                    id = reportId(report, kind),
                    kind = kind,
                    title = report.reportTitle,
                    status = status,
                ),
            experiment =
                ExperimentIdentityDto(
                    id = report.experimentId?.ifBlank { null } ?: "unknown",
                    repository = report.repository,
                    requestedTasks = requestedTasks,
                    mode = experimentMode(report, kind),
                    isProfiler = report.isProfile,
                ),
            execution = executionDto(report),
            variants = variants,
            metrics = metrics,
            datasets = observationProjector.project(report, buildsByVariantId.filterValues { it.isNotEmpty() }),
            diagnostics = diagnosticsDto,
            warnings = warnings,
            completeness =
                CompletenessDto(
                    requestedVariantIds = variants.map { it.id },
                    observedVariantIds = observedIds,
                    missingMetricIds = emptyList(),
                    diagnostics = diagnosticsDto.status,
                ),
        )
    }

    private fun variantDtos(
        report: Report,
        buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
    ): List<VariantDto> {
        val used = mutableSetOf<String>()
        val coverage = if (report.resourceUsageReport) ResourceUsageCoverage.of(buildsByVariant) else null
        return report.variants.mapIndexed { index, sourceTag ->
            val role =
                when (index) {
                    0 -> "baseline"
                    1 -> "candidate"
                    else -> "variant"
                }
            val label = sourceTag.removeExperimentId(report.experimentId)
            val id = uniqueVariantId(sourceTag, label, index, used)
            val variantCoverage = coverage?.byVariant?.get(sourceTag)
            VariantDto(
                id = id,
                label = label,
                sourceTag = sourceTag,
                role = role,
                buildCount = buildsByVariant[sourceTag]?.size ?: 0,
                resourceUsageIncluded = variantCoverage?.includedCount,
                resourceUsageOmitted = variantCoverage?.omitted,
            )
        }
    }

    private fun uniqueVariantId(
        sourceTag: String,
        label: String,
        index: Int,
        used: MutableSet<String>,
    ): String {
        val fromPattern =
            Regex("variant[ab]_(.+)$").find(sourceTag)?.groupValues?.get(1)
                ?: label
        var candidate = MetricIdentity.sanitizeId(fromPattern, "variant-${index + 1}")
        if (candidate in used) {
            var suffix = 2
            while ("$candidate-$suffix" in used) suffix++
            candidate = "$candidate-$suffix"
        }
        used += candidate
        return candidate
    }

    private fun metricDtos(
        variants: List<VariantDto>,
        measurements: Map<String, List<SingleMeasurement>>,
        sourceTagToId: Map<String, String>,
        usedMetricIds: MutableSet<String>,
    ): List<MetricDto> {
        val keys =
            measurements.values
                .flatten()
                .map { MetricKey(it.metric, it.category, it.name, it.qualifier) }
                .distinct()
                .sortedWith(compareBy({ it.metric.name }, { it.category }, { it.name }, { it.qualifier }))
        return keys.map { key ->
            val sample = measurements.values.flatten().first { it.matches(key) }
            val family = MetricIdentity.family(sample.metric)
            val unit = sample.qualifier
            val valueType = MetricIdentity.valueType(unit, sample.metric)
            val values = linkedMapOf<String, StatisticDto>()
            variants.forEach { variant ->
                val row =
                    measurements[variant.sourceTag].orEmpty().firstOrNull { it.matches(key) }
                values[variant.id] =
                    StatisticDto(
                        mean = finiteNumber(row?.variantMean),
                        p50 = finiteNumber(row?.variantP50),
                        p90 = finiteNumber(row?.variantP90),
                        count = countFor(sample.metric, variant),
                    )
            }
            MetricDto(
                id = MetricIdentity.id(sample, usedMetricIds),
                family = family,
                category = sample.category,
                name = sample.name,
                unit = unit,
                valueType = valueType,
                direction = MetricIdentity.direction(valueType),
                scope = scope(sample),
                aggregation =
                    AggregationDto(
                        sampleUnit = MetricIdentity.sampleUnit(sample.metric),
                        method = "existing-ber-method",
                    ),
                values = values,
            )
        }
    }

    private fun countFor(
        metric: Metric,
        variant: VariantDto,
    ): Int? =
        when (metric) {
            Metric.CPU_DIAGNOSTICS, Metric.JFR -> null
            Metric.RESOURCE_USAGE -> variant.resourceUsageIncluded?.takeIf { it > 0 }
            else -> variant.buildCount.takeIf { it > 0 }
        }

    private fun scope(measurement: SingleMeasurement): Map<String, String>? =
        when (measurement.metric) {
            Metric.TASK_PATH -> mapOf("taskPath" to measurement.name)
            Metric.TASK_TYPE -> mapOf("taskType" to measurement.name)
            else -> measurement.qualifier.takeIf { it.isNotBlank() }?.let { mapOf("qualifier" to it) }
        }

    private fun warnings(
        variants: List<VariantDto>,
        diagnostics: DiagnosticsMeasurements,
        sourceTagToId: Map<String, String>,
    ): List<WarningDto> {
        val warnings = mutableListOf<WarningDto>()
        if (diagnostics.status == "not-requested") {
            warnings +=
                WarningDto(
                    code = "DIAGNOSTICS_NOT_REQUESTED",
                    severity = "info",
                    source = "ber",
                    message = "Diagnostics package was not requested",
                )
        }
        variants.filter { it.buildCount == 0 }.forEach { variant ->
            warnings +=
                WarningDto(
                    code = "VARIANT_HAS_NO_BUILDS",
                    severity = "warning",
                    source = "ber",
                    message = "Variant '${variant.sourceTag}' has no builds",
                    variantId = variant.id,
                )
        }
        if (variants.any { it.buildCount == 0 }) {
            warnings +=
                WarningDto(
                    code = "PARTIAL_EXPERIMENT",
                    severity = "warning",
                    source = "ber",
                    message = "Not all requested variants produced builds",
                )
        }
        diagnostics.warnings.forEach { message ->
            warnings +=
                WarningDto(
                    code = warningCode(message),
                    severity = "warning",
                    source = "diagnostics",
                    message = sanitizeWarning(message),
                    variantId = variantIdFromWarning(message, sourceTagToId),
                )
        }
        return warnings
    }

    private fun warningCode(message: String): String =
        when {
            message.contains("not found", ignoreCase = true) -> "DIAGNOSTICS_PACKAGE_MISSING"
            message.contains("Unable to parse", ignoreCase = true) -> "DIAGNOSTICS_PACKAGE_MISSING"
            message.contains("no unique match") -> "DIAGNOSTICS_VARIANT_UNMATCHED"
            message.contains("unknown metric") -> "METRIC_UNAVAILABLE"
            else -> "DIAGNOSTICS_SOURCE"
        }

    private fun variantIdFromWarning(
        message: String,
        sourceTagToId: Map<String, String>,
    ): String? = sourceTagToId.entries.firstOrNull { message.contains(it.key) }?.value

    private fun sanitizeWarning(message: String): String =
        message.replace(Regex("""(/[\w.+~-]+)+""")) { match ->
            match.value.substringAfterLast('/')
        }

    private fun diagnosticsDto(
        diagnostics: DiagnosticsMeasurements,
        metrics: List<MetricDto>,
        sourceTagToId: Map<String, String>,
    ): DiagnosticsDto {
        val jobKeys =
            diagnostics.jobKeysByVariant
                .mapNotNull { (sourceTag, keys) -> sourceTagToId[sourceTag]?.let { it to keys } }
                .toMap()
        val groups = mutableListOf<DiagnosticsGroupDto>()

        fun addGroup(
            id: String,
            family: String,
            metricFamily: String,
        ) {
            val metricIds = metrics.filter { it.family == metricFamily }.map { it.id }
            if (metricIds.isEmpty() && jobKeys.isEmpty()) return
            if (metricIds.isEmpty()) return
            groups +=
                DiagnosticsGroupDto(
                    id = id,
                    family = family,
                    sampleUnit = "diagnostic-sample",
                    metricIds = metricIds,
                    jobKeysByVariant = jobKeys,
                )
        }
        addGroup("cpu", "cpu-diagnostics", "cpu-diagnostics")
        addGroup("jfr", "jfr", "jfr")
        val source =
            if (diagnostics.status == "not-requested") {
                null
            } else {
                DiagnosticsSourceDto(
                    tool = "ExperimentDiagnosticsReport",
                    schemaVersion = diagnostics.sourceSchemaVersion,
                    experimentRunId = diagnostics.sourceExperimentRunId,
                    sourceRepo = diagnostics.sourceRepo,
                )
            }
        return DiagnosticsDto(status = diagnostics.status, source = source, groups = groups)
    }

    private fun reportId(
        report: Report,
        kind: String,
    ): String {
        val runId = report.experimentRunId?.let { MetricIdentity.sanitizeId(it, it) }
        val attempt = report.runAttempt ?: 1
        return when {
            !runId.isNullOrBlank() -> "run-$runId-$kind-attempt-$attempt"
            !report.experimentId.isNullOrBlank() ->
                "experiment-${MetricIdentity.sanitizeId(report.experimentId, "unknown")}-$kind"
            else -> "local-$kind"
        }
    }

    private fun experimentMode(
        report: Report,
        kind: String,
    ): String? =
        when {
            report.isProfile -> "profiler"
            kind == "report" -> null
            else -> kind
        }

    private fun executionDto(report: Report): ExecutionDto {
        val links = linkedMapOf<String, String>()
        val repository = report.repository
        val runId = report.experimentRunId
        if (!repository.isNullOrBlank() && !runId.isNullOrBlank() && repository.contains('/')) {
            links["workflow"] = "https://github.com/$repository/actions/runs/$runId"
        }
        return ExecutionDto(
            source = if (runId.isNullOrBlank()) "local" else "github-actions",
            runId = runId,
            runAttempt = report.runAttempt,
            workflowName = null,
            startedAt = null,
            links = links,
        )
    }

    private data class MetricKey(
        val metric: Metric,
        val category: String,
        val name: String,
        val qualifier: String,
    )

    private fun SingleMeasurement.matches(key: MetricKey): Boolean =
        metric == key.metric && category == key.category && name == key.name && qualifier == key.qualifier
}
