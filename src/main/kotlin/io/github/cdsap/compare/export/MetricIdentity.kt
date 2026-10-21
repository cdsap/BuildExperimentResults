package io.github.cdsap.compare.export

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.compare.model.SingleMeasurement

object MetricIdentity {
    fun family(metric: Metric): String =
        when (metric) {
            Metric.BUILD -> "build"
            Metric.TASK_PATH, Metric.TASK_TYPE -> "task"
            Metric.PROCESS -> "process"
            Metric.KOTLIN_BUILD_REPORT -> "kotlin"
            Metric.TASK_KOTLIN_BUILD_REPORT -> "kotlin-task"
            Metric.RESOURCE_USAGE -> "resource"
            Metric.GC_REPORT -> "gc"
            Metric.CPU_DIAGNOSTICS -> "cpu-diagnostics"
            Metric.JFR -> "jfr"
        }

    fun sampleUnit(metric: Metric): String =
        when (metric) {
            Metric.BUILD, Metric.RESOURCE_USAGE, Metric.PROCESS, Metric.KOTLIN_BUILD_REPORT, Metric.GC_REPORT -> "build"
            Metric.TASK_PATH, Metric.TASK_TYPE, Metric.TASK_KOTLIN_BUILD_REPORT -> "task-invocation"
            Metric.CPU_DIAGNOSTICS, Metric.JFR -> "diagnostic-sample"
        }

    fun valueType(unit: String, metric: Metric): String {
        val normalized = unit.lowercase()
        return when {
            metric == Metric.BUILD || normalized == "ms" || normalized == "minutes" -> "duration"
            normalized == "bytes" || normalized == "gb" || normalized == "mb" -> "memory"
            normalized == "%" || normalized == "percentage" -> "percent"
            metric == Metric.GC_REPORT -> "count"
            metric == Metric.CPU_DIAGNOSTICS || metric == Metric.JFR -> "quantity"
            else -> "quantity"
        }
    }

    fun direction(valueType: String): String? = if (valueType == "duration") "lower-is-better" else null

    fun id(
        measurement: SingleMeasurement,
        used: MutableSet<String>,
    ): String {
        val special =
            when {
                measurement.metric == Metric.BUILD && measurement.name.equals("Build time", ignoreCase = true) ->
                    "build.duration"
                measurement.metric == Metric.BUILD &&
                    measurement.name.equals("Configuration time", ignoreCase = true) ->
                    "build.configuration"
                else -> null
            }
        if (special != null) {
            used += special
            return special
        }
        val parts =
            listOfNotNull(
                family(measurement.metric),
                slug(measurement.category).takeIf { it.isNotBlank() && it != slug(measurement.name) },
                slug(measurement.name),
                slug(measurement.qualifier).takeIf { it.isNotBlank() },
            ).filter { it.isNotBlank() }
        var candidate = parts.joinToString(".").ifBlank { "metric.unknown" }
        if (candidate in used) {
            var suffix = 2
            while ("$candidate-$suffix" in used) {
                suffix++
            }
            candidate = "$candidate-$suffix"
        }
        used += candidate
        return candidate
    }

    fun slug(value: String): String {
        val lowered = value.lowercase()
        val replaced = lowered.replace(Regex("[^a-z0-9._-]+"), "-")
        return replaced.trim('-', '.', '_')
    }

    fun sanitizeId(value: String, fallback: String): String {
        val slug = slug(value)
        return if (slug.isBlank()) fallback else slug
    }
}
