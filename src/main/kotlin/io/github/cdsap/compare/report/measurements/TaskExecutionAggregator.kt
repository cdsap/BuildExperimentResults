package io.github.cdsap.compare.report.measurements

import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Task

class TaskExecutionAggregator(
    private val onlyCacheableOutcome: Boolean,
) {
    fun aggregate(
        builds: List<BuildWithResourceUsage>,
        keySelector: (Task) -> String,
    ): Map<String, MutableList<Long>> {
        val aggregated = mutableMapOf<String, MutableList<Long>>()
        builds.forEach { build ->
            val tasksExecution =
                if (onlyCacheableOutcome) {
                    build.taskExecution.filter { it.avoidanceOutcome == "executed_cacheable" }
                } else {
                    build.taskExecution.toList()
                }

            tasksExecution.forEach { task ->
                val key = keySelector(task)
                if (aggregated.contains(key)) {
                    aggregated[key]?.add(task.duration)
                } else {
                    aggregated[key] = mutableListOf()
                    aggregated[key]?.add(task.duration)
                }
            }
        }
        return aggregated
    }
}
