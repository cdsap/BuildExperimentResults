package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.geapi.client.model.Build
import io.github.cdsap.geapi.client.model.CustomValue
import io.github.cdsap.geapi.client.model.OS
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToInt

class ProcessMeasurement(
    private val variantA: List<Build>,
    private val variantB: List<Build>,
    private val profile: Boolean
) {

    fun get(): List<MeasurementWithPercentiles> {
        return proccessMeasurement(variantA, variantB, profile, "Gradle") +
            proccessMeasurement(variantA,variantB,profile,"Kotlin"
        )

    }

    private fun proccessMeasurement(
        variantA: List<Build>,
        variantB: List<Build>,
        profile: Boolean,
        value: String
    ): List<MeasurementWithPercentiles> {
        if (profile) {

            val measurement = mutableListOf<MeasurementWithPercentiles>()
            val variantAValues = parseProcess(variantA.first().values, value)
            val variantBValues = parseProcess(variantB.first().values, value)
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
                            OS = OS.Linux
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
            val listVariantAValues = getListByValues(variantA, value)
            val listVariantBValues = getListByValues(variantB, value)
            val listVariantAValuesFormatted = formatListValues(listVariantAValues)
            val listVariantBValuesFormatted = formatListValues(listVariantBValues)


            listVariantAValuesFormatted.forEach {

                val qualifierInfo = listVariantAValues[it.key]!!.first().split(" ")[1]
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
                        variantAMean = "$varianta $qualifierInfo",
                        variantBMean = "$variantb $qualifierInfo",
                        category = "$value process state",
                        variantAP50 = "$variantaP50 $qualifierInfo",
                        variantBP50 = "$variantbP50 $qualifierInfo",
                        variantAP90 = "$variantaP90 $qualifierInfo",
                        variantBP90 = "$variantbP90 $qualifierInfo",
                        OS = OS.Linux
                    )
                )
            }
            return measurement.toList()
        }
    }

    private fun getListByValues(builds: List<Build>,value: String): Map<String, MutableList<String>> {
        val listVariantValues = mutableMapOf<String, MutableList<String>>()

        builds.forEach {
            val variantAValues = parseProcess(it.values, value)
            variantAValues.forEach {
                if (!listVariantValues.contains(it.key)) {
                    listVariantValues[it.key] = mutableListOf()
                }
                listVariantValues[it.key]!!.add(it.value)
            }
        }
        return listVariantValues
    }

    private fun formatListValues(values:  Map<String, MutableList<String>>) : Map<String, MutableList<Double>> {
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

    private fun parseProcess(values: Array<CustomValue>, value: String): Map<String, String> {
        return if (values.filter { it.name.contains("$value-Process") }.isNotEmpty()) {
            val measurements = mutableMapOf<String, String>()
            values.filter { it.name.contains("$value-Process") }.forEach {
                val name = it.name.split("-").filterIndexed { index, _ ->
                    index != 2 // you can also specify more interesting filters here...
                }.joinToString("-")
                measurements[name] = it.value
            }
            measurements
        } else {
            emptyMap()
        }
    }
}


