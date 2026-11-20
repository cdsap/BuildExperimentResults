package io.github.cdsap.compare.output

import ConsoleGenerator
import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.ExperimentArtifactWriter
import io.github.cdsap.compare.report.ExperimentOutput
import io.github.cdsap.compare.report.FileExperimentArtifactWriter
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

/** Compatibility HTML/CSV/GHA output. Canonical machine-readable results are written by --output-json. */
class ExperimentView internal constructor(
    val report: Report,
    private val artifactWriter: ExperimentArtifactWriter,
    private val openAiKey: String?,
    private val claudeKey: String?,
    private val openAiAnalysis: (String) -> String,
    private val claudeAnalysis: (String) -> String,
) : ExperimentOutput {
    constructor(
        report: Report,
        artifactWriter: ExperimentArtifactWriter = FileExperimentArtifactWriter(),
        openAiKey: String? = null,
        claudeKey: String? = null,
    ) : this(
        report = report,
        artifactWriter = artifactWriter,
        openAiKey = openAiKey,
        claudeKey = claudeKey,
        openAiAnalysis = { csv -> OpenAiAnalysis(csv, openAiKey).request() },
        claudeAnalysis = { csv -> ClaudeAnalysis(csv, claudeKey).request() },
    )

    private val htmlGenerator: HtmlGenerator = HtmlGenerator(report, chartGenerator = ChartGenerator(report))
    private val csvGenerator: CsvGenerator = CsvGenerator(report)
    private val consoleGenerator: ConsoleGenerator = ConsoleGenerator(report)

    override fun diagnosticsWarning(message: String) {
        println("diagnostics warning: $message")
    }

    override fun generateOutputs(
        measurement: Map<String, List<SingleMeasurement>>,
        buildsByVariant: Map<String, List<BuildWithResourceUsage>>,
    ) {
        val variants = measurement.keys.toList()
        val artifacts = artifactWriter.artifacts()

        val header =
            Header(
                numberOfBuilds = buildsByVariant.values.flatMap { listOf(it.size) },
                task =
                    buildsByVariant.values
                        .first()
                        .first()
                        .requestedTask
                        .joinToString(","),
                experimentId = report.experimentId,
                repository = report.repository,
                url = report.url,
                linkCsv = artifacts.csv,
                experimentRunId = report.experimentRunId,
            )
        println(consoleGenerator.generate(measurement, variants, header, buildsByVariant))
        println("generating html charts ${artifacts.html}")
        artifactWriter.write(
            artifacts.html,
            htmlGenerator.generate(
                measurement,
                variants,
                header.copy(htmlSummary = true),
                buildsByVariant,
            ),
        )
        println("generating csv ${artifacts.csv}")
        artifactWriter.write(artifacts.csv, csvGenerator.generate(measurement, variants, header, false))
        println("generating gha summary")
        artifactWriter.write(
            artifacts.ghaSummary,
            htmlGenerator.generateTableSummary(
                measurement,
                variants,
                header,
                buildsByVariant,
            ),
        )
        var filteredCsvContent: String? = null

        fun filteredCsv(): String =
            filteredCsvContent
                ?: csvGenerator.generate(measurement, variants, header, true).also {
                    filteredCsvContent = it
                    artifactWriter.write(artifacts.filteredCsv, it)
                }

        if (artifactWriter.exists(artifacts.csv) && report.openAiRequest) {
            println("generating open ai analysis")
            val result = openAiAnalysis(filteredCsv())
            println(result)
            val extractSummary = result.split("##")[1].replace("Summary", "")
            if (result.isNotEmpty()) {
                artifactWriter.write(artifacts.openAiAnalysis, result)
                artifactWriter.write(artifacts.openAiTitle, extractSummary)
            }
        }
        if (artifactWriter.exists(artifacts.csv) && report.claudeRequest) {
            val analysisInput = filteredCsv()
            if (!claudeKey.isNullOrEmpty()) {
                println("generating claude analysis via API")
                val result = claudeAnalysis(analysisInput)
                println(result)
                val extractSummary = result.split("##")[1].replace("Summary", "")
                if (result.isNotEmpty()) {
                    artifactWriter.write(artifacts.claudeAnalysis, result)
                    artifactWriter.write(artifacts.claudeTitle, extractSummary)
                }
            } else {
                println("generating filtered csv for claude code analysis: ${artifacts.filteredCsv}")
            }
        }
    }
}
