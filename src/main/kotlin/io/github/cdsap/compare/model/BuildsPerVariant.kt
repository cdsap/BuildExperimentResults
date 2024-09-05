package io.github.cdsap.compare.model

import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

data class BuildsPerVariant(
    val variantA: List<BuildWithResourceUsage>,
    val variantB: List<BuildWithResourceUsage>
)
