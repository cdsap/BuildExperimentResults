package io.github.cdsap.compare.report.measurements

import com.google.gson.Gson
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader

class FilterBuildsPerVariantsTest {
    @Test
    fun buildsAreParsedByVariant() {
        val variants =
            FilterBuildsPerVariant(
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                isProfile = false,
                warmupsToDiscard = 2,
            ).get(builds())
        assertTrue(variants.size == 2)
    }

    @Test
    fun profiledWarmupsAreDiscarded() {
        val variants =
            FilterBuildsPerVariant(
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                isProfile = true,
                warmupsToDiscard = 2,
            ).get(builds())
        assertTrue(variants.size == 2)
        assertTrue(variants.firstNotNullOf { it.value }.size == 3)
    }

    @Test
    fun differentExperimentIdReturnsEmptyVariants() {
        val variants =
            FilterBuildsPerVariant(
                variants = listOf("lint-4-1-different-process", "lint-2-1-different-process"),
                isProfile = true,
                warmupsToDiscard = 2,
            ).get(builds())
        assertTrue(variants.size == 2)
    }

    private fun builds(): List<BuildWithResourceUsage> {
        val builds: List<BuildWithResourceUsage> =
            Gson()
                .fromJson(
                    BufferedReader(InputStreamReader(javaClass.classLoader.getResourceAsStream("outcome.json"))).readText(),
                    Array<BuildWithResourceUsage>::class.java,
                ).toList()
        return builds
    }
}
