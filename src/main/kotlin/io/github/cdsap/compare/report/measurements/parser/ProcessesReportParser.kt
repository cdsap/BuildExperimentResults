package io.github.cdsap.compare.report.measurements.parser

import io.github.cdsap.geapi.client.model.Build
import io.github.cdsap.geapi.client.model.CustomValue

class ProcessesReportParser {

    fun parse(values: Array<CustomValue>, value: String): Map<String, String> {
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

    fun parseByVariant(builds: List<Build>, value: String): Map<String, MutableList<String>> {
        val listVariantValues = mutableMapOf<String, MutableList<String>>()

        builds.forEach {
            val variantValues = parse(it.values, value)
            variantValues.forEach {
                if (!listVariantValues.contains(it.key)) {
                    listVariantValues[it.key] = mutableListOf()
                }
                listVariantValues[it.key]!!.add(it.value)
            }
        }
        return listVariantValues
    }
}
