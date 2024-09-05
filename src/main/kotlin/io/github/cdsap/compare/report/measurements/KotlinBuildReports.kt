package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.geapi.client.model.OS
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

abstract class KotlinBuildReports {
    fun itHasQualifier(it: Map.Entry<String, MutableList<String>>) =
        it.value.any {
            it.contains("ms") || it.contains("GB") || it.contains("MB") || it.contains("KB") || it.contains(
                "B"
            )
        }

    fun format(value: String) = value.replace(",", "").replace("ms", "").split(" ")[0]

    fun insertMeasurement(
        category: String,
        valuesFormattedA: List<String>,
        valuesFormattedB: List<String>,
        key: String,
        variantA: Number,
        variantB: Number,
        qualifier: String,
        metric: Metric
    ): MeasurementWithPercentiles {
        val variantaP50 = valuesFormattedA.map { it.toDouble() }.percentile(50.0).roundToLong()
        val variantbP50 = valuesFormattedB.map { it.toDouble() }.percentile(50.0).roundToLong()
        val variantaP90 = valuesFormattedA.map { it.toDouble() }.percentile(90.0).roundToLong()
        val variantbP90 = valuesFormattedB.map { it.toDouble() }.percentile(90.0).roundToLong()
        return MeasurementWithPercentiles(
            category = category,
            name = key,
            variantAMean = "$variantA",
            variantBMean = "$variantB",
            variantAP50 = "$variantaP50",
            variantBP50 = "$variantbP50",
            variantAP90 = "$variantaP90",
            variantBP90 = "$variantbP90",
            OS = OS.Linux,
            qualifier = qualifier,
            metric = metric
        )
    }

    fun getQualifier(value: String) =
        if (value.contains("ms")) "ms" else value.split(" ")[1]
}
