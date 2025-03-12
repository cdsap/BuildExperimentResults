package io.github.cdsap.compare.report.measurements.parser

import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.CustomValue

class GCReportParser {

    fun parse(values: Array<CustomValue>, value: String): Map<String, String> {
        return if (values.filter { it.name.contains(value) }.isNotEmpty()) {
            val measurements = mutableMapOf<String, String>()
            values.filter { it.name.contains(value) }.forEach {
                val name = it.name.split("$value-")[1]
                measurements[name] = it.value
            }
            measurements
        } else {
            emptyMap()
        }
    }

    fun parseByVariant(builds: List<BuildWithResourceUsage>, value: String): Map<String, MutableList<String>> {
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
