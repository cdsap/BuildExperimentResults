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

class BuildResourceUsageEnricherTest {
    private val metrics = BuildWithResourceUsageProvider()
    private val enricher = BuildResourceUsageEnricher()

    @Test
    fun enrichCopiesOutcomeAndProfileFieldsForMatchingBuildIds() {
        val tasks =
            arrayOf(
                Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
            )
        val values = arrayOf(CustomValue("key", "value"))
        val outcome =
            listOf(
                build(
                    id = "build-1",
                    builtTool = "gradle",
                    taskExecution = tasks,
                    tags = arrayOf("variant-a"),
                    requestedTask = arrayOf("assemble"),
                    buildDuration = 1234L,
                    buildStartTime = 99L,
                    projectName = "demo",
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
                profile(id = "build-1", configuration = 42, totalGarbageCollectionTime = 77L),
            )

        val enriched = enricher.enrich(outcome, usage, profiles, isProfile = false)

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
    fun enrichLeavesProfileFieldsUnsetWhenProfileIsMissing() {
        val tasks =
            arrayOf(
                Task("test", ":app:testDebugUnitTest", "executed", 2500, 3),
            )
        val values = arrayOf(CustomValue("profile", "missing"))
        val enriched =
            enricher.enrich(
                outcome =
                    listOf(
                        build(
                            id = "build-without-profile",
                            builtTool = "gradle",
                            taskExecution = tasks,
                            tags = arrayOf("missing-profile"),
                            requestedTask = arrayOf("test"),
                            buildDuration = 5678L,
                            buildStartTime = 101L,
                            projectName = "profile-demo",
                            values = values,
                        ),
                    ),
                buildWithResourceUsage = listOf(usageBuild(id = "build-without-profile")),
                buildProfile = emptyList(),
                isProfile = false,
            )

        val matched = enriched.single()
        assertEquals(listOf("test"), matched.requestedTask.toList())
        assertEquals(values.toList(), matched.values.toList())
        assertEquals(listOf("missing-profile"), matched.tags.toList())
        assertEquals(5678L, matched.buildDuration)
        assertEquals(101L, matched.buildStartTime)
        assertEquals("gradle", matched.builtTool)
        assertEquals("profile-demo", matched.projectName)
        assertEquals(tasks.toList(), matched.taskExecution.toList())
        assertEquals(0L, matched.configuration)
        assertEquals(0L, matched.totalGarbageCollectionTime)
    }

    @Test
    fun enrichInProfileModeFiltersCleanOnlyBuilds() {
        val outcome =
            listOf(
                build(id = "clean-build", requestedTask = arrayOf("clean")),
                build(id = "assemble-build", requestedTask = arrayOf("assemble")),
                build(id = "clean-and-assemble", requestedTask = arrayOf("clean", "assemble")),
            )
        val usage =
            listOf(
                usageBuild(id = "clean-build"),
                usageBuild(id = "assemble-build"),
                usageBuild(id = "clean-and-assemble"),
            )

        val profileResult = enricher.enrich(outcome, usage, emptyList(), isProfile = true)
        assertEquals(listOf("assemble-build", "clean-and-assemble"), profileResult.map { it.id })

        val nonProfileResult = enricher.enrich(outcome, usage, emptyList(), isProfile = false)
        assertEquals(listOf("clean-build", "assemble-build", "clean-and-assemble"), nonProfileResult.map { it.id })
    }

    private fun build(
        id: String,
        builtTool: String = "gradle",
        taskExecution: Array<Task> = emptyArray(),
        tags: Array<String> = emptyArray(),
        requestedTask: Array<String> = arrayOf("assemble"),
        buildDuration: Long = 10L,
        buildStartTime: Long = 1L,
        projectName: String = "demo",
        values: Array<CustomValue> = emptyArray(),
    ): Build =
        Build(
            id = id,
            builtTool = builtTool,
            taskExecution = taskExecution,
            tags = tags,
            requestedTask = requestedTask,
            buildDuration = buildDuration,
            buildStartTime = buildStartTime,
            projectName = projectName,
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
            goalExecution = emptyArray(),
            values = values,
        )

    private fun profile(
        id: String,
        configuration: Long,
        totalGarbageCollectionTime: Long,
    ): BuildProfileOverview =
        BuildProfileOverview(
            id = id,
            breakdown = Breakdown(total = 10, initialization = 1, configuration = configuration, execution = 5, endOfBuild = 2),
            memoryUsage = MemoryUsage(totalGarbageCollectionTime = totalGarbageCollectionTime, memoryPools = emptyArray()),
        )

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
