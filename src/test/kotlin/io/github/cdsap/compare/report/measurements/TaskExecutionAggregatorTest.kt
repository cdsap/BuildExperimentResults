package io.github.cdsap.compare.report.measurements

import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskExecutionAggregatorTest {
    private val buildWithResourceUsageProvider = BuildWithResourceUsageProvider()

    @Test
    fun `aggregate by task path includes all outcomes when onlyCacheableOutcome is false`() {
        val builds = listOf(buildWithMixedOutcomes())

        val aggregated =
            TaskExecutionAggregator(onlyCacheableOutcome = false).aggregate(builds) { it.taskPath }

        assertEquals(setOf(":app:compileDebugKotlin", ":app:assembleDebug"), aggregated.keys)
        assertEquals(listOf(1500L), aggregated[":app:compileDebugKotlin"])
        assertEquals(listOf(800L), aggregated[":app:assembleDebug"])
    }

    @Test
    fun `aggregate by task path keeps only executed_cacheable when onlyCacheableOutcome is true`() {
        val builds = listOf(buildWithMixedOutcomes())

        val aggregated =
            TaskExecutionAggregator(onlyCacheableOutcome = true).aggregate(builds) { it.taskPath }

        assertEquals(setOf(":app:compileDebugKotlin"), aggregated.keys)
        assertEquals(listOf(1500L), aggregated[":app:compileDebugKotlin"])
        assertFalse(aggregated.containsKey(":app:assembleDebug"))
    }

    @Test
    fun `aggregate by task type groups durations across builds`() {
        val builds =
            listOf(
                buildWithTasks(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20),
                ),
                buildWithTasks(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1600, 10),
                ),
            )

        val aggregated =
            TaskExecutionAggregator(onlyCacheableOutcome = false).aggregate(builds) { it.taskType }

        assertEquals(1, aggregated.size)
        assertTrue(aggregated.containsKey("compile"))
        assertEquals(listOf(1500L, 2000L, 1600L), aggregated["compile"])
    }

    private fun buildWithMixedOutcomes(): BuildWithResourceUsage =
        buildWithTasks(
            Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
            Task("assemble", ":app:assembleDebug", "executed_not_cacheable", 800, 20),
        )

    private fun buildWithTasks(vararg tasks: Task): BuildWithResourceUsage =
        BuildWithResourceUsage(
            builtTool = "A",
            taskExecution = arrayOf(*tasks),
            goalExecution = emptyArray(),
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
            execution = buildWithResourceUsageProvider.get(),
            nonExecution = buildWithResourceUsageProvider.get(),
            total = buildWithResourceUsageProvider.get(),
            totalMemory = 0L,
        )
}
