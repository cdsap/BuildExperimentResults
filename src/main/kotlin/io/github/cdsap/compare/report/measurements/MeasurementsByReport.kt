package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class MeasurementsByReport(
    private val report: Report
) {

    fun get(variants: Map<String, List<BuildWithResourceUsage>>): Map<String, List<SingleMeasurement>> {
        val measurementsPerVariant = mutableMapOf<String, List<SingleMeasurement>>()
        variants.forEach { t, u ->
            val measurements = mutableListOf<SingleMeasurement>()
            if (report.buildReport) {
                measurements += BuildMeasurement(u).get()
            }
            if (report.processesReport) {
                measurements += ProcessMeasurement(u, report.isProfile).get()
            }
            if (report.taskTypeReport) {
                measurements += TasksTypeMeasurements(u, report).get()
            }
            if (report.taskPathReport) {
                measurements += TasksPathMeasurements(u, report).get()
            }
            if (report.kotlinBuildReport) {
                measurements += KotlinBuildReportsMeasurements(u).get()
            }
            if (report.resourceUsageReport) {
                measurements += ResourceUsageMeasurement(u).get()
            }
            if (report.gcReport) {
                measurements += GCReportMeasurement(u, report.isProfile).get()
            }
            measurementsPerVariant[t] = measurements
        }

        return measurementsPerVariant
    }
}
