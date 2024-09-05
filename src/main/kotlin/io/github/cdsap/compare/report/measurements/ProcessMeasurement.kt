package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.report.measurements.parser.ProcessesReportParser
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.OS
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToInt

class ProcessMeasurement(
    private val variantA: List<BuildWithResourceUsage>,
    private val variantB: List<BuildWithResourceUsage>,
    private val profile: Boolean
) {

    fun get(): List<MeasurementWithPercentiles> {
        return processMeasurement(variantA, variantB, profile, "Gradle") +
            processMeasurement(
                variantA,
                variantB,
                profile,
                "Kotlin"
            )
    }

    private fun processMeasurement(
        variantA: List<BuildWithResourceUsage>,
        variantB: List<BuildWithResourceUsage>,
        profile: Boolean,
        value: String
    ): List<MeasurementWithPercentiles> {
        if (profile) {
            val measurement = mutableListOf<MeasurementWithPercentiles>()
            val processesParser = ProcessesReportParser()
            val variantAValues = processesParser.parse(variantA.first().values, value)
            val variantBValues = processesParser.parse(variantB.first().values, value)
            if (variantAValues.size == variantBValues.size) {
                variantAValues.forEach {
                    val variantB = variantBValues[it.key]!!
                    measurement.add(
                        MeasurementWithPercentiles(
                            name = it.key,
                            variantAMean = it.value,
                            variantBMean = variantB,
                            category = "Last $value process state",
                            variantAP50 = "",
                            variantBP50 = "",
                            variantAP90 = "",
                            variantBP90 = "",
                            OS = OS.Linux,
                            qualifier = "",
                            metric = Metric.PROCESS
                        )
                    )
                }
                return measurement.toList()
            } else {
                return emptyList()
            }
            return emptyList()
        } else {
            val measurement = mutableListOf<MeasurementWithPercentiles>()
            val processesParser = ProcessesReportParser()
            val listVariantAValues = processesParser.parseByVariant(variantA, value)
            val listVariantBValues = processesParser.parseByVariant(variantB, value)
            val listVariantAValuesFormatted = formatListValues(listVariantAValues)
            val listVariantBValuesFormatted = formatListValues(listVariantBValues)

            listVariantAValuesFormatted.forEach {
                val x = listVariantBValuesFormatted[it.key]!!

                val varianta =
                    (((it.value.sumOf { it } / it.value.size) * 100.0).roundToInt() / 100.0)
                val variantb =
                    (((x.sumOf { it } / x.size) * 100.0).roundToInt() / 100.0)
                val variantaP50 = (it.value.percentile(50.0) * 100.0).roundToInt() / 100.0
                val variantbP50 = (x.map { it }.percentile(50.0) * 100.0).roundToInt() / 100.0
                val variantaP90 = (it.value.map { it }.percentile(90.0) * 100.0).roundToInt() / 100.0
                val variantbP90 = (x.map { it }.percentile(90.0) * 100.0).roundToInt() / 100.0
                measurement.add(
                    MeasurementWithPercentiles(
                        name = it.key,
                        variantAMean = "$varianta",
                        variantBMean = "$variantb",
                        category = "$value process state",
                        variantAP50 = "$variantaP50",
                        variantBP50 = "$variantbP50",
                        variantAP90 = "$variantaP90",
                        variantBP90 = "$variantbP90",
                        OS = OS.Linux,
                        qualifier = "",
                        metric = Metric.PROCESS
                    )
                )
            }
            return measurement.toList()
        }
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
