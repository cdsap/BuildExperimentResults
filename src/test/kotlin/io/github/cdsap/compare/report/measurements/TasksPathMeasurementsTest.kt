package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TasksPathMeasurementsTest {
    private val buildWithResourceUsageProvider = BuildWithResourceUsageProvider()

    @Test
    fun `test getTaskPathMeasurements`() {
        val variantA =
            listOf(
                BuildWithResourceUsage(
                    builtTool = "A",
                    taskExecution =
                        arrayOf(
                            Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                            Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20),
                        ),
                    goalExecution = emptyArray(),
                    avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                    execution = buildWithResourceUsageProvider.get(),
                    nonExecution = buildWithResourceUsageProvider.get(),
                    total = buildWithResourceUsageProvider.get(),
                    totalMemory = 0L,
                ),
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
                thresholdTaskDuration = -1,
                gcReport = false,
            )

        val tasksPathMeasurements = TasksPathMeasurements(variantA, report)
        val measurements = tasksPathMeasurements.get()

        assertEquals(2, measurements.size)
        assertEquals("Task Path", measurements[0].category)
        assertEquals(":app:compileDebugKotlin", measurements[0].name)
        assertTrue(measurements[0].variantMean.toString().startsWith("1500"))
        assertTrue(measurements[0].variantP50.toString().startsWith("1500"))
        assertTrue(measurements[0].variantP90.toString().startsWith("1500"))
        assertEquals("ms", measurements[0].qualifier)
    }
}
