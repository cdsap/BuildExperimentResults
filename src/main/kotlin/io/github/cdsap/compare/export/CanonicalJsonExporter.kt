package io.github.cdsap.compare.export

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurements
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

interface CanonicalJsonExporter {
    fun export(
        report: Report,
        measurements: Map<String, List<SingleMeasurement>>,
        buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
        diagnostics: DiagnosticsMeasurements,
        outputJson: String,
    )
}
