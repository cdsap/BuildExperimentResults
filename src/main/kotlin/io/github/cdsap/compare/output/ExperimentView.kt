package io.github.cdsap.compare.output

import ConsoleGenerator
import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class ExperimentView(
    val report: Report,
) {
    private val htmlGenerator: HtmlGenerator = HtmlGenerator(report, chartGenerator = ChartGenerator(report))
    private val csvGenerator: CsvGenerator = CsvGenerator(report)
    private val consoleGenerator: ConsoleGenerator = ConsoleGenerator(report)

    fun generateOutputs(
        measurement: Map<String, List<SingleMeasurement>>,
        variants1: Map<String, List<BuildWithResourceUsage>>,
    ) {
        val variants = measurement.keys.toList()
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val htmlFile = "experiment_results_$timestamp.html"
        val csvFile = "experiment_results_$timestamp.csv"
        val csvFileFiltered = "experiment_results_${timestamp}_filtered.csv"

        val header =
            Header(
                numberOfBuilds = variants1.values.flatMap { listOf(it.size) },
                task =
                    variants1.values
                        .first()
                        .first()
                        .requestedTask
                        .joinToString(","),
                experimentId = report.experimentId,
                repository = report.repository,
                url = report.url,
                linkCsv = csvFile,
                experimentRunId = report.experimentRunId,
            )
        println(consoleGenerator.generate(measurement, variants, header, variants1))
        println("generating html charts $htmlFile")
        File(htmlFile).writeText(
            htmlGenerator.generate(
                measurement,
                variants,
                header.copy(htmlSummary = true),
                variants1,
            ),
        )
        println("generating csv $csvFile")
        File(csvFile).writeText(csvGenerator.generate(measurement, variants, header, false))
        println("generating gha summary")
        File("experiment_results_summary_gha").writeText(
            htmlGenerator.generateTableSummary(
                measurement,
                variants,
                header,
                variants1,
            ),
        )
        if (File(csvFile).exists() && report.openAiRequest) {
            println("generating open ai analysis")
            File(csvFileFiltered).writeText(csvGenerator.generate(measurement, variants, header, true))
            val analysis = OpenAiAnalysis(File(csvFileFiltered), report.openAiKey)
            val result = analysis.request()
            println(result)
            val extractSummary = result.split("##")[1].replace("Summary", "")
            if (result.isNotEmpty()) {
                File("experiment_results_openai_analysis_$timestamp").writeText(result)
                File("experiment_results_openai_title_$timestamp").writeText(extractSummary)
            }
        }
    }
}
