package io.github.cdsap.compare.report.diagnostics

data class DiagnosticsPackageDto(
    val schemaVersion: String = "",
    val experimentRunId: String? = null,
    val sourceRepo: String? = null,
    val variants: List<VariantDiagnosticsDto> = emptyList(),
    val warnings: List<String> = emptyList(),
)

data class VariantDiagnosticsDto(
    val variant: String = "",
    val jobKeys: List<String> = emptyList(),
    val measurements: List<PackageMeasurementDto> = emptyList(),
)

data class PackageMeasurementDto(
    val category: String = "",
    val name: String = "",
    val mean: Double = 0.0,
    val p50: Double = 0.0,
    val p90: Double = 0.0,
    val qualifier: String = "",
    val metric: String = "",
)
