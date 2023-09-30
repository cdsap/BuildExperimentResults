package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.*
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
    private val profile: Boolean,
    private val experimentId: String,
    private val variants: List<String>,
    private val report: Report,
    private val warmupsToDiscard: Int
) {

    suspend fun process() {
        val getBuildScans = GetBuildsFromQueryWithAttributesRequest(repository).get(filter)


        val getOutcome = GetBuildsWithCachePerformanceRequest(repository)
        val outcome = getOutcome.get(getBuildScans, filter).filter { it.tags.contains(experimentId) }
        if (variants.size != 2) {
            throw IllegalArgumentException("The numbers of variants to provide is two")
        } else {
            val variantA = "${experimentId}_variant_experiment_${variants[0].trim()}"
            val variantB = "${experimentId}_variant_experiment_${variants[1].trim()}"

            val buildsVariantA = buildsByVariant(outcome, variantA)
            val buildsVariantB = buildsByVariant(outcome, variantB)

            val measurements =
                MeasurementsByReport(report, profile, warmupsToDiscard).get(buildsVariantA, buildsVariantB)

            if (measurements.isNotEmpty()) {
                ExperimentView().print(
                    measurements, variants[0], variants[1], Header(
                        task = filter.requestedTask.toString(),
                        numberOfBuildsForExperimentA = if (profile) buildsVariantA.dropLast(warmupsToDiscard).size else buildsVariantA.size,
                        numberOfBuildsForExperimentB = if (profile) buildsVariantB.dropLast(warmupsToDiscard).size else buildsVariantB.size,
                        experiment = experimentId
                    )
                )
                GeneralCsvOutputView(measurements, variants[0], variants[1]).write()

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

    private fun buildsByVariant(
        outcome: List<Build>,
        variant: String
    ) = outcome.filter { it.tags.contains("experiment") && it.tags.contains(experimentId) && it.tags.contains(variant) }

}
