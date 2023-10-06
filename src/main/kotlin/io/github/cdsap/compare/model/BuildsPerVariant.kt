package io.github.cdsap.compare.model

import io.github.cdsap.geapi.client.model.Build

data class BuildsPerVariant(
    val variantA: List<Build>,
    val variantB: List<Build>
)
