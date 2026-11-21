package io.github.cdsap.compare.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VariantLabelTest {
    @Test
    fun leavesVariantUnchangedWhenExperimentIdIsNull() {
        assertEquals("experiment_variant", "experiment_variant".removeExperimentId(null))
    }

    @Test
    fun removesEveryUnderscoreWhenExperimentIdIsEmpty() {
        assertEquals("experimentvariant", "_experiment__variant_".removeExperimentId(""))
    }

    @Test
    fun leavesVariantUnchangedWhenExperimentIdIsAbsent() {
        assertEquals("other_variant", "other_variant".removeExperimentId("experiment"))
        assertEquals("experiment", "experiment".removeExperimentId("experiment"))
    }

    @Test
    fun removesLeadingExperimentIdToken() {
        assertEquals("variant", "experiment_variant".removeExperimentId("experiment"))
    }

    @Test
    fun removesEmbeddedExperimentIdToken() {
        assertEquals("variant_suffix", "variant_experiment_suffix".removeExperimentId("experiment"))
    }

    @Test
    fun removesEveryMatchingOccurrence() {
        assertEquals("variant_middle_end", "experiment_variant_experiment_middle_experiment_end".removeExperimentId("experiment"))
    }
}
