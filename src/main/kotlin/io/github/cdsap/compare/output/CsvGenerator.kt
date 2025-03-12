package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.SingleMeasurement

class CsvGenerator(
    private val measurementProcessor: MeasurementProcessor = MeasurementProcessor()
) {
    fun generate(
        measurement: Map<String, List<SingleMeasurement>>,
        variants: List<String>,
        header: Header,
        filteredByAiRequest: Boolean
    ): String {
        val output = StringBuilder()

        // Column headers
        output.append("Category,Metric")
        variants.forEach { variant ->
            output.append(",${variant.removeExperimentId(header.experimentId)} Mean")
        }
        variants.forEach { variant ->
            output.append(",${variant.removeExperimentId(header.experimentId)} P50")
        }
        variants.forEach { variant ->
            output.append(",${variant.removeExperimentId(header.experimentId)} P90")
        }
        output.append(",Unit\n")

        // Data rows
        measurementProcessor.processMeasurements(measurement)
            .filter {
                if (filteredByAiRequest) {
                    if (it["metric"] == "Task Path" || it["metric"] == "Task Type") {
                        val p90Value = when (val p90 = "${variants[0]}-p90") {
                            is String -> p90.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        p90Value >= 1000
                    } else {
                        true
                    }
                    measurementProcessor.filterUnwantedMetrics(it)
                } else {
                    true
                }
            }
            .forEach { rowData ->
                output.append(generateCsvRow(rowData, variants))
            }

        return output.toString()
    }

    private fun generateCsvRow(rowData: Map<String, Any?>, variants: List<String>): String {
        val output = StringBuilder()
        output.append("${rowData["category"]},${rowData["name"]}")

        // Mean values
        variants.forEach { variant ->
            output.append(",${rowData["$variant-mean"] ?: ""}")
        }
        // P50 values
        variants.forEach { variant ->
            output.append(",${rowData["$variant-median"] ?: ""}")
        }
        // P90 values
        variants.forEach { variant ->
            output.append(",${rowData["$variant-p90"] ?: ""}")
        }
        // Unit in the last column
        output.append(",${rowData["${variants[0]}-unit"] ?: ""}\n")

        return output.toString()
    }
}
