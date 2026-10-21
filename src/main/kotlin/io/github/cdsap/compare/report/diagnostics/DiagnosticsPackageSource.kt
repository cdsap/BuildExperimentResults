package io.github.cdsap.compare.report.diagnostics

import io.github.cdsap.compare.model.SingleMeasurement

data class DiagnosticsMeasurements(
    val measurements: Map<String, List<SingleMeasurement>> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val status: String = "not-requested",
    val sourceSchemaVersion: String? = null,
    val sourceExperimentRunId: String? = null,
    val sourceRepo: String? = null,
    val jobKeysByVariant: Map<String, List<String>> = emptyMap(),
)

interface DiagnosticsPackageSource {
    fun load(
        diagnosticsPackagePath: String?,
        reportVariants: List<String>,
    ): DiagnosticsMeasurements
}
