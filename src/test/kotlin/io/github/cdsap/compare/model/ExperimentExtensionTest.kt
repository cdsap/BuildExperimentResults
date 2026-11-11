package io.github.cdsap.compare.model

import io.github.cdsap.compare.output.removeExperimentId as removeOutputExperimentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExperimentExtensionTest {
    @Test
    fun leavesVariantUnchangedWhenExperimentIdIsNull() {
        assertEquals("experiment_variant", "experiment_variant".removeExperimentId(null))
    }

    @Test
    fun leavesVariantUnchangedWhenExperimentIdIsAbsent() {
        assertEquals("other_variant", "other_variant".removeExperimentId("experiment"))
        assertEquals("experiment", "experiment".removeExperimentId("experiment"))
    }

    @Test
    fun removesEveryMatchingOccurrence() {
        assertEquals("variant_middle_end", "experiment_variant_experiment_middle_experiment_end".removeExperimentId("experiment"))
    }

    @Test
    fun outputPackageExtensionRemainsCompatible() {
        assertEquals("variant", "experiment_variant".removeOutputExperimentId("experiment"))
    }
}
