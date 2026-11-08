package io.github.cdsap.compare.export

import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurements
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiagnosticsExportMapperTest {
    private val mapper = DiagnosticsExportMapper()

    @Test
    fun exportsNotRequestedDiagnosticsAsInfoWarningWithoutSource() {
        val export = mapper.map(DiagnosticsMeasurements(), emptyList(), emptyMap())

        assertEquals("not-requested", export.diagnostics.status)
        assertNull(export.diagnostics.source)
        assertEquals("DIAGNOSTICS_NOT_REQUESTED", export.warnings.single().code)
        assertEquals("info", export.warnings.single().severity)
    }

    @Test
    fun classifiesMissingPackageAndSanitizesItsPath() {
        val export =
            mapper.map(
                DiagnosticsMeasurements(
                    status = "unavailable",
                    warnings =
                        listOf(
                            "Diagnostics package not found: /tmp/private/missing.json; continuing without diagnostics",
                        ),
                ),
                emptyList(),
                emptyMap(),
            )

        val warning = export.warnings.single()
        assertEquals("DIAGNOSTICS_PACKAGE_MISSING", warning.code)
        assertEquals("diagnostics", warning.source)
        assertTrue(warning.message.contains("missing.json"))
        assertTrue(!warning.message.contains("/tmp/private"))
    }

    @Test
    fun associatesDiagnosticWarningWithItsVariant() {
        val export =
            mapper.map(
                DiagnosticsMeasurements(warnings = listOf("no unique match for variant-a"), status = "present"),
                emptyList(),
                mapOf("variant-a" to "candidate"),
            )

        assertEquals("DIAGNOSTICS_VARIANT_UNMATCHED", export.warnings.single().code)
        assertEquals("candidate", export.warnings.single().variantId)
    }

    @Test
    fun createsCpuAndJfrGroupsAndFiltersUnknownJobVariants() {
        val export =
            mapper.map(
                DiagnosticsMeasurements(
                    status = "present",
                    jobKeysByVariant =
                        mapOf(
                            "variant-a" to listOf("cpu-job"),
                            "unknown-variant" to listOf("ignored-job"),
                        ),
                ),
                listOf(metric("cpu.metric", "cpu-diagnostics"), metric("jfr.metric", "jfr"), metric("build.metric", "build")),
                mapOf("variant-a" to "baseline"),
            )

        assertEquals(listOf("cpu", "jfr"), export.diagnostics.groups.map { it.id })
        assertEquals(listOf("cpu.metric"), export.diagnostics.groups[0].metricIds)
        assertEquals(listOf("jfr.metric"), export.diagnostics.groups[1].metricIds)
        assertEquals(mapOf("baseline" to listOf("cpu-job")), export.diagnostics.groups[0].jobKeysByVariant)
        assertEquals(mapOf("baseline" to listOf("cpu-job")), export.diagnostics.groups[1].jobKeysByVariant)
    }

    private fun metric(id: String, family: String) =
        MetricDto(
            id = id,
            family = family,
            category = "category",
            name = "name",
            unit = "unit",
            valueType = "number",
            direction = null,
            scope = null,
            aggregation = AggregationDto("diagnostic-sample", "existing-ber-method"),
            values = emptyMap(),
        )
}
