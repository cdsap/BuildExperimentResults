package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

interface ReportMeasurementCalculator {
    fun get(variants: Map<String, List<BuildWithResourceUsage>>): Map<String, List<SingleMeasurement>>
}
