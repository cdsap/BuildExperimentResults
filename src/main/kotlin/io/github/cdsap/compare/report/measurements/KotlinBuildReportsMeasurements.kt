package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.measurements.parser.KotlinBuildReportsParserCustomValues
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class KotlinBuildReportsMeasurements(
    private val variant: List<BuildWithResourceUsage>,
) {
    private val excludedList =
        listOf(
            "Start time of worker execution",
            "Start time of task action",
            "Total memory usage at the end of build",
            "Finish gradle part of task execution",
            "Worker submit time",
            "Increase memory usage",
        )

    fun get(): List<SingleMeasurement> {
        val kotlinReportsParserCustomValues =
            KotlinBuildReportsParserCustomValues(variant).parse()
        val kotlinReportsAggregated = KotlinReportsByTaskPath(kotlinReportsParserCustomValues).get(excludedList)
        val kotlinReportsByTaskPath = KotlinReportsAggregated(kotlinReportsParserCustomValues).get(excludedList)

        return kotlinReportsAggregated + kotlinReportsByTaskPath
    }
}
