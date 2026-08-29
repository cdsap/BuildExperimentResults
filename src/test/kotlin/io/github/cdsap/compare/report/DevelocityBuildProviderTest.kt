package io.github.cdsap.compare.report

import io.github.cdsap.compare.report.measurements.BuildWithResourceUsageProvider
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.Breakdown
import io.github.cdsap.geapi.client.model.Build
import io.github.cdsap.geapi.client.model.BuildProfileOverview
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.CustomValue
import io.github.cdsap.geapi.client.model.MemoryUsage
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DevelocityBuildProviderTest {
    private val metrics = BuildWithResourceUsageProvider()

    @Test
    fun enrichCopiesOutcomeAndProfileFieldsForMatchingBuildIds() {
        val tasks =
            arrayOf(
                Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
            )
        val values = arrayOf(CustomValue("key", "value"))
        val outcome =
            listOf(
                Build(
                    id = "build-1",
                    builtTool = "gradle",
                    taskExecution = tasks,
                    tags = arrayOf("variant-a"),
                    requestedTask = arrayOf("assemble"),
                    buildDuration = 1234L,
                    buildStartTime = 99L,
                    projectName = "demo",
                    avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                    goalExecution = emptyArray(),
                    values = values,
                ),
            )
        val usage =
            listOf(
                usageBuild(id = "build-1"),
                usageBuild(id = "build-unmatched"),
            )
        val profiles =
            listOf(
                BuildProfileOverview(
                    id = "build-1",
                    breakdown = Breakdown(total = 10, initialization = 1, configuration = 42, execution = 5, endOfBuild = 2),
                    memoryUsage = MemoryUsage(totalGarbageCollectionTime = 77L, memoryPools = emptyArray()),
                ),
            )

        val enriched = DevelocityBuildProvider.enrich(outcome, usage, profiles, isProfile = false)

        assertEquals(2, enriched.size)
        val matched = enriched.first { it.id == "build-1" }
        assertEquals(listOf("assemble"), matched.requestedTask.toList())
        assertEquals(values.toList(), matched.values.toList())
        assertEquals(listOf("variant-a"), matched.tags.toList())
        assertEquals(1234L, matched.buildDuration)
        assertEquals(99L, matched.buildStartTime)
        assertEquals("gradle", matched.builtTool)
        assertEquals("demo", matched.projectName)
        assertEquals(tasks.toList(), matched.taskExecution.toList())
        assertEquals(42L, matched.configuration)
        assertEquals(77L, matched.totalGarbageCollectionTime)

        val unmatched = enriched.first { it.id == "build-unmatched" }
        assertEquals(0L, unmatched.configuration)
        assertEquals(0L, unmatched.totalGarbageCollectionTime)
        assertTrue(unmatched.requestedTask.isEmpty())
    }

    @Test
    fun enrichInProfileModeFiltersCleanOnlyBuilds() {
        val outcome =
            listOf(
                Build(
                    id = "clean-build",
                    builtTool = "gradle",
                    taskExecution = emptyArray(),
                    tags = emptyArray(),
                    requestedTask = arrayOf("clean"),
                    buildDuration = 10L,
                    buildStartTime = 1L,
                    projectName = "demo",
                    avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                    goalExecution = emptyArray(),
                    values = emptyArray(),
                ),
                Build(
                    id = "assemble-build",
                    builtTool = "gradle",
                    taskExecution = emptyArray(),
                    tags = emptyArray(),
                    requestedTask = arrayOf("assemble"),
                    buildDuration = 20L,
                    buildStartTime = 2L,
                    projectName = "demo",
                    avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                    goalExecution = emptyArray(),
                    values = emptyArray(),
                ),
                Build(
                    id = "clean-and-assemble",
                    builtTool = "gradle",
                    taskExecution = emptyArray(),
                    tags = emptyArray(),
                    requestedTask = arrayOf("clean", "assemble"),
                    buildDuration = 30L,
                    buildStartTime = 3L,
                    projectName = "demo",
                    avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                    goalExecution = emptyArray(),
                    values = emptyArray(),
                ),
            )
        val usage =
            listOf(
                usageBuild(id = "clean-build"),
                usageBuild(id = "assemble-build"),
                usageBuild(id = "clean-and-assemble"),
            )

        val profileResult = DevelocityBuildProvider.enrich(outcome, usage, emptyList(), isProfile = true)
        assertEquals(listOf("assemble-build", "clean-and-assemble"), profileResult.map { it.id })

        val nonProfileResult = DevelocityBuildProvider.enrich(outcome, usage, emptyList(), isProfile = false)
        assertEquals(listOf("clean-build", "assemble-build", "clean-and-assemble"), nonProfileResult.map { it.id })
    }

    private fun usageBuild(id: String): BuildWithResourceUsage =
        BuildWithResourceUsage(
            id = id,
            builtTool = "",
            taskExecution = emptyArray(),
            goalExecution = emptyArray(),
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
            execution = metrics.get(),
            nonExecution = metrics.get(),
            total = metrics.get(),
            totalMemory = 0L,
        )
}
