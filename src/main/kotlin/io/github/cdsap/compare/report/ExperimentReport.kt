package io.github.cdsap.compare.report

import io.github.cdsap.compare.export.ExperimentJsonWriter
import io.github.cdsap.compare.export.ExperimentResultAssembler
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurementAugmenter
import io.github.cdsap.compare.report.diagnostics.DiagnosticsPackageLoader
import io.github.cdsap.compare.report.diagnostics.DiagnosticsPackageSource
import io.github.cdsap.compare.report.measurements.FilterBuildsPerVariant
import io.github.cdsap.compare.report.measurements.MeasurementsByReport
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Filter
import java.nio.file.Path

class ExperimentReport(
    private val filter: Filter,
    private val buildDataProvider: BuildDataProvider,
    private val report: Report,
    private val output: ExperimentOutput,
    private val diagnosticsMeasurementsSource: DiagnosticsPackageSource = DiagnosticsPackageLoader(),
) {
    suspend fun process() {
        if (report.variants.isEmpty()) {
            throw IllegalArgumentException("At least one variant is required")
        } else {
            val buildWithResourceUsage = buildDataProvider.getBuilds(filter)
            val variants = checkVariantsData(FilterBuildsPerVariant(report).get(buildWithResourceUsage))
            val measurements = MeasurementsByReport(report).get(variants)
            val diagnosticsAugmentation =
                DiagnosticsMeasurementAugmenter(diagnosticsMeasurementsSource).augmentWithDiagnostics(
                    measurements = measurements,
                    diagnosticsPackagePath = report.diagnosticsPackagePath,
                    reportVariants = report.variants,
                )
            val augmentedMeasurements = diagnosticsAugmentation.measurements
            if (augmentedMeasurements.isNotEmpty()) {
                val outputJson = report.outputJson
                if (outputJson != null) {
                    val result =
                        ExperimentResultAssembler().assemble(
                            report = report,
                            measurements = augmentedMeasurements,
                            buildsByVariant = variants,
                            diagnostics = diagnosticsAugmentation.diagnostics,
                        )
                    ExperimentJsonWriter().write(result, Path.of(outputJson))
                    println("generating json $outputJson")
                }
                output.generateOutputs(augmentedMeasurements, variants)
            }
        }
    }

    private fun checkVariantsData(buildMap: Map<String, List<BuildWithResourceUsage>>): Map<String, List<BuildWithResourceUsage>> {
        val keysWithEmptyLists = buildMap.filterValues { it.isEmpty() }.keys

        if (keysWithEmptyLists.size == buildMap.size) {
            throw IllegalArgumentException("All variants have empty lists. Please check your input data.")
        }

        return buildMap.filterValues { it.isNotEmpty() }
    }
}
