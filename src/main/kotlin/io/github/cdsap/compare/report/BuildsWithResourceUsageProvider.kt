package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.domain.impl.GetBuildsFromQueryWithAttributesRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsProfileRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsResourceUsageRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsWithCachePerformanceRequest
import io.github.cdsap.geapi.client.model.Build
import io.github.cdsap.geapi.client.model.BuildProfileOverview
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.repository.impl.GradleRepositoryImpl

class BuildsWithResourceUsageProvider(
    private val repository: GradleRepositoryImpl,
) {
    suspend fun request(
        filter: Filter,
        isProfile: Boolean,
    ): List<BuildWithResourceUsage> {
        val getBuildScans = GetBuildsFromQueryWithAttributesRequest(repository).get(filter)
        val getOutcome = GetBuildsWithCachePerformanceRequest(repository)
        val outcome = getOutcome.get(getBuildScans, filter)
        val buildWithResourceUsage = GetBuildsResourceUsageRequest(repository).get(getBuildScans, filter)
        val buildProfile = GetBuildsProfileRequest(repository).get(getBuildScans, filter)
        return enrich(outcome, buildWithResourceUsage, buildProfile, isProfile)
    }

    companion object {
        internal fun enrich(
            outcome: List<Build>,
            buildWithResourceUsage: List<BuildWithResourceUsage>,
            buildProfile: List<BuildProfileOverview>,
            isProfile: Boolean,
        ): List<BuildWithResourceUsage> {
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
            return if (isProfile) {
                buildWithResourceUsage.filterNot { it.requestedTask.size == 1 && it.requestedTask.first() == "clean" }
            } else {
                buildWithResourceUsage
            }
        }
    }
}
