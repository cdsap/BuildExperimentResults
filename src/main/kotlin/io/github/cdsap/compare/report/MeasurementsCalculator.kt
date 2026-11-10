package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

interface MeasurementsCalculator {
    fun get(variants: Map<String, List<BuildWithResourceUsage>>): Map<String, List<SingleMeasurement>>
}
