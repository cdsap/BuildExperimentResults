package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.Build

class MeasurementsByReport(
    private val report: Report, private val profile: Boolean,
    private val warmupsToDiscard: Int
) {

    fun get(
        buildsVariantA: List<Build>,
        buildsVariantB: List<Build>
    ): List<MeasurementWithPercentiles> {
        val measurements = mutableListOf<MeasurementWithPercentiles>()
        if (report.buildReport) {
            measurements += BuildMeasurement(
                filterBuildsWithProfile(buildsVariantA, profile),
                filterBuildsWithProfile(buildsVariantB, profile)).get()
        }
        if (report.processesReport) {
            measurements += ProcessMeasurement(buildsVariantA, buildsVariantB, profile).get()
        }
        if (report.taskTypeReport) {
            measurements += TasksTypeMeasurements(
                filterBuildsWithProfile(buildsVariantA, profile),
                filterBuildsWithProfile(buildsVariantB, profile)
            ).get()
        }
        if (report.taskPathReport) {
            measurements += TasksPathMeasurements(
                filterBuildsWithProfile(buildsVariantA, profile),
                filterBuildsWithProfile(buildsVariantB, profile)
            ).get()
        }
        if (report.kotlinBuildReport) {
            measurements += KotlinBuildReportsMeasurements(
                filterBuildsWithProfile(buildsVariantA, profile),
                filterBuildsWithProfile(buildsVariantB, profile)
            ).get()
        }
        return measurements
    }

    private fun filterBuildsWithProfile(builds: List<Build>, profile: Boolean): List<Build> {
        return if (profile) builds.dropLast(warmupsToDiscard)
        else builds
    }
}
