package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.SingleMeasurement
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

abstract class KotlinBuildReports {
    fun itHasQualifier(it: Map.Entry<String, MutableList<String>>) =
        it.value.any {
            it.contains("ms") ||
                it.contains("GB") ||
                it.contains("MB") ||
                it.contains("KB") ||
                it.contains(
                    "B",
                )
        }

    fun format(value: String) = value.replace(",", "").replace("ms", "").split(" ")[0]

    fun insertMeasurement(
        category: String,
        valuesFormattedA: List<String>,
        key: String,
        variantA: Number,
        qualifier: String,
        metric: Metric,
    ): SingleMeasurement {
        val variantP50 = valuesFormattedA.map { it.toDouble() }.percentile(50.0).roundToLong()
        val variantP90 = valuesFormattedA.map { it.toDouble() }.percentile(90.0).roundToLong()
        return SingleMeasurement(
            category = category,
            name = key,
            variantMean = "$variantA",
            variantP50 = "$variantP50",
            variantP90 = "$variantP90",
            qualifier = qualifier,
            metric = metric,
        )
    }

    fun getQualifier(value: String) = if (value.contains("ms")) "ms" else value.split(" ")[1]
}
