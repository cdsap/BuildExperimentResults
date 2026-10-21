package io.github.cdsap.compare.export

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.SingleMeasurement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MetricIdentityTest {
    @Test
    fun buildTimeAndConfigurationUseStableIds() {
        val used = mutableSetOf<String>()
        assertEquals("build.duration", MetricIdentity.id(measurement(Metric.BUILD, "Build", "Build time"), used))
        assertEquals(
            "build.configuration",
            MetricIdentity.id(measurement(Metric.BUILD, "Build", "Configuration time"), used),
        )
    }

    @Test
    fun collidingIdsAreDisambiguated() {
        val used = mutableSetOf<String>()
        val first = MetricIdentity.id(measurement(Metric.TASK_PATH, "Task Path", ":app:compile"), used)
        val second = MetricIdentity.id(measurement(Metric.TASK_PATH, "Task Path", ":app:compile"), used)
        assertEquals("task.task-path.app-compile.ms", first)
        assertEquals("task.task-path.app-compile.ms-2", second)
    }

    @Test
    fun diagnosticsKeepDistinctFamilies() {
        assertEquals("cpu-diagnostics", MetricIdentity.family(Metric.CPU_DIAGNOSTICS))
        assertEquals("jfr", MetricIdentity.family(Metric.JFR))
        assertEquals("diagnostic-sample", MetricIdentity.sampleUnit(Metric.CPU_DIAGNOSTICS))
        assertEquals("build", MetricIdentity.sampleUnit(Metric.BUILD))
    }

    private fun measurement(
        metric: Metric,
        category: String,
        name: String,
    ) = SingleMeasurement(
        category = category,
        name = name,
        variantMean = 1,
        variantP50 = 1,
        variantP90 = 1,
        qualifier = "ms",
        metric = metric,
    )
}
