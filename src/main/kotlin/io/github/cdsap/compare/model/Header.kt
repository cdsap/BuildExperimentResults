package io.github.cdsap.compare.model

data class Header(
    val task: String,
    val numberOfBuilds: List<Int>,
    val experiment: String
)
