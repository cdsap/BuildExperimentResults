package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.Task
import io.github.cdsap.compare.report.measurements.BuildWithResourceUsageProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class VariantBuildsValidatorTest {
    private val validator = VariantBuildsValidator()
    private val metrics = BuildWithResourceUsageProvider()

    @Test
    fun throwsWhenAllVariantsHaveEmptyLists() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                validator.validate(
                    mapOf(
                        "variant-a" to emptyList(),
                        "variant-b" to emptyList(),
                    ),
                )
            }

        assertEquals("All variants have empty lists. Please check your input data.", exception.message)
    }

    @Test
    fun returnsOnlyVariantsWithBuildsWhenSomeListsAreEmpty() {
        val build = build()

        assertEquals(
            mapOf("variant-a" to listOf(build)),
            validator.validate(
                mapOf(
                    "variant-a" to listOf(build),
                    "variant-empty" to emptyList(),
                ),
            ),
        )
    }

    @Test
    fun returnsAllVariantsWhenEveryListHasBuilds() {
        val build = build()
        val builds =
            mapOf(
                "variant-a" to listOf(build),
                "variant-b" to listOf(build),
            )

        assertEquals(builds, validator.validate(builds))
    }

    private fun build() =
        BuildWithResourceUsage(
            id = "build-1",
            builtTool = "gradle",
            taskExecution = arrayOf(Task("compile", ":app:compileKotlin", "executed_cacheable", 1500, 10)),
            goalExecution = emptyArray(),
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
            tags = arrayOf("variant-a"),
            requestedTask = arrayOf("assemble"),
            buildDuration = 10L,
            buildStartTime = 1L,
            projectName = "demo",
            configuration = 5L,
            execution = metrics.get(),
            nonExecution = metrics.get(),
            total = metrics.get(),
            totalMemory = 0L,
        )
}
