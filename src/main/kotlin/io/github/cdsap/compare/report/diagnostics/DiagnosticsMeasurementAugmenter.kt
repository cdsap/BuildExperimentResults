package io.github.cdsap.compare.report.diagnostics

import io.github.cdsap.compare.model.SingleMeasurement

class DiagnosticsMeasurementAugmenter(
    private val diagnosticsMeasurementsSource: DiagnosticsPackageSource = DiagnosticsPackageLoader(),
) {

    fun augment(
        measurements: Map<String, List<SingleMeasurement>>,
        diagnosticsPackagePath: String?,
        reportVariants: List<String>,
    ): Map<String, List<SingleMeasurement>> =
        augmentWithDiagnostics(measurements, diagnosticsPackagePath, reportVariants).measurements

    internal fun augmentWithDiagnostics(
        measurements: Map<String, List<SingleMeasurement>>,
        diagnosticsPackagePath: String?,
        reportVariants: List<String>,
    ): DiagnosticsAugmentation {
        val diagnostics = diagnosticsMeasurementsSource.load(diagnosticsPackagePath, reportVariants)
        diagnostics.warnings.forEach { println("diagnostics warning: $it") }
        if (diagnostics.measurements.isEmpty()) return DiagnosticsAugmentation(measurements, diagnostics)

        val merged = measurements.toMutableMap()
        for ((variant, rows) in diagnostics.measurements) {
            merged[variant] = merged[variant].orEmpty() + rows
        }
        return DiagnosticsAugmentation(merged, diagnostics)
    }
}

internal data class DiagnosticsAugmentation(
    val measurements: Map<String, List<SingleMeasurement>>,
    val diagnostics: DiagnosticsMeasurements,
)
