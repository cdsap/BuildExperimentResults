package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.domain.impl.GetBuildScansWithQueryImpl
import io.github.cdsap.geapi.client.model.*
import io.github.cdsap.geapi.client.repository.impl.GradleRepositoryImpl
import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.report.measurements.KotlinBuildReportsMeasurements
import io.github.cdsap.compare.report.measurements.ProcessMeasurement
import io.github.cdsap.compare.report.measurements.TasksMeasurements
import io.github.cdsap.compare.view.ExperimentView
import io.github.cdsap.geapi.client.domain.impl.GetCachePerformanceImpl
import org.nield.kotlinstatistics.percentile
import java.lang.IllegalStateException
import kotlin.math.roundToInt
import kotlin.math.roundToLong


class ExperimentReport(
    private val filter: Filter,
    private val repository: GradleRepositoryImpl,
    private val profile: Boolean
) {

    suspend fun process() {
        val getBuildScans = GetBuildScansWithQueryImpl(repository).get(filter)
        val getOutcome = GetCachePerformanceImpl(repository)
        val outcome = getOutcome.get(getBuildScans, filter).filter { it.tags.contains(filter.experimentId) }
        if (filter.variants == null) {
            throw IllegalArgumentException("Variants can not be null")
        } else {
            val variants = filter.variants!!.split(",")
            val variantA = "${filter.experimentId}_variant_experiment_${variants[0].trim()}"
            val variantB = "${filter.experimentId}_variant_experiment_${variants[1].trim()}"
            outcome.map {
                if (it.tags.contains("experiment") && it.tags.contains(variantA)) {
                    it.experiment = Experiment.VARIANT_A
                }
                if (it.tags.contains("experiment") && it.tags.contains(variantB)) {
                    it.experiment = Experiment.VARIANT_B
                }
            }

            val buildsVariantA =
                filterbuildsWithProfile(outcome.filter { it.experiment == Experiment.VARIANT_A }, profile).size
            val buildsVariantB =
                filterbuildsWithProfile(outcome.filter { it.experiment == Experiment.VARIANT_B }, profile).size
            if (buildsVariantA != buildsVariantB) {
                throw IllegalStateException("Different number of builds: ${variants[0]} $buildsVariantA - ${variants[1]} $buildsVariantB")
            }
            val measurements = measurements(outcome)
            if (measurements.isNotEmpty()) {
                ExperimentView().print(
                    measurements, variants[0], variants[1], Header(
                        task = filter.requestedTask.toString(),
                        numberOfBuildsForExperimentA = buildsVariantA,
                        numberOfBuildsForExperimentB = buildsVariantB,
                        experiment = filter.experimentId!!
                    )
                )
            }
        }
    }

    private fun filterbuildsWithProfile(builds: List<Build>, profile: Boolean): List<Build> {
        return if (profile) builds.dropLast(2)
        else builds
    }

    private fun measurements(builds: List<Build>): List<MeasurementWithPercentiles> {
        return builds.groupBy { it.OS }.flatMap {
            val buildsVariantA = it.value.filter { it.experiment == Experiment.VARIANT_A }
            val buildsVariantB = it.value.filter { it.experiment == Experiment.VARIANT_B }

            TasksMeasurements(
                filterbuildsWithProfile(buildsVariantA, profile),
                filterbuildsWithProfile(buildsVariantB, profile)
            ).get() +
                KotlinBuildReportsMeasurements(
                    filterbuildsWithProfile(buildsVariantA, profile),
                    filterbuildsWithProfile(buildsVariantB, profile)
                ).get() +
                ProcessMeasurement(buildsVariantA, buildsVariantB, profile).get()
        }
    }
}


data class Header(
    val task: String,
    val numberOfBuildsForExperimentA: Int,
    val numberOfBuildsForExperimentB: Int,
    val experiment: String
)
