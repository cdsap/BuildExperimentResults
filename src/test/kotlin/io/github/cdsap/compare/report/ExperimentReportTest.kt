package io.github.cdsap.compare.report

import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.ClientType
import io.github.cdsap.geapi.client.model.Filter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExperimentReportTest {
    @Test
    fun processThrowsWhenVariantsAreEmptyWithoutGeClient() {
        val fakeProvider =
            object : BuildProvider {
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
                        buildProvider = fakeProvider,
                        report = report(variants = emptyList()),
                    ).process()
                }
            }

        assertEquals("At least one variant is required", exception.message)
    }

    @Test
    fun processUsesFakeBuildProviderWithoutGeClientOrRepository() {
        var providerInvoked = false
        val fakeProvider =
            object : BuildProvider {
                override suspend fun get(
                    filter: Filter,
                    report: Report,
                ): List<BuildWithResourceUsage> {
                    providerInvoked = true
                    return emptyList()
                }
            }

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    ExperimentReport(
                        filter = filter(),
                        buildProvider = fakeProvider,
                        report = report(variants = listOf("variant-a")),
                    ).process()
                }
            }

        assertTrue(providerInvoked)
        assertEquals("All variants have empty lists. Please check your input data.", exception.message)
    }

    private fun filter() =
        Filter(
            maxBuilds = 10,
            tags = listOf("variant-a"),
            exclusiveTags = false,
            clientType = ClientType.CLI,
        )

    private fun report(variants: List<String>) =
        Report(
            taskPathReport = true,
            taskTypeReport = true,
            kotlinBuildReport = false,
            processesReport = false,
            buildReport = true,
            resourceUsageReport = false,
            isProfile = false,
            gcReport = false,
            warmupsToDiscard = 0,
            variants = variants,
            onlyCacheableOutcome = false,
            thresholdTaskDuration = -1,
        )
}
