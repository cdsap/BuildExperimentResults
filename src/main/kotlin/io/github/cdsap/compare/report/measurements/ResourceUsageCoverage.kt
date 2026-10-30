package io.github.cdsap.compare.report.measurements

import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.PerformanceMetrics

data class ResourceUsageVariantCoverage(
    val included: List<BuildWithResourceUsage>,
    val omitted: Int,
) {
    val includedCount: Int get() = included.size
}

data class ResourceUsageCoverage(
    val byVariant: Map<String, ResourceUsageVariantCoverage>,
) {
    val comparisonEligible: Boolean
        get() = byVariant.isNotEmpty() && byVariant.values.all { it.includedCount >= 1 }

    companion object {
        fun of(buildsByVariant: Map<String, List<BuildWithResourceUsage>>): ResourceUsageCoverage =
            ResourceUsageCoverage(
                buildsByVariant.mapValues { (_, builds) ->
                    val included = builds.filter { resourceUsageTotalOrNull(it) != null }
                    ResourceUsageVariantCoverage(
                        included = included,
                        omitted = builds.size - included.size,
                    )
                },
            )
    }
}

internal fun resourceUsageTotalOrNull(build: BuildWithResourceUsage): PerformanceMetrics? =
    ResourceUsageTotals.orNull(build)
