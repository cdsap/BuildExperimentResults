package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurements
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

interface ExperimentResultOutput {
    fun write(
        report: Report,
        measurements: Map<String, List<SingleMeasurement>>,
        buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
        diagnostics: DiagnosticsMeasurements,
        outputJson: String,
    )
}
