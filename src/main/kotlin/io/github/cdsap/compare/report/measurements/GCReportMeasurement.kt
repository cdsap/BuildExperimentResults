package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.measurements.parser.GCReportParser
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToInt

class GCReportMeasurement(
    private val variant: List<BuildWithResourceUsage>,
    private val profile: Boolean,
) {
    fun get(): List<SingleMeasurement> {
        val uniqueCollections = mutableSetOf<String>()
        variant.forEach {
            uniqueCollections.addAll(
                it.values.filter { it.name.contains("-total-collections") }.map { it.name.replace("-total-collections", "") },
            )
        }
        uniqueCollections.flatMap { processMeasurement(profile, it) }

        return uniqueCollections.flatMap { processMeasurement(profile, it) }
    }

    private fun processMeasurement(
        profile: Boolean,
        value: String,
    ): List<SingleMeasurement> {
        val measurement = mutableListOf<SingleMeasurement>()
        if (profile) {
            val gcReportParser = GCReportParser()
            val variantAValues = gcReportParser.parse(variant.first().values, value)
            variantAValues.forEach {
                measurement.add(
                    SingleMeasurement(
                        name = it.key,
                        variantMean = it.value,
                        category = "Last $value process state",
                        variantP50 = "",
                        variantP90 = "",
                        qualifier = "",
                        metric = Metric.GC_REPORT,
                    ),
                )
            }
        } else {
            val gcReportParser = GCReportParser()
            val listVariantAValues = gcReportParser.parseByVariant(variant, value)

            listVariantAValues.forEach {
                val varianta =
                    (((it.value.sumOf { it.toInt() } / it.value.size) * 100.0).roundToInt() / 100.0)
                val variantaP50 = (it.value.map { it.toInt() }.percentile(50.0) * 100.0).roundToInt() / 100.0
                val variantaP90 = (it.value.map { it.toInt() }.percentile(90.0) * 100.0).roundToInt() / 100.0
                measurement.add(
                    SingleMeasurement(
                        name = it.key,
                        variantMean = "$varianta",
                        category = value,
                        variantP50 = "$variantaP50",
                        variantP90 = "$variantaP90",
                        qualifier = "",
                        metric = Metric.GC_REPORT,
                    ),
                )
            }
        }
        return measurement
    }
}
