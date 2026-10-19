package io.github.cdsap.compare.report.diagnostics

import io.github.cdsap.compare.model.SingleMeasurement

data class DiagnosticsMeasurements(
    val measurements: Map<String, List<SingleMeasurement>> = emptyMap(),
    val warnings: List<String> = emptyList(),
)

interface DiagnosticsMeasurementSource {
    fun load(
        diagnosticsPackagePath: String?,
        reportVariants: List<String>,
    ): DiagnosticsMeasurements
}
