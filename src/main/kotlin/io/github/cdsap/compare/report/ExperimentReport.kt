package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.output.ExperimentView
import io.github.cdsap.compare.report.measurements.FilterBuildsPerVariant
import io.github.cdsap.compare.report.measurements.MeasurementsByReport
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.repository.impl.GradleRepositoryImpl

class ExperimentReport(
    private val filter: Filter,
    private val repository: GradleRepositoryImpl,
    private val report: Report,
) {
    suspend fun process() {
        if (report.variants.isEmpty()) {
            throw IllegalArgumentException("At least one variant is required")
        } else {
            val buildWithResourceUsage = BuildsWithResourceUsageProvider(repository).request(filter, report.isProfile)
            val variants = checkVariantsData(FilterBuildsPerVariant(report).get(buildWithResourceUsage))
            val measurements = MeasurementsByReport(report).get(variants)
            if (measurements.isNotEmpty()) {
                ExperimentView(report).generateOutputs(measurements, variants)
            }
        }
    }

    private fun checkVariantsData(buildMap: Map<String, List<BuildWithResourceUsage>>): Map<String, List<BuildWithResourceUsage>> {
        val keysWithEmptyLists = buildMap.filterValues { it.isEmpty() }.keys

        if (keysWithEmptyLists.size == buildMap.size) {
            throw IllegalArgumentException("All variants have empty lists. Please check your input data.")
        }

        return buildMap.filterValues { it.isNotEmpty() }
    }
}
