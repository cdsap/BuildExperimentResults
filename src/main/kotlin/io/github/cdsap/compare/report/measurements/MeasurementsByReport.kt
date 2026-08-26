package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class MeasurementsByReport(
    private val report: Report,
) {
    fun get(variants: Map<String, List<BuildWithResourceUsage>>): Map<String, List<SingleMeasurement>> {
        val enabledProviders = measurementProviders.filter { it.enabled(report) }
        val measurementsPerVariant = mutableMapOf<String, List<SingleMeasurement>>()
        variants.forEach { (variant, builds) ->
            measurementsPerVariant[variant] = enabledProviders.flatMap { it.measure(builds, report) }
        }

        return measurementsPerVariant
    }

    private data class MeasurementProvider(
        val enabled: (Report) -> Boolean,
        val measure: (List<BuildWithResourceUsage>, Report) -> List<SingleMeasurement>,
    )

    private companion object {
        private val measurementProviders =
            listOf(
                MeasurementProvider(
                    enabled = { it.buildReport },
                    measure = { builds, _ -> BuildMeasurement(builds).get() },
                ),
                MeasurementProvider(
                    enabled = { it.processesReport },
                    measure = { builds, report -> ProcessMeasurement(builds, report.isProfile).get() },
                ),
                MeasurementProvider(
                    enabled = { it.taskTypeReport },
                    measure = { builds, report -> TasksTypeMeasurements(builds, report).get() },
                ),
                MeasurementProvider(
                    enabled = { it.taskPathReport },
                    measure = { builds, report -> TasksPathMeasurements(builds, report).get() },
                ),
                MeasurementProvider(
                    enabled = { it.kotlinBuildReport },
                    measure = { builds, _ -> KotlinBuildReportsMeasurements(builds).get() },
                ),
                MeasurementProvider(
                    enabled = { it.resourceUsageReport },
                    measure = { builds, _ -> ResourceUsageMeasurement(builds).get() },
                ),
                MeasurementProvider(
                    enabled = { it.gcReport },
                    measure = { builds, report -> GCReportMeasurement(builds, report.isProfile).get() },
                ),
            )
    }
}
