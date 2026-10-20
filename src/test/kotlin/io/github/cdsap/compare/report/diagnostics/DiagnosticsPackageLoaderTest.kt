package io.github.cdsap.compare.report.diagnostics

import io.github.cdsap.compare.model.Metric
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DiagnosticsPackageLoaderTest {
    private val loader = DiagnosticsPackageLoader()

    @TempDir
    lateinit var tempDir: File

    @Test
    fun remapsExactAndSuffixVariants() {
        assertEquals("lint-4-1", loader.remapVariant("lint-4-1", listOf("lint-4-1")))
        assertEquals(
            "owner-1_varianta_lint-4-1",
            loader.remapVariant("lint-4-1", listOf("owner-1_varianta_lint-4-1", "owner-1_variantb_other")),
        )
    }

    @Test
    fun loadsAndMapsMeasurements() {
        val file = File(tempDir, "pkg.json")
        file.writeText(
            """
            {
              "schemaVersion": "1.0",
              "variants": [{
                "variant": "lint-4-1",
                "measurements": [
                  {"category":"cpu","name":"vmstat.us","mean":10.0,"p50":9.0,"p90":12.0,"qualifier":"system","metric":"CPU_DIAGNOSTICS"},
                  {"category":"jfr","name":"recording.size_bytes","mean":100.0,"p50":100.0,"p90":100.0,"qualifier":"gradle","metric":"JFR"}
                ]
              }]
            }
            """.trimIndent(),
        )

        val diagnostics = loader.load(file.path, listOf("owner-1_varianta_lint-4-1"))
        assertTrue(diagnostics.warnings.isEmpty())
        val rows = diagnostics.measurements.getValue("owner-1_varianta_lint-4-1")
        assertEquals(2, rows.size)
        assertEquals(Metric.CPU_DIAGNOSTICS, rows[0].metric)
        assertEquals(Metric.JFR, rows[1].metric)
        assertEquals(10.0, rows[0].variantMean)
    }

    @Test
    fun rejectsUnknownMajorSchema() {
        val file = File(tempDir, "bad.json")
        file.writeText("""{"schemaVersion":"2.0","variants":[]}""")
        assertThrows(IllegalArgumentException::class.java) { loader.load(file) }
    }

    @Test
    fun missingPackageIsSoftFailureWhenLoadedThroughSource() {
        val result = loader.load(File(tempDir, "missing.json").path, listOf("variant-a"))

        assertTrue(result.measurements.isEmpty())
        assertEquals(
            "Diagnostics package not found: ${File(tempDir, "missing.json").path}; continuing without diagnostics",
            result.warnings.single(),
        )
    }

    @Test
    fun warnsAndSkipsAmbiguousVariantMatches() {
        val result =
            loader.toMeasurements(
                DiagnosticsPackageDto(
                    schemaVersion = "1.0",
                    variants =
                        listOf(
                            VariantDiagnosticsDto(
                                variant = "raw",
                                measurements =
                                    listOf(
                                        PackageMeasurementDto(metric = "JFR", name = "duration"),
                                    ),
                            ),
                        ),
                ),
                listOf("varianta_raw", "variantb_raw"),
            )

        assertTrue(result.first.isEmpty())
        assertTrue(result.second.single().contains("no unique match"))
    }
}
