package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.OS
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class BuildMeasurement(
    private val variantA: List<BuildWithResourceUsage>,
    private val variantB: List<BuildWithResourceUsage>
) {

    fun get(): List<MeasurementWithPercentiles> {
        if (variantA.isNotEmpty() && variantB.isNotEmpty()) {
            val variantAMean = "${variantA.sumOf { it.buildDuration } / variantA.size}"
            val variantBMean = "${variantB.sumOf { it.buildDuration } / variantB.size}"
            val variantAP50 = "${variantA.flatMap { listOf(it.buildDuration) }.percentile(50.0).roundToLong()}"
            val variantBP50 = "${variantB.flatMap { listOf(it.buildDuration) }.percentile(50.0).roundToLong()}"
            val variantAP90 = "${variantA.flatMap { listOf(it.buildDuration) }.percentile(90.0).roundToLong()}"
            val variantBP90 = "${variantB.flatMap { listOf(it.buildDuration) }.percentile(90.0).roundToLong()}"

            return listOf(
                MeasurementWithPercentiles(
                    name = "Build time",
                    variantAMean = variantAMean,
                    variantBMean = variantBMean,
                    category = "Build",
                    variantAP50 = variantAP50,
                    variantBP50 = variantBP50,
                    variantAP90 = variantAP90,
                    variantBP90 = variantBP90,
                    qualifier = "ms",
                    metric = Metric.BUILD
                )
            )
        } else {
            return emptyList()
        }
    }
}
