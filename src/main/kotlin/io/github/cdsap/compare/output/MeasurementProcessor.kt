package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement

class MeasurementProcessor(
    val report: Report,
) {
    fun processMeasurements(measurementsMap: Map<String, List<SingleMeasurement>>): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()

        // Group measurements by category and name
        val groupedMeasurements =
            measurementsMap.values
                .flatten()
                .groupBy { Pair(it.category, it.name) }

        // Process each group
        groupedMeasurements.forEach { (key, measurements) ->
            val (category, name) = key
            // the threshold for the task
            var filteredTaskPath = true
            if (category == "Task Path") {
                var numberOfVariants = 0
                measurementsMap.forEach { (variant, variantMeasurements) ->
                    val measurement =
                        variantMeasurements.find {
                            it.category == category && it.name == name
                        }
                    if (measurement != null) {
                        if (measurement.variantMean.toString().toLong() > report.thresholdTaskDuration) {
                            numberOfVariants++
                        }
                    }
                }
                filteredTaskPath = numberOfVariants == measurementsMap.size
            }
            if (filteredTaskPath) {
                val rowData = mutableMapOf<String, Any?>()

                rowData["category"] = category
                rowData["name"] = name
                rowData["metric"] = measurements.first().metric.name

                // Process measurements for each variant
                measurementsMap.forEach { (variant, variantMeasurements) ->
                    val measurement =
                        variantMeasurements.find {
                            it.category == category && it.name == name
                        }

                    if (measurement != null) {
                        rowData["$variant-mean"] = measurement.variantMean
                        rowData["$variant-median"] = measurement.variantP50
                        rowData["$variant-p90"] = measurement.variantP90
                        rowData["$variant-unit"] = measurement.qualifier
                    }
                }
                result.add(rowData)
            }
        }
        return result
    }

    fun filterUnwantedMetrics(rowData: Map<String, Any?>): Boolean {
        val metric = rowData["metric"].toString()
        return metric != Metric.TASK_PATH.name &&
            metric != Metric.KOTLIN_BUILD_REPORT.name &&
            metric != Metric.TASK_KOTLIN_BUILD_REPORT.name
    }
}
