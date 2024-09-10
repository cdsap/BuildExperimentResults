package io.github.cdsap.compare.model

import io.github.cdsap.geapi.client.model.OS
import kotlin.math.abs

class Measurement(
    val name: String,
    val variantA: String,
    val variantB: String
) {
    fun diff(): Int {
        println(variantA)
        if (isInteger(variantA) && isInteger(variantB)) {
            val abs = abs(variantA.toInt() - variantB.toInt())
            val s = (abs * 100) / variantA.toInt()
            return s
        } else if (isLong(variantA) && isLong(variantB)) {
            val abs = abs(variantA.toLong() - variantB.toLong())
            val s = (abs * 100) / variantA.toLong()
            return s.toInt()
        } else if (isDouble(variantA) && isDouble(variantB)) {
            val abs = abs(variantA.toDouble() - variantB.toDouble())
            val s = (abs * 100) / variantA.toDouble()
            return s.toInt()
        } else {
            return 0
        }
    }
}

data class TaskFingerprintingSummary(val count: Int, val serialDuration: Long)

data class MeasurementWithPercentiles(
    val category: String,
    val name: String,
    val variantAMean: Any,
    val variantBMean: Any,
    val variantAP50: Any,
    val variantBP50: Any,
    val variantAP90: Any,
    val variantBP90: Any,
    val qualifier: String,
    val metric: Metric
)

fun isInteger(str: String): Boolean {
    try {
        // Attempt to parse the string as an integer
        str.toInt()
        return true
    } catch (e: NumberFormatException) {
        // The string couldn't be parsed as an integer
        return false
    }
}

fun isDouble(str: String): Boolean {
    try {
        // Attempt to parse the string as a double
        str.toDouble()
        return true
    } catch (e: NumberFormatException) {
        // The string couldn't be parsed as a double
        return false
    }
}

fun isLong(str: String): Boolean {
    try {
        // Attempt to parse the string as a long
        str.toLong()
        return true
    } catch (e: NumberFormatException) {
        // The string couldn't be parsed as a long
        return false
    }
}
