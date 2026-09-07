package io.github.cdsap.compare.report

import io.github.cdsap.compare.report.measurements.BuildWithResourceUsageEnricher
import io.github.cdsap.geapi.client.domain.impl.GetBuildsFromQueryWithAttributesRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsProfileRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsResourceUsageRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsWithCachePerformanceRequest
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.repository.GradleEnterpriseRepository

class GeApiBuildDataSource(
    private val repository: GradleEnterpriseRepository,
    private val isProfile: Boolean,
) : BuildDataSource {
    override suspend fun getBuilds(filter: Filter): List<BuildWithResourceUsage> {
        val getBuildScans = GetBuildsFromQueryWithAttributesRequest(repository).get(filter)
        val getOutcome = GetBuildsWithCachePerformanceRequest(repository)
        val outcome = getOutcome.get(getBuildScans, filter)
        val buildWithResourceUsage = GetBuildsResourceUsageRequest(repository).get(getBuildScans, filter)
        val buildProfile = GetBuildsProfileRequest(repository).get(getBuildScans, filter)
        return BuildWithResourceUsageEnricher().enrich(outcome, buildWithResourceUsage, buildProfile, isProfile)
    }
}
