package io.github.cdsap.compare.model

import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

data class BuildsPerVariants(
    val variants: Map<String, List<BuildWithResourceUsage>>,
)
