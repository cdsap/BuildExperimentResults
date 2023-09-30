package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.report.measurements.parser.KotlinBuildReportsParserCustomValues
import io.github.cdsap.geapi.client.model.Build

class KotlinBuildReportsMeasurements(
    private val variantA: List<Build>,
    private val variantB: List<Build>
) {

    private val excludedList = listOf(
        "Start time of worker execution", "Start time of task action", "Total memory usage at the end of build",
        "Finish gradle part of task execution", "Worker submit time"
    )

    fun get(): List<MeasurementWithPercentiles> {
        val kotlinReportsParserCustomValues =
            KotlinBuildReportsParserCustomValues(variantA, variantB).parse()
        val kotlinReportsAggregated = KotlinReportsByTaskPath(kotlinReportsParserCustomValues).get(excludedList)
        val kotlinReportsByTaskPath = KotlinReportsAggregated(kotlinReportsParserCustomValues).get(excludedList)
        return kotlinReportsAggregated + kotlinReportsByTaskPath
    }
}
