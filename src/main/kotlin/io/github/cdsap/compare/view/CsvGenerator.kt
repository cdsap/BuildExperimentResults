package io.github.cdsap.compare.view

import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class CsvGenerator(
    private val measurementProcessor: MeasurementProcessor = MeasurementProcessor()
) {
    fun generate(
        measurement: Map<String, List<SingleMeasurement>>,
        variants: List<String>,
        header: Header,
        variants1: Map<String, List<BuildWithResourceUsage>>
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
