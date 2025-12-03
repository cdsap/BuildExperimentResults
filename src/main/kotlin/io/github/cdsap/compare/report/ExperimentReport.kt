package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.output.ExperimentView
import io.github.cdsap.compare.report.measurements.FilterBuildsPerVariant
import io.github.cdsap.compare.report.measurements.MeasurementsByReport
import io.github.cdsap.geapi.client.domain.impl.GetBuildsFromQueryWithAttributesRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsProfileRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsResourceUsageRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsWithCachePerformanceRequest
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
            val buildWithResourceUsage = requestBuilds()
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

    private suspend fun requestBuilds(): List<BuildWithResourceUsage> {
        val getBuildScans = GetBuildsFromQueryWithAttributesRequest(repository).get(filter)
        val getOutcome = GetBuildsWithCachePerformanceRequest(repository)
        val outcome = getOutcome.get(getBuildScans, filter)
        val buildWithResourceUsage = GetBuildsResourceUsageRequest(repository).get(getBuildScans, filter)
        val buildProfile = GetBuildsProfileRequest(repository).get(getBuildScans, filter)
        outcome.forEach { build ->
            val usage = buildWithResourceUsage.find { build.id == it.id }
            usage?.requestedTask = build.requestedTask
            usage?.values = build.values
            usage?.tags = build.tags
            usage?.buildDuration = build.buildDuration
            usage?.buildStartTime = build.buildStartTime
            usage?.builtTool = build.builtTool
            usage?.projectName = build.projectName
            usage?.taskExecution = build.taskExecution
            val profile = buildProfile.find { build.id == it.id }
            if (profile != null && usage != null) {
                usage.configuration = profile.breakdown.configuration
                usage.totalGarbageCollectionTime = profile.memoryUsage.totalGarbageCollectionTime
            }
        }
        if (report.isProfile) {
            return buildWithResourceUsage.filterNot { it.requestedTask.size == 1 && it.requestedTask.first() == "clean" }
        } else {
            return buildWithResourceUsage
        }
    }
}
