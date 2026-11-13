package io.github.cdsap.compare.report.measurements

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinBuildReportMetricsTest {
    @Test
    fun preservesSupportedMetricsAndTheirOrder() {
        assertEquals(
            listOf(
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
                "Sources compilation round",
            ),
            KotlinBuildReportMetrics.ALLOWED_LIST,
        )
    }
}
