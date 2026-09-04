package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.report.measurements.BuildWithResourceUsageProvider
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.ClientType
import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.model.Task
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ExperimentReportTest {
    private val metrics = BuildWithResourceUsageProvider()

    @Test
    fun processThrowsWhenVariantsAreEmptyWithoutGeClient() {
        val fakeLoader =
            object : BuildLoader {
                override suspend fun get(
                    filter: Filter,
                    report: Report,
                ): List<BuildWithResourceUsage> = emptyList()
            }

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    ExperimentReport(
                        filter = filter(),
                        buildLoader = fakeLoader,
                        report = report(variants = emptyList()),
                    ).process()
                }
            }

        assertEquals("At least one variant is required", exception.message)
    }

    @Test
    fun processThrowsWhenAllVariantResultsAreEmpty() {
        var loaderInvoked = false
        val fakeLoader =
            object : BuildLoader {
                override suspend fun get(
                    filter: Filter,
                    report: Report,
                ): List<BuildWithResourceUsage> {
                    loaderInvoked = true
                    return emptyList()
                }
            }

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    ExperimentReport(
                        filter = filter(),
                        buildLoader = fakeLoader,
                        report = report(variants = listOf("variant-a")),
                    ).process()
                }
            }

        assertTrue(loaderInvoked)
        assertEquals("All variants have empty lists. Please check your input data.", exception.message)
    }

    @Test
    fun processContinuesWithNonEmptyVariantsUsingFakeBuildLoader() {
        var loaderInvoked = false
        val fakeLoader =
            object : BuildLoader {
                override suspend fun get(
                    filter: Filter,
                    report: Report,
                ): List<BuildWithResourceUsage> {
                    loaderInvoked = true
                    return listOf(
                        usageBuild(id = "build-1", tags = arrayOf("variant-a"), buildDuration = 10L),
                        usageBuild(id = "build-2", tags = arrayOf("variant-a"), buildDuration = 20L),
                        usageBuild(id = "build-3", tags = arrayOf("variant-a"), buildDuration = 30L),
                        usageBuild(id = "build-ignored", tags = arrayOf("other"), buildDuration = 40L),
                    )
                }
            }

        val before =
            File(".")
                .listFiles()
                ?.filter { it.name.startsWith("experiment_results") }
                ?.map { it.absolutePath }
                ?.toSet()
                .orEmpty()

        try {
            runBlocking {
                ExperimentReport(
                    filter = filter(),
                    buildLoader = fakeLoader,
                    report =
                        report(
                            variants = listOf("variant-a", "variant-empty"),
                            buildReport = true,
                            taskPathReport = false,
                            taskTypeReport = false,
                        ),
                ).process()
            }
        } finally {
            File(".")
                .listFiles()
                ?.filter { it.name.startsWith("experiment_results") && it.absolutePath !in before }
                ?.forEach { it.delete() }
        }

        assertTrue(loaderInvoked)
    }

    private fun filter() =
        Filter(
            maxBuilds = 10,
            tags = listOf("variant-a"),
            exclusiveTags = false,
            clientType = ClientType.CLI,
        )

    private fun report(
        variants: List<String>,
        buildReport: Boolean = true,
        taskPathReport: Boolean = true,
        taskTypeReport: Boolean = true,
    ) = Report(
        taskPathReport = taskPathReport,
        taskTypeReport = taskTypeReport,
        kotlinBuildReport = false,
        processesReport = false,
        buildReport = buildReport,
        resourceUsageReport = false,
        isProfile = false,
        gcReport = false,
        warmupsToDiscard = 0,
        variants = variants,
        onlyCacheableOutcome = false,
        thresholdTaskDuration = -1,
    )

    private fun usageBuild(
        id: String,
        tags: Array<String>,
        buildDuration: Long,
    ): BuildWithResourceUsage =
        BuildWithResourceUsage(
            id = id,
            builtTool = "gradle",
            taskExecution =
                arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                ),
            goalExecution = emptyArray(),
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
            tags = tags,
            requestedTask = arrayOf("assemble"),
            buildDuration = buildDuration,
            buildStartTime = 1L,
            projectName = "demo",
            configuration = 5L,
            execution = metrics.get(),
            nonExecution = metrics.get(),
            total = metrics.get(),
            totalMemory = 0L,
        )
}
