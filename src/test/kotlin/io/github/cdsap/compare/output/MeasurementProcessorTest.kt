package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.SingleMeasurement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MeasurementProcessorTest {
    private val measurementProcessor = MeasurementProcessor()

    @Test
    fun `test processing measurements with multiple variants`() {
        val measurements = mapOf(
            "main" to listOf(
                SingleMeasurement("BUILD", "DURATION", 100.0, 100.0, 90.0, "ms", Metric.BUILD),
                SingleMeasurement(
                    "RESOURCE_USAGE",
                    "PROCESS_MEMORY",
                    900.0,
                    1000.0,
                    900.0,
                    "Gb",
                    Metric.RESOURCE_USAGE
                )
            ),
            "feature" to listOf(
                SingleMeasurement("BUILD", "DURATION", 100.0, 95.0, 85.0, "ms", Metric.BUILD),
                SingleMeasurement(
                    "RESOURCE_USAGE",
                    "PROCESS_MEMORY",
                    900.0,
                    950.0,
                    850.0,
                    "Gb",
                    Metric.RESOURCE_USAGE
                )
            )
        )

        val result = measurementProcessor.processMeasurements(measurements)

        assertEquals(2, result.size)

        // Verify BUILD DURATION measurements
        val durationRow = result.find { it["name"] == "DURATION" }
        assertNotNull(durationRow)
        assertEquals("BUILD", durationRow!!["category"])
        assertEquals("ms", durationRow["main-unit"])
        assertEquals(100.0, durationRow["main-mean"])
        assertEquals(100.0, durationRow["main-median"])
        assertEquals(90.0, durationRow["main-p90"])
        assertEquals(100.0, durationRow["feature-mean"])
        assertEquals(95.0, durationRow["feature-median"])
        assertEquals(85.0, durationRow["feature-p90"])

        // Verify RESOURCE_USAGE PROCESS_MEMORY measurements
        val memoryRow = result.find { it["name"] == "PROCESS_MEMORY" }
        assertNotNull(memoryRow)
        assertEquals("RESOURCE_USAGE", memoryRow!!["category"])
        assertEquals("Gb", memoryRow["main-unit"])
        assertEquals(900.0, memoryRow["main-mean"])
        assertEquals(1000.0, memoryRow["main-median"])
        assertEquals(900.0, memoryRow["main-p90"])
        assertEquals(900.0, memoryRow["feature-mean"])
        assertEquals(950.0, memoryRow["feature-median"])
        assertEquals(850.0, memoryRow["feature-p90"])
    }

    @Test
    fun `test processing measurements with single variant`() {
        val measurements = mapOf(
            "main" to listOf(
                SingleMeasurement("BUILD", "DURATION", 100.0, 100.0, 90.0, "ms", Metric.BUILD)
            )
        )

        val result = measurementProcessor.processMeasurements(measurements)

        assertEquals(1, result.size)
        val row = result.first()
        assertEquals("BUILD", row["category"])
        assertEquals("DURATION", row["name"])
        assertEquals("ms", row["main-unit"])
        assertEquals(100.0, row["main-mean"])
        assertEquals(100.0, row["main-median"])
        assertEquals(90.0, row["main-p90"])
    }

    @Test
    fun `test filtering unwanted metrics`() {
        val measurements = mapOf(
            "main" to listOf(
                SingleMeasurement("BUILD", "DURATION", "ms", 100.0, 90.0, "ms", Metric.BUILD),
                SingleMeasurement("BUILD", "TASK_PATH", "path", 0.0, 0.0, "ms", Metric.TASK_PATH),
                SingleMeasurement("BUILD", "KOTLIN_BUILD_REPORT", "report", 0.0, 0.0, "ms", Metric.KOTLIN_BUILD_REPORT),
                SingleMeasurement(
                    "BUILD",
                    "TASK_KOTLIN_BUILD_REPORT",
                    "report",
                    0.0,
                    0.0,
                    "ms",
                    Metric.TASK_KOTLIN_BUILD_REPORT
                )
            )
        )

        val result = measurementProcessor.processMeasurements(measurements)
            .filter { measurementProcessor.filterUnwantedMetrics(it) }

        assertEquals(1, result.size)
        assertEquals("DURATION", result.first()["name"])
    }

    @Test
    fun `test processing empty measurements`() {
        val measurements = mapOf<String, List<SingleMeasurement>>()
        val result = measurementProcessor.processMeasurements(measurements)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test processing measurements with missing values`() {
        val measurements = mapOf(
            "main" to listOf(
                SingleMeasurement("BUILD", "DURATION", "ms", 100.0, 90.0, "ms", Metric.BUILD)
            ),
            "feature" to listOf(
                SingleMeasurement("BUILD", "PROCESS_MEMORY", "bytes", 1000.0, 900.0, "ms", Metric.BUILD)
            )
        )

        val result = measurementProcessor.processMeasurements(measurements)

        assertEquals(2, result.size)
        val durationRow = result.find { it["name"] == "DURATION" }!!
        assertNull(durationRow["feature-mean"])
        assertNull(durationRow["feature-median"])
        assertNull(durationRow["feature-p90"])

        val memoryRow = result.find { it["name"] == "PROCESS_MEMORY" }!!
        assertNull(memoryRow["main-mean"])
        assertNull(memoryRow["main-median"])
        assertNull(memoryRow["main-p90"])
    }
}
