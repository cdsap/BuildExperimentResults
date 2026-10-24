package io.github.cdsap.compare.report

import io.github.cdsap.compare.export.ExperimentResultDto

interface ExperimentResultOutput {
    fun write(
        result: ExperimentResultDto,
        outputJson: String,
    )
}
