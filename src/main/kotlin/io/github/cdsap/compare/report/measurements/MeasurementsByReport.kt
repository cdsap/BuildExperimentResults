package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.BuildsPerVariant
import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.Build

class MeasurementsByReport(
    private val report: Report
) {

    fun get(variants: BuildsPerVariant): List<MeasurementWithPercentiles> {
        val measurements = mutableListOf<MeasurementWithPercentiles>()
        if (report.buildReport) {
            measurements += BuildMeasurement(variants.variantA, variants.variantB).get()
        }
        if (report.processesReport) {
            measurements += ProcessMeasurement(variants.variantA, variants.variantB, report.isProfile).get()
        }
        if (report.taskTypeReport) {
            measurements += TasksTypeMeasurements(variants.variantA, variants.variantB).get()
        }
        if (report.taskPathReport) {
            measurements += TasksPathMeasurements(variants.variantA, variants.variantB).get()
        }
        if (report.kotlinBuildReport) {
            measurements += KotlinBuildReportsMeasurements(variants.variantA, variants.variantB).get()
        }
        return measurements
    }
}
