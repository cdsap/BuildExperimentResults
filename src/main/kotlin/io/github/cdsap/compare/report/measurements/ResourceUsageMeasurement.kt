package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.median
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class ResourceUsageMeasurement(
    private val variant: List<BuildWithResourceUsage>,
) {
    fun get(): List<SingleMeasurement> = processMeasurement()

    private fun processMeasurement(): List<SingleMeasurement> {
        val totals = variant.mapNotNull { resourceUsageTotalOrNull(it) }
        val measurement = mutableListOf<SingleMeasurement>()
        if (totals.isEmpty()) {
            return measurement
        }
        extracted(
            totals.map { it.allProcessesCpu.max },
            "Max",
            "All processes cpu",
            measurement,
            "percentage",
        )

        extracted(
            totals.map { it.allProcessesMemory.max },
            "Max",
            "All processes memory",
            measurement,
            "bytes",
        )

        extracted(
            totals.map { it.buildProcessCpu.max },
            "Max",
            "Build process cpu",
            measurement,
            "percentage",
        )

        extracted(
            totals.map { it.buildProcessMemory.max },
            "Max",
            "Build processes memory",
            measurement,
            "bytes",
        )

        extracted(
            totals.map { it.buildChildProcessesCpu.max },
            "Max",
            "Build child processes cpu",
            measurement,
            "percentage",
        )

        extracted(
            totals.map { it.buildChildProcessesMemory.max },
            "Max",
            "Build child processes memory",
            measurement,
            "bytes",
        )
        return measurement
    }

    private fun extracted(
        variantValues: List<Long>,
        name: String,
        category: String,
        measurements: MutableList<SingleMeasurement>,
        type: String,
    ) {
        val variantMedian =
            if (type == "bytes") {
                "${
                    bytesToGigabytes(
                        variantValues.median(),
                    )
                }"
            } else {
                "${variantValues.median()}"
            }
        val variantAMean =
            if (type == "bytes") {
                "${
                    bytesToGigabytes(
                        variantValues.average(),
                    )
                }"
            } else {
                "${variantValues.average()}"
            }

        val variantP90 =
            if (type == "bytes") {
                "${
                    bytesToGigabytes(
                        variantValues.percentile(90.0),
                    )
                }"
            } else {
                "${variantValues.percentile(90.0).roundToLong()}"
            }

        val unit = if (type == "bytes") "Gb" else "%"

        measurements.add(
            SingleMeasurement(
                name = name,
                variantMean = variantAMean,
                category = category,
                variantP50 = variantMedian,
                variantP90 = variantP90,
                qualifier = unit,
                metric = Metric.RESOURCE_USAGE,
            ),
        )
    }

    private fun bytesToGigabytes(bytes: Double): Double = String.format("%.2f", bytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
}
