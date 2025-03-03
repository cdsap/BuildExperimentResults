package io.github.cdsap.compare.model

data class Report(
    val taskPathReport: Boolean,
    val taskTypeReport: Boolean,
    val kotlinBuildReport: Boolean,
    val processesReport: Boolean,
    val buildReport: Boolean,
    val resourceUsageReport: Boolean,
    val isProfile: Boolean,
    val warmupsToDiscard: Int,
    val variants: List<String>,
    val experimentId: String? = null,
    val onlyCacheableOutcome: Boolean,
    val thresholdTaskDuration: Long,
    val repository: String? = null,
    val url: String? = null,
    val experimentRunId: String? = null
)
