package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement

class CsvGenerator(
    private val report: Report,
    private val measurementProcessor: MeasurementProcessor = MeasurementProcessor(report)
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
            .forEach { rowData ->
                val content = if (filteredByAiRequest) {
                    generateCsvRowFiltered(rowData, variants)
                } else {
                    generateCsvRow(rowData, variants)
                }
                if (content.isNotEmpty()) {
                    output.append(content)
                }
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

    private fun generateCsvRowFiltered(rowData: Map<String, Any?>, variants: List<String>): String {

        // if any of the variants are missing mean, median or p90 values, skip the row
        var check = 0
        variants.forEach { variant ->
            if(rowData["$variant-mean"] != null){
                check++
            }
        }
        if(check != variants.count()){
            return ""
        }
        val output = StringBuilder()
        val kotlinBuildReport = rowData["category"] == "Kotlin Build Reports"
        val build = rowData["category"] == "Build"
        val kotlinGCTime = rowData["category"] == "Kotlin process state" && rowData["name"] == "Kotlin-Process-gcTime"
        val gradleGCTime = rowData["category"] == "Gradle process state" && rowData["name"] == "Gradle-Process-gcTime"
        val totalCollections = rowData["name"] == "total-collections"
        val totalProcesses = rowData["name"] == "Max"
        val task = rowData["category"] == "Task Path" && rowData["${variants.first()}-median"].toString().toLong() > 1000
        val taskPath = rowData["category"] == "Task Type" && rowData["${variants.first()}-median"].toString().toLong() > 1000

        if (kotlinBuildReport || build || kotlinGCTime || gradleGCTime || totalCollections || totalProcesses || task || taskPath) {

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
        return ""
    }
}
