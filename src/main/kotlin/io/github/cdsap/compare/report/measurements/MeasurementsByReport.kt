package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.compare.report.MeasurementsCalculator
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

interface MeasurementProvider {
    fun enabled(report: Report): Boolean

    fun measure(
        builds: List<BuildWithResourceUsage>,
        report: Report,
    ): List<SingleMeasurement>
}

class MeasurementsByReport(
    private val report: Report,
    private val measurementProviders: List<MeasurementProvider> = defaultMeasurementProviders,
) : MeasurementsCalculator {
    override fun get(variants: Map<String, List<BuildWithResourceUsage>>): Map<String, List<SingleMeasurement>> {
        val enabledProviders = measurementProviders.filter { it.enabled(report) }
        val measurementsPerVariant = mutableMapOf<String, List<SingleMeasurement>>()
        variants.forEach { (variant, builds) ->
            measurementsPerVariant[variant] = enabledProviders.flatMap { it.measure(builds, report) }
        }

        return measurementsPerVariant
    }

    private data class DefaultMeasurementProvider(
        val enabledWhen: (Report) -> Boolean,
        val measureWith: (List<BuildWithResourceUsage>, Report) -> List<SingleMeasurement>,
    ) : MeasurementProvider {
        override fun enabled(report: Report) = enabledWhen(report)

        override fun measure(
            builds: List<BuildWithResourceUsage>,
            report: Report,
        ) = measureWith(builds, report)
    }

    private companion object {
        private val defaultMeasurementProviders =
            listOf(
                DefaultMeasurementProvider(
                    enabledWhen = { it.buildReport },
                    measureWith = { builds, _ -> BuildMeasurement(builds).get() },
                ),
                DefaultMeasurementProvider(
                    enabledWhen = { it.processesReport },
                    measureWith = { builds, report -> ProcessMeasurement(builds, report.isProfile).get() },
                ),
                DefaultMeasurementProvider(
                    enabledWhen = { it.taskTypeReport },
                    measureWith = { builds, report -> TasksTypeMeasurements(builds, report).get() },
                ),
                DefaultMeasurementProvider(
                    enabledWhen = { it.taskPathReport },
                    measureWith = { builds, report -> TasksPathMeasurements(builds, report).get() },
                ),
                DefaultMeasurementProvider(
                    enabledWhen = { it.kotlinBuildReport },
                    measureWith = { builds, _ -> KotlinBuildReportsMeasurements(builds).get() },
                ),
                DefaultMeasurementProvider(
                    enabledWhen = { it.resourceUsageReport },
                    measureWith = { builds, _ -> ResourceUsageMeasurement(builds).get() },
                ),
                DefaultMeasurementProvider(
                    enabledWhen = { it.gcReport },
                    measureWith = { builds, report -> GCReportMeasurement(builds, report.isProfile).get() },
                ),
            )
    }
}
