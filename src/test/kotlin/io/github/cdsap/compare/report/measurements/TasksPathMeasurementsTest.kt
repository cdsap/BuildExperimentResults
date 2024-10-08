package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TasksPathMeasurementsTest {
    private val BuildWithResourceUsageProvider = BuildWithResourceUsageProvider()

    @Test
    fun `test getTaskPathMeasurements`() {
        val variantA = listOf(
            BuildWithResourceUsage(
                builtTool = "A",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )

        val variantB = listOf(
            BuildWithResourceUsage(
                builtTool = "B",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 3000, 15),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 4000, 25)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )
        val report =
            Report(
                taskPathReport = true,
                taskTypeReport = true,
                kotlinBuildReport = true,
                processesReport = false,
                buildReport = true,
                isProfile = false,
                resourceUsageReport = false,
                warmupsToDiscard = 2,
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                experimentId = "154",
                onlyCacheableOutcome = false,
                thresholdTaskDuration = -1
            )

        val tasksPathMeasurements = TasksPathMeasurements(variantA, variantB, report)
        val measurements = tasksPathMeasurements.get()

        assertEquals(2, measurements.size)
        assertEquals("Task Path", measurements[0].category)
        assertEquals(":app:compileDebugKotlin", measurements[0].name)
        assertTrue(measurements[0].variantAMean.toString().startsWith("1500"))
        assertTrue(measurements[0].variantBMean.toString().startsWith("3000"))
        assertTrue(measurements[0].variantAP50.toString().startsWith("1500"))
        assertTrue(measurements[0].variantBP50.toString().startsWith("3000"))
        assertTrue(measurements[0].variantAP90.toString().startsWith("1500"))
        assertTrue(measurements[0].variantBP90.toString().startsWith("3000"))
        assertEquals("ms", measurements[0].qualifier)
    }
}
