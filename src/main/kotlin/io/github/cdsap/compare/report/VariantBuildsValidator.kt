package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class VariantBuildsValidator {
    fun validate(buildMap: Map<String, List<BuildWithResourceUsage>>): Map<String, List<BuildWithResourceUsage>> {
        val keysWithEmptyLists = buildMap.filterValues { it.isEmpty() }.keys

        if (keysWithEmptyLists.size == buildMap.size) {
            throw IllegalArgumentException("All variants have empty lists. Please check your input data.")
        }

        return buildMap.filterValues { it.isNotEmpty() }
    }
}
