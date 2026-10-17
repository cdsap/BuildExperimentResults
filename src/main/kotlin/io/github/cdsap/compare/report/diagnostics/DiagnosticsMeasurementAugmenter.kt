package io.github.cdsap.compare.report.diagnostics

import io.github.cdsap.compare.model.SingleMeasurement
import java.io.File

class DiagnosticsMeasurementAugmenter(
    private val diagnosticsPackageLoader: DiagnosticsPackageLoader = DiagnosticsPackageLoader(),
) {
    fun augment(
        measurements: Map<String, List<SingleMeasurement>>,
        diagnosticsPackagePath: String?,
        reportVariants: List<String>,
    ): Map<String, List<SingleMeasurement>> {
        val path = diagnosticsPackagePath ?: return measurements
        val dto = diagnosticsPackageLoader.load(File(path))
        val (diagnostics, warnings) = diagnosticsPackageLoader.toMeasurements(dto, reportVariants)
        warnings.forEach { println("diagnostics warning: $it") }
        if (diagnostics.isEmpty()) return measurements

        val merged = measurements.toMutableMap()
        for ((variant, rows) in diagnostics) {
            merged[variant] = merged[variant].orEmpty() + rows
        }
        return merged
    }
}
