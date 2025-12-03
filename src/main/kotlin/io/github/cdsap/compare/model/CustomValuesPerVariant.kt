package io.github.cdsap.compare.model

data class CustomValuesPerVariant(
    val variant: MutableMap<String, Map<String, MutableList<MetricKotlin>>>,
)
