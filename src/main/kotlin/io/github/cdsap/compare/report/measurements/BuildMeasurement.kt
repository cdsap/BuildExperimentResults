package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class BuildMeasurement(
    val variant: List<BuildWithResourceUsage>
) {

    fun get(): List<SingleMeasurement> {
        val measurements = mutableListOf<SingleMeasurement>()
        if (variant.isNotEmpty()) {
            val variantMean = "${variant.sumOf { it.buildDuration } / variant.size}"
            val variantP50 = "${variant.flatMap { listOf(it.buildDuration) }.percentile(50.0).roundToLong()}"
            val variantP90 = "${variant.flatMap { listOf(it.buildDuration) }.percentile(90.0).roundToLong()}"
            measurements.add(
                SingleMeasurement(
                    name = "Build time",
                    variantMean = variantMean,
                    category = "Build",
                    variantP50 = variantP50,
                    variantP90 = variantP90,
                    qualifier = "ms",
                    metric = Metric.BUILD
                )
            )
        }
        return measurements
    }
}
