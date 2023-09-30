package io.github.cdsap.compare.model

data class CustomValuesPerVariant(
    val variantA: MutableMap<String, Map<String, MutableList<MetricKotlin>>>,
    val variantB: MutableMap<String, Map<String, MutableList<MetricKotlin>>>,
)

