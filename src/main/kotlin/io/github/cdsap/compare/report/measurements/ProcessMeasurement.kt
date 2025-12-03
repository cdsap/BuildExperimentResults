package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.measurements.parser.ProcessesReportParser
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToInt

class ProcessMeasurement(
    private val variant: List<BuildWithResourceUsage>,
    private val profile: Boolean,
) {
    fun get(): List<SingleMeasurement> =
        processMeasurement(profile, "Gradle") +
            processMeasurement(
                profile,
                "Kotlin",
            )

    private fun processMeasurement(
        profile: Boolean,
        value: String,
    ): List<SingleMeasurement> {
        val measurement = mutableListOf<SingleMeasurement>()
        if (profile) {
            val processesParser = ProcessesReportParser()
            val variantAValues = processesParser.parse(variant.first().values, value)
            variantAValues.forEach {
                measurement.add(
                    SingleMeasurement(
                        name = it.key,
                        variantMean = it.value,
                        category = "Last $value process state",
                        variantP50 = "",
                        variantP90 = "",
                        qualifier = "",
                        metric = Metric.PROCESS,
                    ),
                )
            }
        } else {
            val processesParser = ProcessesReportParser()
            val listVariantAValues = processesParser.parseByVariant(variant, value)
            val listVariantAValuesFormatted = formatListValues(listVariantAValues)

            listVariantAValuesFormatted.forEach {
                val varianta =
                    (((it.value.sumOf { it } / it.value.size) * 100.0).roundToInt() / 100.0)
                val variantaP50 = (it.value.percentile(50.0) * 100.0).roundToInt() / 100.0
                val variantaP90 = (it.value.map { it }.percentile(90.0) * 100.0).roundToInt() / 100.0
                measurement.add(
                    SingleMeasurement(
                        name = it.key,
                        variantMean = "$varianta",
                        category = "$value process state",
                        variantP50 = "$variantaP50",
                        variantP90 = "$variantaP90",
                        qualifier = "",
                        metric = Metric.PROCESS,
                    ),
                )
            }
        }
        return measurement
    }

    private fun formatListValues(values: Map<String, MutableList<String>>): Map<String, MutableList<Double>> {
        val listValuesFormatted = mutableMapOf<String, MutableList<Double>>()

        values.forEach {
            if (it.value.filter { it.contains("GB") || it.contains("minutes") }.isNotEmpty()) {
                val aux = mutableListOf<Double>()
                it.value.filter { it.contains("GB") || it.contains("minutes") }.forEach {
                    val valuesNo = it.split(" ")
                    aux.add(valuesNo[0].toDouble())
                }
                listValuesFormatted[it.key] = aux
            }
        }
        return listValuesFormatted
    }
}
