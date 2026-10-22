package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

interface ExperimentOutput {
    fun generateOutputs(
        measurement: Map<String, List<SingleMeasurement>>,
        buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
    )
}
