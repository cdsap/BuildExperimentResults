package io.github.cdsap.compare.report.measurements

import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.Build
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TasksPathMeasurementsTest {

    @Test
    fun `test getTaskPathMeasurements`() {

        val variantA = listOf(
            Build(
                builtTool = "A",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("","","")
            )
        )

        val variantB = listOf(
            Build(
                builtTool = "B",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 3000, 15),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 4000, 25)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("","","")
            )
        )

        val tasksPathMeasurements = TasksPathMeasurements(variantA, variantB)
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
