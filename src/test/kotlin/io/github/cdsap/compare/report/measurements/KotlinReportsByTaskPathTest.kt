package io.github.cdsap.compare.report.measurements

import io.github.cdsap.compare.report.measurements.parser.KotlinBuildReportsParserCustomValues
import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.CustomValue
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KotlinReportsByTaskPathTest {
    private val BuildWithResourceUsageProvider = BuildWithResourceUsageProvider()

    @Test
    fun buildsWithKotlinBuildReportsReturnAggregatedData() {
        val buildA = listOf(
            BuildWithResourceUsage(
                id = "1",
                builtTool = "A",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                values = arrayOf(
                    CustomValue(
                        ":task1:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [Shrink current classpath snapshot non-incrementally: 10ms,Connect to Kotlin daemon: 10ms]"
                    ),
                    CustomValue(
                        ":task2:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [Shrink current classpath snapshot non-incrementally: 100ms,Connect to Kotlin daemon: 100ms]"
                    )
                ),
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )
        val buildB = listOf(
            BuildWithResourceUsage(
                id = "2",
                builtTool = "A",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                values = arrayOf(
                    CustomValue(
                        ":task1:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [Shrink current classpath snapshot non-incrementally: 50ms,Connect to Kotlin daemon: 50ms]"
                    ),
                    CustomValue(
                        ":task2:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [Shrink current classpath snapshot non-incrementally: 500ms,Connect to Kotlin daemon: 500ms]"
                    )
                ),
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )
        val kotlinReportsParserCustomValues =
            KotlinBuildReportsParserCustomValues(buildA, buildB).parse()

        val measurements = KotlinReportsByTaskPath(kotlinReportsParserCustomValues).get(emptyList())

        assertTrue(measurements.size == 4)
        assertTrue(measurements.any { it.category == ":task2:kaptGenerateStubsDemoReleaseKotlin" && it.name == "Shrink current classpath snapshot non-incrementally" })
        assertTrue(measurements.any { it.category == ":task1:kaptGenerateStubsDemoReleaseKotlin" && it.name == "Shrink current classpath snapshot non-incrementally" })
        assertTrue(measurements[0].variantAP90 == "10")
        assertTrue(measurements[0].variantBP90 == "50")
    }

    @Test
    fun buildsWithKotlinBuildReportsWithOutQualifiersReturnAggregatedData() {
        val buildA = listOf(
            BuildWithResourceUsage(
                id = "1",
                builtTool = "A",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                values = arrayOf(
                    CustomValue(
                        ":task1:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [lines analyzed: 10]"
                    ),
                    CustomValue(
                        ":task2:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [lines analyzed: 10]"
                    )
                ),
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )
        val buildB = listOf(
            BuildWithResourceUsage(
                id = "2",
                builtTool = "A",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                values = arrayOf(
                    CustomValue(
                        ":task1:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [lines analyzed: 10]"
                    ),
                    CustomValue(
                        ":task2:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [lines analyzed: 10]"
                    )
                ),
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )
        val kotlinReportsParserCustomValues =
            KotlinBuildReportsParserCustomValues(buildA, buildB).parse()

        val measurements = KotlinReportsByTaskPath(kotlinReportsParserCustomValues).get(emptyList())

        assertTrue(measurements.size == 2)
        assertTrue(measurements.any { it.category == ":task1:kaptGenerateStubsDemoReleaseKotlin" && it.name == "lines analyzed" })
        assertTrue(measurements[0].variantAP90 == "10")
        assertTrue(measurements[0].variantBP90 == "10")
    }

    @Test
    fun whenIncludingExclusionMetricReturnsEmpty() {
        val buildA = listOf(
            BuildWithResourceUsage(
                id = "1",
                builtTool = "A",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                values = arrayOf(
                    CustomValue(
                        ":task1:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [Worker submit time: 10]"
                    ),
                    CustomValue(
                        ":task2:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [Worker submit time: 10]"
                    )
                ),
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )
        val buildB = listOf(
            BuildWithResourceUsage(
                id = "2",
                builtTool = "A",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                values = arrayOf(
                    CustomValue(
                        ":task1:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [Worker submit time: 10]"
                    ),
                    CustomValue(
                        ":task2:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [Worker submit time: 10]"
                    )
                ),
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )
        val kotlinReportsParserCustomValues =
            KotlinBuildReportsParserCustomValues(buildA, buildB).parse()

        val measurements = KotlinReportsByTaskPath(kotlinReportsParserCustomValues).get(listOf("Worker submit time"))

        assertTrue(measurements.isEmpty())
    }

    @Test
    fun whenBuildsDontIncludeSameMetricsReturnsEmpty() {
        val buildA = listOf(
            BuildWithResourceUsage(
                id = "1",
                builtTool = "A",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                values = arrayOf(
                    CustomValue(
                        ":task1:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [first: 10]"
                    ),
                    CustomValue(
                        ":task2:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [first: 10]"
                    )
                ),
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )
        val buildB = listOf(
            BuildWithResourceUsage(
                id = "2",
                builtTool = "A",
                taskExecution = arrayOf(
                    Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                    Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
                ),
                goalExecution = emptyArray(),
                avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
                values = arrayOf(
                    CustomValue(
                        ":task1:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [another_mer: 10]"
                    ),
                    CustomValue(
                        ":task2:kaptGenerateStubsDemoReleaseKotlin",
                        "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [another_mer: 10]"
                    )
                ),
                execution = BuildWithResourceUsageProvider.get(),
                nonExecution = BuildWithResourceUsageProvider.get(),
                total = BuildWithResourceUsageProvider.get(),
                totalMemory = 0L
            )
        )
        val kotlinReportsParserCustomValues =
            KotlinBuildReportsParserCustomValues(buildA, buildB).parse()

        val measurements = KotlinReportsByTaskPath(kotlinReportsParserCustomValues).get(emptyList())

        assertTrue(measurements.isEmpty())
    }
}
