package io.github.cdsap.compare.report

import io.github.cdsap.compare.export.CanonicalJsonExporter
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.report.diagnostics.DiagnosticsMeasurementAugmenter
import io.github.cdsap.compare.report.diagnostics.DiagnosticsPackageLoader
import io.github.cdsap.compare.report.diagnostics.DiagnosticsPackageSource
import io.github.cdsap.compare.report.measurements.FilterBuildsPerVariant
import io.github.cdsap.compare.report.measurements.MeasurementsByReport
import io.github.cdsap.geapi.client.model.Filter

class ExperimentReport(
    private val filter: Filter,
    private val buildDataProvider: BuildDataProvider,
    private val report: Report,
    private val output: ExperimentOutput,
    private val diagnosticsMeasurementsSource: DiagnosticsPackageSource = DiagnosticsPackageLoader(),
    private val canonicalJsonExporter: CanonicalJsonExporter,
    private val diagnosticsMeasurementAugmenter: DiagnosticsMeasurementAugmenter =
        DiagnosticsMeasurementAugmenter(diagnosticsMeasurementsSource),
    private val variantBuildsValidator: VariantBuildsValidator = VariantBuildsValidator(),
) {
    suspend fun process() {
        if (report.variants.isEmpty()) {
            throw IllegalArgumentException("At least one variant is required")
        } else {
            val buildWithResourceUsage = buildDataProvider.getBuilds(filter)
            val variants =
                variantBuildsValidator.validate(
                    FilterBuildsPerVariant(report.variants, report.isProfile, report.warmupsToDiscard).get(buildWithResourceUsage),
                )
            val measurements = MeasurementsByReport(report).get(variants)
            val diagnosticsAugmentation =
                diagnosticsMeasurementAugmenter.augmentWithDiagnostics(
                    measurements = measurements,
                    diagnosticsPackagePath = report.diagnosticsPackagePath,
                    reportVariants = report.variants,
                )
            val augmentedMeasurements = diagnosticsAugmentation.measurements
            if (augmentedMeasurements.isNotEmpty()) {
                val outputJson = report.outputJson
                if (outputJson != null) {
                    canonicalJsonExporter.export(
                        report = report,
                        measurements = augmentedMeasurements,
                        buildsByVariant = variants,
                        diagnostics = diagnosticsAugmentation.diagnostics,
                        outputJson = outputJson,
                    )
                }
                output.generateOutputs(augmentedMeasurements, variants)
            }
        }
    }

}
