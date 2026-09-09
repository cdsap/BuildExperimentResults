package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Filter

interface BuildsProvider {
    suspend fun getBuilds(filter: Filter): List<BuildWithResourceUsage>
}
