package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.BuildsPerVariant
import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class FilterBuildsPerVariant(val report: Report) {

    fun get(builds: List<BuildWithResourceUsage>): BuildsPerVariant {
        val variantA = "${report.experimentId}_variant_experiment_${report.variants[0].trim()}"
        val variantB = "${report.experimentId}_variant_experiment_${report.variants[1].trim()}"

        val buildsVariantA = buildsByVariant(builds, variantA)
        val buildsVariantB = buildsByVariant(builds, variantB)

        return BuildsPerVariant(buildsVariantA, buildsVariantB)
    }

    private fun buildsByVariant(
        outcome: List<BuildWithResourceUsage>,
        variant: String
    ): List<BuildWithResourceUsage> {
        val variants = outcome.filter {
            it.tags.contains("experiment") && it.tags.contains(report.experimentId) && it.tags.contains(variant)
        }
        return if (report.isProfile) {
            variants.dropLast(report.warmupsToDiscard)
        } else {
            variants
        }
    }
}
