package io.github.cdsap.compare.export

import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurements

internal class DiagnosticsExportMapper {
    fun map(
        diagnostics: DiagnosticsMeasurements,
        metrics: List<MetricDto>,
        sourceTagToVariantId: Map<String, String>,
    ): DiagnosticsExport {
        val warnings = diagnosticWarnings(diagnostics, sourceTagToVariantId)
        val diagnosticsDto = diagnosticsDto(diagnostics, metrics, sourceTagToVariantId)
        return DiagnosticsExport(diagnosticsDto, warnings)
    }

    private fun diagnosticWarnings(
        diagnostics: DiagnosticsMeasurements,
        sourceTagToVariantId: Map<String, String>,
    ): List<WarningDto> =
        buildList {
            if (diagnostics.status == "not-requested") {
                add(
                    WarningDto(
                        code = "DIAGNOSTICS_NOT_REQUESTED",
                        severity = "info",
                        source = "ber",
                        message = "Diagnostics package was not requested",
                    ),
                )
            }
            diagnostics.warnings.forEach { message ->
                add(
                    WarningDto(
                        code = warningCode(message),
                        severity = "warning",
                        source = "diagnostics",
                        message = sanitizeWarning(message),
                        variantId = variantIdFromWarning(message, sourceTagToVariantId),
                    ),
                )
            }
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
        sourceTagToVariantId: Map<String, String>,
    ): String? = sourceTagToVariantId.entries.firstOrNull { message.contains(it.key) }?.value

    private fun sanitizeWarning(message: String): String =
        message.replace(Regex("""(/[\w.+~-]+)+""")) { match ->
            match.value.substringAfterLast('/')
        }

    private fun diagnosticsDto(
        diagnostics: DiagnosticsMeasurements,
        metrics: List<MetricDto>,
        sourceTagToVariantId: Map<String, String>,
    ): DiagnosticsDto {
        val jobKeysByVariant =
            diagnostics.jobKeysByVariant
                .mapNotNull { (sourceTag, keys) -> sourceTagToVariantId[sourceTag]?.let { it to keys } }
                .toMap()
        val groups = mutableListOf<DiagnosticsGroupDto>()
        fun addGroup(
            id: String,
            family: String,
            metricFamily: String,
        ) {
            val metricIds = metrics.filter { it.family == metricFamily }.map { it.id }
            if (metricIds.isEmpty() && jobKeysByVariant.isEmpty()) return
            if (metricIds.isEmpty()) return
            groups +=
                DiagnosticsGroupDto(
                    id = id,
                    family = family,
                    sampleUnit = "diagnostic-sample",
                    metricIds = metricIds,
                    jobKeysByVariant = jobKeysByVariant,
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
}

internal data class DiagnosticsExport(
    val diagnostics: DiagnosticsDto,
    val warnings: List<WarningDto>,
)
