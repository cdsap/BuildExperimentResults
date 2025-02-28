package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.median
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class ResourceUsageMeasurement(
    private val variant: List<BuildWithResourceUsage>
) {

    fun get(): List<SingleMeasurement> {
        return processMeasurement()
    }

    private fun processMeasurement(): List<SingleMeasurement> {
        val measurement = mutableListOf<SingleMeasurement>()
        if (variant.isNotEmpty() && variant.first().total != null) {
            extracted(
                variant.flatMap { listOf(it.total.allProcessesCpu.max) },
                "Max",
                "All processes cpu",
                measurement,
                "percentage"
            )

            extracted(
                variant.flatMap { listOf(it.total.allProcessesMemory.max) },
                "Max",
                "All processes memory",
                measurement,
                "bytes"
            )

            extracted(
                variant.flatMap { listOf(it.total.buildProcessCpu.max) },
                "Max",
                "Build process cpu",
                measurement,
                "percentage"
            )

            extracted(
                variant.flatMap { listOf(it.total.buildProcessMemory.max) },
                "Max",
                "Build processes memory",
                measurement,
                "bytes"
            )

            extracted(
                variant.flatMap { listOf(it.total.buildChildProcessesCpu.max) },
                "Max",
                "Build child processes cpu",
                measurement,
                "percentage"
            )

            extracted(
                variant.flatMap { listOf(it.total.buildChildProcessesMemory.max) },
                "Max",
                "Build child processes memory",
                measurement,
                "bytes"
            )
        }
        return measurement
    }

    private fun extracted(
        variantValues: List<Long>,
        name: String,
        category: String,
        measurements: MutableList<SingleMeasurement>,
        type: String
    ) {
        val variantMedian = if (type == "bytes") {
            "${
            bytesToGigabytes(
                variantValues.median()
            )
            }"
        } else {
            "${variantValues.median()}"
        }
        val variantAMean = if (type == "bytes") {
            "${
            bytesToGigabytes(
                variantValues.average()
            )
            }"
        } else {
            "${variantValues.average()}"
        }

        val variantP90 = if (type == "bytes") {
            "${
            bytesToGigabytes(
                variantValues.percentile(90.0)
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
                metric = Metric.RESOURCE_USAGE
            )
        )
    }

    private fun bytesToGigabytes(bytes: Double): Double {
        return String.format("%.2f", bytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
    }
}
