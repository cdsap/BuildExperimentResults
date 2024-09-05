package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.model.Metric
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Test

class BuildMeasurementTest {
    private val BuildWithResourceUsageProvider = BuildWithResourceUsageProvider()

    @Test
    fun testPercentilesAreCalculatedCorrectly() {
        val tasks = arrayOf(
            Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
            Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
        )
        val avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", "")
        val buildsA = listOf(
            BuildWithResourceUsage(
                builtTool = "A",
                taskExecution = tasks,
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = avoidanceSavingsSummary,
                buildDuration = 10,
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            ),
            BuildWithResourceUsage(
                builtTool = "A",
                taskExecution = tasks,
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = avoidanceSavingsSummary,
                buildDuration = 20,
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            ),
            BuildWithResourceUsage(
                builtTool = "A",
                taskExecution = tasks,
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = avoidanceSavingsSummary,
                buildDuration = 30,
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            ),
            BuildWithResourceUsage(
                builtTool = "A",
                taskExecution = tasks,
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = avoidanceSavingsSummary,
                buildDuration = 40,
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            ),
            BuildWithResourceUsage(
                builtTool = "A",
                taskExecution = tasks,
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = avoidanceSavingsSummary,
                buildDuration = 50,
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )

        val buildsB = listOf(
            BuildWithResourceUsage(
                builtTool = "A",
                taskExecution = tasks,
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = avoidanceSavingsSummary,
                buildDuration = 1000,
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            ),
            BuildWithResourceUsage(
                builtTool = "A",
                taskExecution = tasks,
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = avoidanceSavingsSummary,
                buildDuration = 1000,
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            ),
            BuildWithResourceUsage(
                builtTool = "A",
                taskExecution = tasks,
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = avoidanceSavingsSummary,
                buildDuration = 1000,
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            ),
            BuildWithResourceUsage(
                builtTool = "A",
                taskExecution = tasks,
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = avoidanceSavingsSummary,
                buildDuration = 1000,
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            ),
            BuildWithResourceUsage(
                builtTool = "A",
                taskExecution = tasks,
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = avoidanceSavingsSummary,
                buildDuration = 1000,
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )

        val measurements = BuildMeasurement(buildsA, buildsB).get()

        assert(measurements.size == 1)
        assert(measurements[0].variantBP50 == "1000")
        assert(measurements[0].variantBP90 == "1000")
        assert(measurements[0].variantBMean == "1000")
        assert(measurements[0].variantAP50 == "30")
        assert(measurements[0].variantAP90 == "50")
        assert(measurements[0].variantAMean == "30")
        assert(measurements[0].category == "Build")
        assert(measurements[0].name == "Build time")
        assert(measurements[0].metric == Metric.BUILD)
    }

    @Test
    fun whenVariantBIsEmptyNoMetricsAreProvided() {
        @Test
        fun testPercentilesAreCalculatedCorrectly() {
            val tasks = arrayOf(
                Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
            )
            val avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", "")
            val buildsA = listOf(
                BuildWithResourceUsage(
                    builtTool = "A",
                    taskExecution = tasks,
                    goalExecution = emptyArray(),
                    avoidanceSavingsSummary = avoidanceSavingsSummary,
                    buildDuration = 10,
                    execution = BuildWithResourceUsageProvider.get(),
                    nonExecution = BuildWithResourceUsageProvider.get(),
                    total = BuildWithResourceUsageProvider.get(),
                    totalMemory = 0L
                ),
                BuildWithResourceUsage(
                    builtTool = "A",
                    taskExecution = tasks,
                    goalExecution = emptyArray(),
                    avoidanceSavingsSummary = avoidanceSavingsSummary,
                    buildDuration = 20,
                    execution = BuildWithResourceUsageProvider.get(),
                    nonExecution = BuildWithResourceUsageProvider.get(),
                    total = BuildWithResourceUsageProvider.get(),
                    totalMemory = 0L
                ),
                BuildWithResourceUsage(
                    builtTool = "A",
                    taskExecution = tasks,
                    goalExecution = emptyArray(),
                    avoidanceSavingsSummary = avoidanceSavingsSummary,
                    buildDuration = 30,
                    execution = BuildWithResourceUsageProvider.get(),
                    nonExecution = BuildWithResourceUsageProvider.get(),
                    total = BuildWithResourceUsageProvider.get(),
                    totalMemory = 0L
                ),
                BuildWithResourceUsage(
                    builtTool = "A",
                    taskExecution = tasks,
                    goalExecution = emptyArray(),
                    avoidanceSavingsSummary = avoidanceSavingsSummary,
                    buildDuration = 40,
                    execution = BuildWithResourceUsageProvider.get(),
                    nonExecution = BuildWithResourceUsageProvider.get(),
                    total = BuildWithResourceUsageProvider.get(),
                    totalMemory = 0L
                ),
                BuildWithResourceUsage(
                    builtTool = "A",
                    taskExecution = tasks,
                    goalExecution = emptyArray(),
                    avoidanceSavingsSummary = avoidanceSavingsSummary,
                    buildDuration = 50,
                    execution = BuildWithResourceUsageProvider.get(),
                    nonExecution = BuildWithResourceUsageProvider.get(),
                    total = BuildWithResourceUsageProvider.get(),
                    totalMemory = 0L
                )
            )

            val buildsB = emptyList<BuildWithResourceUsage>()

            val measurements = BuildMeasurement(buildsA, buildsB).get()

            assert(measurements.isEmpty())
        }
    }
}
