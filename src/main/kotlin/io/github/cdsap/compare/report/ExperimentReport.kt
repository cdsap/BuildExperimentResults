package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.*
import io.github.cdsap.compare.report.measurements.FilterBuildsPerVariant
import io.github.cdsap.geapi.client.model.*
import io.github.cdsap.geapi.client.repository.impl.GradleRepositoryImpl
import io.github.cdsap.compare.report.measurements.MeasurementsByReport
import io.github.cdsap.compare.view.ExperimentView
import io.github.cdsap.compare.view.GeneralCsvOutputView
import io.github.cdsap.geapi.client.domain.impl.GetBuildsFromQueryWithAttributesRequest
import io.github.cdsap.geapi.client.domain.impl.GetBuildsWithCachePerformanceRequest


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

            val variants = FilterBuildsPerVariant(report).get(outcome)

            val measurements = MeasurementsByReport(report).get(variants)

            if (measurements.isNotEmpty()) {
                ExperimentView(report, filter.requestedTask!!).print(measurements, variants)
                GeneralCsvOutputView(report).write(measurements)
            }
        }
    }

    // Future release of the max diff per util
    private fun extracted(measurements: List<MeasurementWithPercentiles>) {
        val a = mutableListOf<Measurement>()
        measurements.filter { it.metric != Metric.TASK_KOTLIN_BUILD_REPORT }.forEach {
            a.add(
                Measurement(
                    variantA = it.variantAP50.toString(),
                    variantB = it.variantBP50.toString(),
                    name = "${it.category}-${it.name}"
                )
            )
        }

        a.forEach {
            println("${it.name} ==== ${it.diff()}")

        }

        a.sortedBy { it.diff() }.forEach {
            println("${it.name} ==== ${it.diff()}")
        }
    }
}
