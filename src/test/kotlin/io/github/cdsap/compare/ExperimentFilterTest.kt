package io.github.cdsap.compare

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExperimentFilterTest {
    @Test
    fun registersFromInstantOption() {
        val names = Experiment().registeredOptions().flatMap { it.names }.toSet()
        assertTrue("--from-instant" in names)
    }

    @Test
    fun passesFromInstantOntoTheDevelocityFilter() {
        val filter =
            experimentFilter(
                maxBuilds = 200,
                project = null,
                variants = listOf("cdsap-249_varianta_2.2.21"),
                requestedTask = null,
                fromInstant = 1766178689570L,
            )

        assertEquals(1766178689570L, filter.fromInstant)
        assertEquals(listOf("cdsap-249_varianta_2.2.21"), filter.tags.toList())
    }

    @Test
    fun omitsFromInstantWhenUnset() {
        val filter =
            experimentFilter(
                maxBuilds = 200,
                project = null,
                variants = emptyList(),
                requestedTask = null,
                fromInstant = null,
            )

        assertNull(filter.fromInstant)
    }
}
