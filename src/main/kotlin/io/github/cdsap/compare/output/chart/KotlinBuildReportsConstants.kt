package io.github.cdsap.compare.output.chart

object KotlinBuildReportsConstants {
    val ALLOWED_LIST = listOf(
        "Run compilation",
        "Run compilation in Gradle worker",
        "Compiler code analysis",
        "Compiler code generation",
        "Compiler initialization time",
        "Compiler IR generation",
        "Compiler IR translation",
        "Compiler IR lowering",
        "Spent time before task action",
        "Total Gradle task time",
        "Task action before worker execution",
        "Connect to Kotlin daemon",
        "Incremental compilation in daemon",
        "Sources compilation round"
    )
}
