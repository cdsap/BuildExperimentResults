package io.github.cdsap.compare.model

data class SingleMeasurement(
    val category: String,
    val name: String,
    val variantMean: Any,
    val variantP50: Any,
    val variantP90: Any,
    val qualifier: String,
    val metric: Metric
)
