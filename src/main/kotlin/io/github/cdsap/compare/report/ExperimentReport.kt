package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.report.measurements.FilterBuildsPerVariant
import io.github.cdsap.compare.report.measurements.MeasurementsByReport
import io.github.cdsap.compare.view.ExperimentView
import io.github.cdsap.compare.view.GeneralCsvOutputView
import io.github.cdsap.geapi.client.domain.impl.GetBuildsFromQueryWithAttributesRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsResourceUsageRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsWithCachePerformanceRequest
import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.repository.impl.GradleRepositoryImpl

class ExperimentReport(
    private val filter: Filter,
    private val repository: GradleRepositoryImpl,
    private val report: Report
) {

    suspend fun process() {
        if (report.variants.size != 2) {
            throw IllegalArgumentException("The numbers of variants to provide is two")
        } else {
            val getBuildScans = GetBuildsFromQueryWithAttributesRequest(repository).get(filter)
            val getOutcome = GetBuildsWithCachePerformanceRequest(repository)
            val outcome = getOutcome.get(getBuildScans, filter)
            val BuildWithResourceUsage = GetBuildsResourceUsageRequest(repository).get(getBuildScans, filter)
            outcome.forEach { build ->
                val usage = BuildWithResourceUsage.find { build.id == it.id }
                usage?.requestedTask = build.requestedTask
                usage?.values = build.values
                usage?.tags = build.tags
                usage?.buildDuration = build.buildDuration
                usage?.buildStartTime = build.buildStartTime
                usage?.builtTool = build.builtTool
                usage?.projectName = build.projectName
                usage?.taskExecution = build.taskExecution
            }

            val variants = FilterBuildsPerVariant(report).get(BuildWithResourceUsage)

            val measurements = MeasurementsByReport(report).get(variants)

            if (measurements.isNotEmpty()) {
                ExperimentView(report, filter.requestedTask!!).print(measurements, variants)
                GeneralCsvOutputView(report).write(measurements)
            }
        }
    }
}
