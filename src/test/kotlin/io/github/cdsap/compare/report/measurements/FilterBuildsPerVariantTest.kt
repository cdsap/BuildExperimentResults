package io.github.cdsap.compare.report.measurements

import com.google.gson.Gson
import io.github.cdsap.compare.model.Report
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader

class FilterBuildsPerVariantTest {

    @Test
    fun buildsAreParsedByVariant() {
        val report =
            Report(
                taskPathReport = true,
                taskTypeReport = true,
                kotlinBuildReport = true,
                processesReport = true,
                buildReport = true,
                resourceUsageReport = false,
                isProfile = false,
                warmupsToDiscard = 2,
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                experimentId = "154"
            )
        val variants = FilterBuildsPerVariant(report).get(builds())
        assertTrue(variants.variantA.size == 5)
        assertTrue(variants.variantB.size == 5)
    }

    @Test
    fun profiledWarmupsAreDiscarded() {
        val report =
            Report(
                taskPathReport = true,
                taskTypeReport = true,
                kotlinBuildReport = true,
                processesReport = true,
                buildReport = true,
                resourceUsageReport = false,
                isProfile = true,
                warmupsToDiscard = 2,
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                experimentId = "154"
            )
        val variants = FilterBuildsPerVariant(report).get(builds())
        assertTrue(variants.variantA.size == 3)
        assertTrue(variants.variantB.size == 3)
    }

    @Test
    fun differentExperimentIdReturnsEmptyVariants() {
        val report =
            Report(
                taskPathReport = true,
                taskTypeReport = true,
                kotlinBuildReport = true,
                processesReport = true,
                buildReport = true,
                resourceUsageReport = false,
                isProfile = true,
                warmupsToDiscard = 2,
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                experimentId = "987"
            )
        val variants = FilterBuildsPerVariant(report).get(builds())
        assertTrue(variants.variantA.isEmpty())
        assertTrue(variants.variantB.isEmpty())
    }

    private fun builds(): List<BuildWithResourceUsage> {
        val builds: List<BuildWithResourceUsage> =
            Gson().fromJson(
                BufferedReader(InputStreamReader(javaClass.classLoader.getResourceAsStream("outcome.json"))).readText(),
                Array<BuildWithResourceUsage>::class.java
            ).toList()
        return builds
    }
}
