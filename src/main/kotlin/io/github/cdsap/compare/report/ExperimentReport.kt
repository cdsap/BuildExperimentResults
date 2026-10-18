package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.output.ExperimentView
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurementsSource
import io.github.cdsap.compare.report.diagnostics.FileDiagnosticsMeasurementsSource
import io.github.cdsap.compare.report.measurements.FilterBuildsPerVariant
import io.github.cdsap.compare.report.measurements.MeasurementsByReport
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Filter

class ExperimentReport(
    private val filter: Filter,
    private val buildDataProvider: BuildDataProvider,
    private val report: Report,
    private val diagnosticsMeasurementsSource: DiagnosticsMeasurementsSource = FileDiagnosticsMeasurementsSource(),
) {
    suspend fun process() {
        if (report.variants.isEmpty()) {
            throw IllegalArgumentException("At least one variant is required")
        } else {
            val buildWithResourceUsage = buildDataProvider.getBuilds(filter)
            val variants = checkVariantsData(FilterBuildsPerVariant(report).get(buildWithResourceUsage))
            val measurements = MeasurementsByReport(report).get(variants)
            val diagnostics = diagnosticsMeasurementsSource.load(report.diagnosticsPackagePath, report.variants)
            diagnostics.warnings.forEach { println("diagnostics warning: $it") }
            val augmentedMeasurements = measurements.toMutableMap().apply {
                for ((variant, rows) in diagnostics.measurements) {
                    this[variant] = get(variant).orEmpty() + rows
                }
            }
            if (augmentedMeasurements.isNotEmpty()) {
                ExperimentView(report).generateOutputs(augmentedMeasurements, variants)
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
