package io.github.cdsap.compare.report.diagnostics

import java.io.File

class FileDiagnosticsMeasurementsSource(
    private val diagnosticsPackageLoader: DiagnosticsPackageLoader = DiagnosticsPackageLoader(),
) : DiagnosticsMeasurementsSource {
    override fun load(
        diagnosticsPackagePath: String?,
        reportVariants: List<String>,
    ): DiagnosticsMeasurements {
        if (diagnosticsPackagePath == null) return DiagnosticsMeasurements()

        val dto = diagnosticsPackageLoader.load(File(diagnosticsPackagePath))
        val (measurements, warnings) = diagnosticsPackageLoader.toMeasurements(dto, reportVariants)
        return DiagnosticsMeasurements(measurements, warnings)
    }
}
