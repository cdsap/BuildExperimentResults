package io.github.cdsap.compare.model

data class Header(
    val task: String,
    val numberOfBuilds: List<Int>,
    val experimentId: String? = null,
    val repository: String? = null,
    val url: String? = null,
    val linkCsv: String = "",
    val experimentRunId: String? = null,
    val htmlSummary: Boolean = false,
)
