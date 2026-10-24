package io.github.cdsap.compare.export

import io.github.cdsap.compare.report.ExperimentResultOutput
import java.nio.file.Path

class ExperimentJsonFileOutput(
    private val writer: ExperimentJsonWriter = ExperimentJsonWriter(),
) : ExperimentResultOutput {
    override fun write(
        result: ExperimentResultDto,
        outputJson: String,
    ) {
        writer.write(result, Path.of(outputJson))
    }
}
