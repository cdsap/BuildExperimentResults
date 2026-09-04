package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Filter

interface BuildLoader {
    suspend fun get(
        filter: Filter,
        report: Report,
    ): List<BuildWithResourceUsage>
}
