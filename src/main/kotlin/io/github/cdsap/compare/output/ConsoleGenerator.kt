import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.output.MeasurementProcessor
import io.github.cdsap.compare.output.removeExperimentId
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class ConsoleGenerator(
    private val report: Report,
    private val measurementProcessor: MeasurementProcessor = MeasurementProcessor(report),
) {
    fun generate(
        measurement: Map<String, List<SingleMeasurement>>,
        variants: List<String>,
        header: Header,
        variants1: Map<String, List<BuildWithResourceUsage>>,
    ) = table {
        cellStyle {
            border = true
            alignment = TextAlignment.MiddleLeft
            paddingLeft = 1
            paddingRight = 1
        }
        body {
            if (header.experimentId != null) {
                row {
                    cell("Experiment id")
                    cell(header.experimentId) {
                        columnSpan = (variants.size * 3) + 1
                    }
                }
            }
            row {
                cell("Task")
                cell(header.task) {
                    columnSpan = (variants.size * 3) + 1
                }
            }

            variants1.forEach { (variant, builds) ->
                row {
                    cell(variant.removeExperimentId(header.experimentId))
                    cell("Builds processed: ${builds.size}") {
                        columnSpan = (variants.size * 3) + 1
                    }
                }
            }

            row {
                cell("Category") {
                    rowSpan = 2
                    alignment = TextAlignment.MiddleCenter
                }
                cell("Metric") {
                    rowSpan = 2
                    alignment = TextAlignment.MiddleCenter
                }
                cell("Mean") {
                    columnSpan = variants.size
                    alignment = TextAlignment.MiddleCenter
                }
                cell("P50") {
                    columnSpan = variants.size
                    alignment = TextAlignment.MiddleCenter
                }
                cell("P90") {
                    columnSpan = variants.size
                    alignment = TextAlignment.MiddleCenter
                }
            }

            row {
                variants.forEach { variant ->
                    cell(variant.removeExperimentId(header.experimentId).formatString()) { alignment = TextAlignment.MiddleCenter }
                }
                variants.forEach { variant ->
                    cell(variant.removeExperimentId(header.experimentId).formatString()) { alignment = TextAlignment.MiddleCenter }
                }
                variants.forEach { variant ->
                    cell(variant.removeExperimentId(header.experimentId).formatString()) { alignment = TextAlignment.MiddleCenter }
                }
            }

            measurementProcessor
                .processMeasurements(measurement)
                .filter { measurementProcessor.filterUnwantedMetrics(it) }
                .forEach { rowData ->
                    row {
                        cell(rowData["category"].toString().splitString())
                        cell(rowData["name"].toString().formatString())

                        variants.forEach { variant ->
                            cell(rowData["$variant-mean"]?.toString() + " " + rowData["$variant-unit"]) {
                                alignment = TextAlignment.MiddleRight
                            }
                        }
                        variants.forEach { variant ->
                            cell(rowData["$variant-median"]?.toString() + " " + rowData["$variant-unit"]) {
                                alignment = TextAlignment.MiddleRight
                            }
                        }
                        variants.forEach { variant ->
                            cell(rowData["$variant-p90"]?.toString() + " " + rowData["$variant-unit"]) {
                                alignment = TextAlignment.MiddleRight
                            }
                        }
                    }
                }
        }
    }.toString()

    private fun String.splitString() =
        this
            .replace("_", " ")
            .lowercase()
            .split(" ")
            .joinToString(" ") { it }

    private fun String.formatString() = this.chunked(22).joinToString("\n")
}
