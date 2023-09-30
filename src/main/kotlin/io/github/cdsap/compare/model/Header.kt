package io.github.cdsap.compare.model

data class Header(
    val task: String,
    val numberOfBuildsForExperimentA: Int,
    val numberOfBuildsForExperimentB: Int,
    val experiment: String
)
