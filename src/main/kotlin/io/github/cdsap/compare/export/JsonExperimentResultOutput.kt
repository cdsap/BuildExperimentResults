package io.github.cdsap.compare.export

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.ExperimentResultOutput
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurements
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import java.nio.file.Path

class JsonExperimentResultOutput(
    private val assembler: ExperimentResultAssembler = ExperimentResultAssembler(),
    private val writer: ExperimentJsonWriter = ExperimentJsonWriter(),
) : ExperimentResultOutput {
    override fun write(
        report: Report,
        measurements: Map<String, List<SingleMeasurement>>,
        buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
        diagnostics: DiagnosticsMeasurements,
        outputJson: String,
    ) {
        val result = assembler.assemble(report, measurements, buildsByVariant, diagnostics)
        writer.write(result, Path.of(outputJson))
    }
}
