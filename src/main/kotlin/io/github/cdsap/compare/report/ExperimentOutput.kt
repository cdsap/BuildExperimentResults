package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

interface ExperimentOutput {
    fun generateOutputs(
        measurement: Map<String, List<SingleMeasurement>>,
        buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
    )
}

data class ExperimentArtifacts(
    val html: String,
    val csv: String,
    val filteredCsv: String,
    val ghaSummary: String,
    val openAiAnalysis: String,
    val openAiTitle: String,
    val claudeAnalysis: String,
    val claudeTitle: String,
) {
    companion object {
        fun timestamped(timestamp: String): ExperimentArtifacts =
            ExperimentArtifacts(
                html = "experiment_results_$timestamp.html",
                csv = "experiment_results_$timestamp.csv",
                filteredCsv = "experiment_results_${timestamp}_filtered.csv",
                ghaSummary = "experiment_results_summary_gha",
                openAiAnalysis = "experiment_results_openai_analysis_$timestamp",
                openAiTitle = "experiment_results_openai_title_$timestamp",
                claudeAnalysis = "experiment_results_claude_analysis_$timestamp",
                claudeTitle = "experiment_results_claude_title_$timestamp",
            )
    }
}

interface ExperimentArtifactWriter {
    fun artifacts(): ExperimentArtifacts

    fun write(
        name: String,
        content: String,
    )

    fun exists(name: String): Boolean

    fun file(name: String): File
}

class FileExperimentArtifactWriter : ExperimentArtifactWriter {
    private val artifactSet by lazy {
        ExperimentArtifacts.timestamped(
            SimpleDateFormat("yyyyMMddHHmmss").format(Date()),
        )
    }

    override fun artifacts() = artifactSet

    override fun write(
        name: String,
        content: String,
    ) {
        file(name).writeText(content)
    }

    override fun exists(name: String) = file(name).exists()

    override fun file(
        name: String,
    ) = File(name)
}
