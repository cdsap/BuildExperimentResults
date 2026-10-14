package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Filter

interface BuildDataSource {
    suspend fun getBuilds(filter: Filter): List<BuildWithResourceUsage>
}
