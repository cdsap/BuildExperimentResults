package io.github.cdsap.compare.report.diagnostics

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.SingleMeasurement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DiagnosticsMeasurementAugmenterTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun mergesLoadedRowsWithoutRunningReportGeneration() {
        val packageFile = File(tempDir, "diagnostics.json")
        packageFile.writeText(
            """
            {
              "schemaVersion": "1.0",
              "warnings": ["partial diagnostics"],
              "variants": [{
                "variant": "variant-a",
                "measurements": [{
                  "category": "cpu",
                  "name": "vmstat.us",
                  "mean": 10.0,
                  "p50": 9.0,
                  "p90": 12.0,
                  "qualifier": "system",
                  "metric": "CPU_DIAGNOSTICS"
                }]
              }]
            }
            """.trimIndent(),
        )
        val base = mapOf("variant-a" to listOf(baseMeasurement()))

        val result =
            DiagnosticsMeasurementAugmenter(DiagnosticsPackageLoader()).augment(
                measurements = base,
                diagnosticsPackagePath = packageFile.path,
                reportVariants = listOf("variant-a"),
            )

        assertEquals(2, result.getValue("variant-a").size)
        assertEquals(Metric.CPU_DIAGNOSTICS, result.getValue("variant-a")[1].metric)
    }

    @Test
    fun returnsBaseMeasurementsWhenPackageIsNotConfigured() {
        val base = mapOf("variant-a" to listOf(baseMeasurement()))

        val result = DiagnosticsMeasurementAugmenter().augment(base, null, listOf("variant-a"))

        assertEquals(base, result)
    }

    private fun baseMeasurement() =
        SingleMeasurement(
            category = "build",
            name = "duration",
            variantMean = 1.0,
            variantP50 = 1.0,
            variantP90 = 1.0,
            qualifier = "",
            metric = Metric.BUILD,
        )
}
