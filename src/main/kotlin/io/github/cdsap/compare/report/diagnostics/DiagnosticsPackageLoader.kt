package io.github.cdsap.compare.report.diagnostics

import com.google.gson.Gson
import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.SingleMeasurement
import java.io.File

class DiagnosticsPackageLoader(
    private val gson: Gson = Gson(),
) : DiagnosticsPackageSource {
    override fun load(
        diagnosticsPackagePath: String?,
        reportVariants: List<String>,
    ): DiagnosticsMeasurements {
        if (diagnosticsPackagePath == null) return DiagnosticsMeasurements()

        val path = File(diagnosticsPackagePath)
        if (!path.isFile) {
            return DiagnosticsMeasurements(
                warnings = listOf("Diagnostics package not found: ${path.path}; continuing without diagnostics"),
                status = "unavailable",
            )
        }
        val dto =
            try {
                load(path)
            } catch (exception: IllegalArgumentException) {
                // Schema-version errors are deliberate compatibility failures and must remain visible.
                throw exception
            } catch (exception: Exception) {
                return DiagnosticsMeasurements(
                    warnings =
                        listOf(
                            "Unable to parse diagnostics package ${path.path}: " +
                                "${exception.message ?: exception::class.simpleName}; continuing without diagnostics",
                        ),
                    status = "unavailable",
                )
            }
        val (measurements, warnings) = toMeasurements(dto, reportVariants)
        val jobKeysByVariant = linkedMapOf<String, List<String>>()
        var unmatched = 0
        for (variantBlock in dto.variants) {
            val mapped = remapVariant(variantBlock.variant, reportVariants)
            if (mapped == null) {
                unmatched++
                continue
            }
            jobKeysByVariant[mapped] = variantBlock.jobKeys
        }
        val status =
            when {
                unmatched > 0 -> "partial"
                else -> "present"
            }
        return DiagnosticsMeasurements(
            measurements = measurements,
            warnings = warnings,
            status = status,
            sourceSchemaVersion = dto.schemaVersion,
            sourceExperimentRunId = dto.experimentRunId,
            sourceRepo = dto.sourceRepo,
            jobKeysByVariant = jobKeysByVariant,
        )
    }

    fun load(path: File): DiagnosticsPackageDto {
        require(path.isFile) { "Diagnostics package not found: ${path.path}" }
        val dto = gson.fromJson(path.readText(), DiagnosticsPackageDto::class.java)
        val major = dto.schemaVersion.substringBefore('.').toIntOrNull()
        if (major == null || major != 1) {
            throw IllegalArgumentException(
                "Unsupported diagnostics package schemaVersion '${dto.schemaVersion}' (expected major 1)",
            )
        }
        return dto
    }

    /** Remaps package variants onto BER `--variants` tags and converts rows to measurements. */
    fun toMeasurements(
        dto: DiagnosticsPackageDto,
        reportVariants: List<String>,
    ): Pair<Map<String, List<SingleMeasurement>>, List<String>> {
        val warnings = mutableListOf<String>()
        warnings += dto.warnings
        val result = mutableMapOf<String, MutableList<SingleMeasurement>>()

        for (variantBlock in dto.variants) {
            val mapped = remapVariant(variantBlock.variant, reportVariants)
            if (mapped == null) {
                warnings +=
                    "Skipping diagnostics variant '${variantBlock.variant}': " +
                        "no unique match in --variants $reportVariants"
                continue
            }
            val list = result.getOrPut(mapped) { mutableListOf() }
            for (row in variantBlock.measurements) {
                val metric = parseMetric(row.metric)
                if (metric == null) {
                    warnings += "Skipping unknown metric kind '${row.metric}' for ${row.name}"
                    continue
                }
                list +=
                    SingleMeasurement(
                        category = row.category,
                        name = row.name,
                        variantMean = row.mean,
                        variantP50 = row.p50,
                        variantP90 = row.p90,
                        qualifier = row.qualifier,
                        metric = metric,
                    )
            }
        }
        return result.mapValues { it.value.toList() } to warnings.distinct()
    }

    internal fun remapVariant(raw: String, reportVariants: List<String>): String? {
        if (raw in reportVariants) return raw
        val suffixMatches = reportVariants.filter { tag ->
            tag.endsWith("varianta_$raw") || tag.endsWith("variantb_$raw") || tag.endsWith("_$raw")
        }
        return suffixMatches.singleOrNull()
    }

    private fun parseMetric(value: String): Metric? =
        when (value.uppercase()) {
            "CPU_DIAGNOSTICS" -> Metric.CPU_DIAGNOSTICS
            "JFR" -> Metric.JFR
            else -> runCatching { Metric.valueOf(value) }.getOrNull()
        }
}
