package io.github.cdsap.compare.report.diagnostics

import io.github.cdsap.compare.model.SingleMeasurement

class DiagnosticsMeasurementAugmenter(
    private val diagnosticsMeasurementsSource: DiagnosticsMeasurementSource = DiagnosticsPackageLoader(),
) {

    fun augment(
        measurements: Map<String, List<SingleMeasurement>>,
        diagnosticsPackagePath: String?,
        reportVariants: List<String>,
    ): Map<String, List<SingleMeasurement>> {
        val diagnostics = diagnosticsMeasurementsSource.load(diagnosticsPackagePath, reportVariants)
        diagnostics.warnings.forEach { println("diagnostics warning: $it") }
        if (diagnostics.measurements.isEmpty()) return measurements

        val merged = measurements.toMutableMap()
        for ((variant, rows) in diagnostics.measurements) {
            merged[variant] = merged[variant].orEmpty() + rows
        }
        return merged
    }
}
