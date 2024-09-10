package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.OS
import org.nield.kotlinstatistics.median
import org.nield.kotlinstatistics.percentile
import kotlin.math.roundToLong

class ResourceUsageMeasurement(
    private val variantA: List<BuildWithResourceUsage>,
    private val variantB: List<BuildWithResourceUsage>
) {

    fun get(): List<MeasurementWithPercentiles> {
        if (variantA.any { it.total == null } || variantB.any { it.total == null }) {
            return emptyList()
        } else {
            return processMeasurement(variantA, variantB)
        }
    }

    private fun processMeasurement(
        variantA: List<BuildWithResourceUsage>,
        variantB: List<BuildWithResourceUsage>
    ): List<MeasurementWithPercentiles> {
        val measurement = mutableListOf<MeasurementWithPercentiles>()

        extracted(
            variantA.flatMap { listOf(it.total.allProcessesCpu.max) },
            variantB.flatMap { listOf(it.total.allProcessesCpu.max) },
            "Max",
            "All processes cpu",
            measurement,
            "percentage"
        )

        extracted(
            variantA.flatMap { listOf(it.total.allProcessesMemory.max) },
            variantB.flatMap { listOf(it.total.allProcessesMemory.max) },
            "Max",
            "All processes memory",
            measurement,
            "bytes"
        )

        extracted(
            variantA.flatMap { listOf(it.total.buildProcessCpu.max) },
            variantB.flatMap { listOf(it.total.buildProcessCpu.max) },
            "Max",
            "Build process cpu",
            measurement,
            "percentage"
        )

        extracted(
            variantA.flatMap { listOf(it.total.buildProcessMemory.max) },
            variantB.flatMap { listOf(it.total.buildProcessMemory.max) },
            "Max",
            "Build processes memory",
            measurement,
            "bytes"
        )

        extracted(
            variantA.flatMap { listOf(it.total.buildChildProcessesCpu.max) },
            variantB.flatMap { listOf(it.total.buildChildProcessesCpu.max) },
            "Max",
            "Build child processes cpu",
            measurement,
            "percentage"
        )

        extracted(
            variantA.flatMap { listOf(it.total.buildChildProcessesMemory.max) },
            variantB.flatMap { listOf(it.total.buildChildProcessesMemory.max) },
            "Max",
            "Build child processes memory",
            measurement,
            "bytes"
        )

        return measurement.toList()
    }

    private fun extracted(
        variantAValues: List<Long>,
        variantBValues: List<Long>,
        name: String,
        category: String,
        measurements: MutableList<MeasurementWithPercentiles>,
        type: String
    ) {
        val variantAMedian = if (type == "bytes") {
            "${
            bytesToGigabytes(
                variantAValues.median()
            )
            }"
        } else {
            "${variantAValues.median()}"
        }
        val variantBMedian = if (type == "bytes") {
            "${
            bytesToGigabytes(
                variantBValues.median()
            )
            }"
        } else {
            "${variantBValues.median()}"
        }
        val variantAMean = if (type == "bytes") {
            "${
            bytesToGigabytes(
                variantAValues.average()
            )
            }"
        } else {
            "${variantAValues.average()}"
        }
        val variantBMean = if (type == "bytes") {
            "${
            bytesToGigabytes(
                variantBValues.average()
            )
            }"
        } else {
            "${variantBValues.average()}"
        }
        val variantAP90 = if (type == "bytes") {
            "${
            bytesToGigabytes(
                variantAValues.percentile(90.0)
            )
            }"
        } else {
            "${variantAValues.percentile(90.0).roundToLong()}"
        }
        val variantBP90 = if (type == "bytes") {
            "${
            bytesToGigabytes(
                variantBValues.percentile(90.0)
            )
            }"
        } else {
            "${variantBValues.percentile(90.0).roundToLong()}"
        }

        val unit = if (type == "bytes") "Gb" else "%"

        measurements.add(
            MeasurementWithPercentiles(
                name = name,
                variantAMean = variantAMean,
                variantBMean = variantBMean,
                category = category,
                variantAP50 = variantAMedian,
                variantBP50 = variantBMedian,
                variantAP90 = variantAP90,
                variantBP90 = variantBP90,
                qualifier = unit,
                metric = Metric.RESOURCE_USAGE
            )
        )
    }

    private fun bytesToGigabytes(bytes: Double): Double {
        return String.format("%.2f", bytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
    }
}
