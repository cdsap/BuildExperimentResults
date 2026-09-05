package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.domain.impl.GetBuildsFromQueryWithAttributesRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsProfileRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsResourceUsageRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsWithCachePerformanceRequest
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.repository.impl.GradleRepositoryImpl

class GeApiBuildLoader(
    private val repository: GradleRepositoryImpl,
) : BuildLoader {
    override suspend fun get(
        filter: Filter,
        report: Report,
    ): List<BuildWithResourceUsage> {
        val getBuildScans = GetBuildsFromQueryWithAttributesRequest(repository).get(filter)
        val getOutcome = GetBuildsWithCachePerformanceRequest(repository)
        val outcome = getOutcome.get(getBuildScans, filter)
        val buildWithResourceUsage = GetBuildsResourceUsageRequest(repository).get(getBuildScans, filter)
        val buildProfile = GetBuildsProfileRequest(repository).get(getBuildScans, filter)
        return BuildUsageAssembler().assemble(outcome, buildWithResourceUsage, buildProfile, report.isProfile)
    }
}
