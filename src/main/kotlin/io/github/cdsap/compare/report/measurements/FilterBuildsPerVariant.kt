package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class FilterBuildsPerVariant(val report: Report) {

    fun get(builds: List<BuildWithResourceUsage>): Map<String, List<BuildWithResourceUsage>> {
        val buildsPerVariants = mutableMapOf<String, List<BuildWithResourceUsage>>()
        report.variants.forEach { variant ->
            val buildsVariant = buildsByVariant(builds, variant.trim())
            buildsPerVariants[variant] = buildsVariant
        }

        return buildsPerVariants
    }

    private fun buildsByVariant(
        outcome: List<BuildWithResourceUsage>,
        variant: String
    ): List<BuildWithResourceUsage> {
        val variants = outcome.filter {
            it.tags.contains(variant)
        }
        return if (report.isProfile) {
            variants.dropLast(report.warmupsToDiscard)
        } else {
            variants
        }
    }
}
