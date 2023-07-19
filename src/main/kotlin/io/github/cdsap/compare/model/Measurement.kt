package io.github.cdsap.compare.model

import io.github.cdsap.geapi.client.model.OS

class Measurement(
    val category: String, val name: String, val variantA: Any, val variantB: Any, val OS: OS
) {
    fun diff(): Any {
        if (variantA is Int) {
            if ((variantA as Int) - (variantB as Int) != 0) {
                if (variantA != 0) {
                    val x = (variantB * 100) / variantA
                    val result = 100 - x
                    return "$result%"
                } else {
                    return ""
                }
            } else {
                return ""
            }

        } else if (variantA is Long) {
            if ((variantA as Long) - (variantB as Long) != 0L) {
                if (variantA != 0L) {
                    val x = (variantB * 100L) / variantA
                    val result = 100L - x
                    return "$result%"
                } else {
                    return ""
                }
            } else {
                return ""
            }

        } else if (variantA is Double) {
            if ((variantA as Double) - (variantB as Double) != 0.0) {
                val x = (variantB * 100.0) / variantA
                val result = 100 - x
                return "$result%"
            } else {
                return ""
            }
        } else {

        }
        return ""
    }

}

data class TaskFingerprintingSummary(val count: Int, val serialDuration: Long)

data class MeasurementWithPercentiles(
    val category: String, val name: String,
    val variantAMean: Any,
    val variantBMean: Any,
    val variantAP50: Any,
    val variantBP50: Any,
    val variantAP90: Any,
    val variantBP90: Any,
    val OS: OS
)
