package io.github.cdsap.compare.export

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.ExperimentResultExporter
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurements
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import java.nio.file.Path

class JsonExperimentResultExporter(
    private val assembler: ExperimentResultAssembler = ExperimentResultAssembler(),
    private val writer: ExperimentJsonWriter = ExperimentJsonWriter(),
) : ExperimentResultExporter {
    override fun export(
        report: Report,
        measurements: Map<String, List<SingleMeasurement>>,
        buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
        diagnostics: DiagnosticsMeasurements,
        outputJson: String,
    ) {
        val result = assembler.assemble(report, measurements, buildsByVariant, diagnostics)
        writer.write(result, Path.of(outputJson))
        println("generating json $outputJson")
    }
}
