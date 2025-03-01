package io.github.cdsap.compare.view

import ConsoleGenerator
import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import java.io.File

class ExperimentView(
    val report: Report
) {
    private val htmlGenerator: HtmlGenerator = HtmlGenerator()
    private val csvGenerator: CsvGenerator = CsvGenerator()
    private val consoleGenerator: ConsoleGenerator = ConsoleGenerator()

    fun genereateOutputs(
        measurement: Map<String, List<SingleMeasurement>>,
        variants1: Map<String, List<BuildWithResourceUsage>>
    ) {
        val variants = measurement.keys.toList()
        val header = Header(
            numberOfBuilds = variants1.values.flatMap { listOf(it.size) },
            task = variants1.values.first().first().requestedTask.joinToString(","),
            experiment = report.experimentId
        )
        println(consoleGenerator.generate(measurement, variants, header, variants1))
        File("experiment_results.html").writeText(htmlGenerator.generate(measurement, variants, header, variants1))
        File("experiment_results.csv").writeText(csvGenerator.generate(measurement, variants, header, variants1))
        File("experiment_results_summary_gha").writeText(
            htmlGenerator.generateTableSummary(
                measurement,
                variants,
                header,
                variants1
            )
        )
    }
}
