package io.github.cdsap.compare.report.measurements

import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class FilterBuildsPerVariant(
    private val variants: List<String>,
    private val isProfile: Boolean,
    private val warmupsToDiscard: Int,
) {
    fun get(builds: List<BuildWithResourceUsage>): Map<String, List<BuildWithResourceUsage>> {
        val buildsPerVariants = mutableMapOf<String, List<BuildWithResourceUsage>>()
        variants.forEach { variant ->
            val buildsVariant = buildsByVariant(builds, variant.trim())
            buildsPerVariants[variant] = buildsVariant
        }

        return buildsPerVariants
    }

    private fun buildsByVariant(
        outcome: List<BuildWithResourceUsage>,
        variant: String,
    ): List<BuildWithResourceUsage> {
        val variants =
            outcome.filter {
                it.tags.contains(variant)
            }
        return if (isProfile) {
            variants.dropLast(warmupsToDiscard)
        } else {
            variants
        }
    }
}
