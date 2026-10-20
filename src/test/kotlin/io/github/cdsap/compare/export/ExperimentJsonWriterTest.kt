package io.github.cdsap.compare.export

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ExperimentJsonWriterTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun writesPrettyJsonWithFiniteNumbers() {
        val path = tempDir.resolve("experiment-result.json")
        ExperimentJsonWriter().write(validResult(), path)

        val json = JsonParser.parseString(path.toFile().readText()).asJsonObject
        assertEquals("1.0.0", json.get("schemaVersion").asString)
        assertTrue(json.getAsJsonArray("metrics")[0].asJsonObject.getAsJsonObject("values")
            .getAsJsonObject("baseline").get("mean").isJsonPrimitive)
        assertTrue(json.getAsJsonArray("metrics")[0].asJsonObject.getAsJsonObject("values")
            .getAsJsonObject("baseline").get("mean").asJsonPrimitive.isNumber)
        assertTrue(json.getAsJsonObject("diagnostics").get("source").isJsonNull)
    }

    @Test
    fun rejectsNonFiniteStatisticsBeforeWriting() {
        val path = tempDir.resolve("broken.json")
        val result =
            validResult().copy(
                metrics =
                    listOf(
                        validResult().metrics.single().copy(
                            values = mapOf("baseline" to StatisticDto(Double.NaN, 1.0, 1.0, 1)),
                        ),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            ExperimentJsonWriter().write(result, path)
        }
        assertTrue(!File(path.toUri()).exists())
    }

    private fun validResult() =
        ExperimentResultDto(
            schemaVersion = EXPERIMENT_RESULT_SCHEMA_VERSION,
            generatedAt = "2026-10-20T21:00:00Z",
            producer = ProducerDto(PRODUCER_NAME, PRODUCER_VERSION, "sha"),
            report = ReportIdentityDto("run-1-report-attempt-1", "report", null, "complete"),
            experiment = ExperimentIdentityDto("exp", "cdsap/Telltale", listOf("assemble"), null, false),
            execution = ExecutionDto("local", null, null, null, null, emptyMap()),
            variants = listOf(VariantDto("baseline", "variant-a", "variant-a", "baseline", 1)),
            metrics =
                listOf(
                    MetricDto(
                        id = "build.duration",
                        family = "build",
                        category = "Build",
                        name = "Build time",
                        unit = "ms",
                        valueType = "duration",
                        direction = "lower-is-better",
                        scope = null,
                        aggregation = AggregationDto("build", "existing-ber-method"),
                        values = mapOf("baseline" to StatisticDto(10.0, 10.0, 10.0, 1)),
                    ),
                ),
            datasets =
                DatasetsDto(
                    builds = emptyMap(),
                    taskSeries = emptyMap(),
                    resourceSeries = emptyMap(),
                    processSeries = emptyMap(),
                    gcSeries = emptyMap(),
                    kotlinBuildReportSeries = emptyMap(),
                ),
            diagnostics = DiagnosticsDto("not-requested", null, emptyList()),
            warnings = emptyList(),
            completeness = CompletenessDto(listOf("baseline"), listOf("baseline"), emptyList(), "not-requested"),
        )
}
