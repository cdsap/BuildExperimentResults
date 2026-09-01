package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.model.Build
import io.github.cdsap.geapi.client.model.BuildProfileOverview
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

internal class BuildResourceUsageEnricher {
    fun enrich(
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
