package io.github.cdsap.compare.report.measurements.parser

import io.github.cdsap.geapi.client.model.AvoidanceSavingsSummary
import io.github.cdsap.geapi.client.model.Build
import io.github.cdsap.geapi.client.model.CustomValue
import io.github.cdsap.geapi.client.model.Task
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KotlinBuildReportsParserCustomValuesTest {

    @Test
    fun testBuildsAreNotIncludingBuildReportsReturnsEmptyValues() {
        val build = Build(
            builtTool = "A",
            taskExecution = arrayOf(
                Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
            ),
            goalExecution = emptyArray(),
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
            values = arrayOf(CustomValue("no process", "k"))
        )

        val kotlinBuildReports = KotlinBuildReportsParserCustomValues(listOf(build), listOf(build)).parse()
        assertTrue(kotlinBuildReports.variantA.isEmpty())
        assertTrue(kotlinBuildReports.variantB.isEmpty())
    }

    @Test
    fun testBuildsWithoutCustomValuesReturnsEmptyValues() {
        val build = Build(
            builtTool = "A",
            taskExecution = arrayOf(
                Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
            ),
            goalExecution = emptyArray(),
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", "")
        )

        val kotlinBuildReports = KotlinBuildReportsParserCustomValues(listOf(build), listOf(build)).parse()
        assertTrue(kotlinBuildReports.variantA.isEmpty())
        assertTrue(kotlinBuildReports.variantB.isEmpty())
    }

    @Test
    fun testBuildsWithSingleKotlinBuildReportCustomValuesReturnsMetricKotlin() {
        val build = Build(
            id = "skkaow",
            builtTool = "A",
            taskExecution = arrayOf(
                Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
            ),
            goalExecution = emptyArray(),
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
            values = arrayOf(
                CustomValue(
                    ":ui-test-hilt-manifest:kaptGenerateStubsDemoReleaseKotlin",
                    "Non incremental build because: [Unknown Gradle changes]; Kotlin language version: 1.9; Performance: [Shrink current classpath snapshot non-incrementally: 52ms,Connect to Kotlin daemon: 9ms,Update caches: 29ms,Compiler code analysis: 1066ms,Compiler initialization time: 72ms,Incremental compilation in daemon: 1275ms,Sources compilation round: 1139ms,Start gradle worker: 5ms,Run compilation in Gradle worker: 1369ms,Shrink and save current classpath snapshot after compilation: 67ms,Spent time before task action: 293ms,Load current classpath snapshot: 9ms,Total Gradle task time: 1712ms,Run compilation: 1358ms,Calculate output size: 3ms,Task action before worker execution: 9ms,Total size of the cache directory: 288.3 KB,ABI snapshot size: 49 B,Increase memory usage: 184.8 MB,Total memory usage at the end of build: 835.8 MB,Total compiler iteration: 1,Number of lines analyzed: 27,Analysis lines per second: 25,Number of times classpath snapshot is shrunk and saved after compilation: 1,Number of classpath entries: 36,Size of classpath snapshot: 2.3 MB,Size of shrunk classpath snapshot: 6.2 KB,Number of times classpath snapshot is loaded: 1,Number of cache hits when loading classpath entry snapshots: 33,Number of cache misses when loading classpath entry snapshots: 3,Start time of task action: 1694473298557]"
                )
            )
        )

        val kotlinBuildReports = KotlinBuildReportsParserCustomValues(listOf(build), listOf(build)).parse()
        assertTrue(kotlinBuildReports.variantA.containsKey("skkaow"))
        assertTrue(kotlinBuildReports.variantA["skkaow"]?.containsKey(":ui-test-hilt-manifest:kaptGenerateStubsDemoReleaseKotlin")!!)
        assertTrue(kotlinBuildReports.variantA["skkaow"]!![":ui-test-hilt-manifest:kaptGenerateStubsDemoReleaseKotlin"]!!.size == 31)
        assertTrue(kotlinBuildReports.variantA["skkaow"]!![":ui-test-hilt-manifest:kaptGenerateStubsDemoReleaseKotlin"]!!.filter { it.desc == "Connect to Kotlin daemon" }
            .isNotEmpty())

    }

    @Test
    fun testBuildsWrongFormatReturnsEmptyMetricKotlin() {
        val build = Build(
            id = "skkaow",
            builtTool = "A",
            taskExecution = arrayOf(
                Task("compile", ":app:compileDebugKotlin", "executed_cacheable", 1500, 10),
                Task("compile", ":core:compileDebugKotlin", "executed_cacheable", 2000, 20)
            ),
            goalExecution = emptyArray(),
            avoidanceSavingsSummary = AvoidanceSavingsSummary("", "", ""),
            values = arrayOf(
                CustomValue(
                    ":ui-test-hilt-manifest:kaptGenerateStubsDemoReleaseKotlin",
                    "Non incremental build because: xUnknown Gradle changes]; Kotlin language version: 1.9; Performance: xrink current classpath snapshot non-incrementally: 52ms,Connect to Kotlin daemon: 9ms,Update caches: 29ms,Compiler code analysis: 1066ms,Compiler initialization time: 72ms,Incremental compilation in daemon: 1275ms,Sources compilation round: 1139ms,Start gradle worker: 5ms,Run compilation in Gradle worker: 1369ms,Shrink and save current classpath snapshot after compilation: 67ms,Spent time before task action: 293ms,Load current classpath snapshot: 9ms,Total Gradle task time: 1712ms,Run compilation: 1358ms,Calculate output size: 3ms,Task action before worker execution: 9ms,Total size of the cache directory: 288.3 KB,ABI snapshot size: 49 B,Increase memory usage: 184.8 MB,Total memory usage at the end of build: 835.8 MB,Total compiler iteration: 1,Number of lines analyzed: 27,Analysis lines per second: 25,Number of times classpath snapshot is shrunk and saved after compilation: 1,Number of classpath entries: 36,Size of classpath snapshot: 2.3 MB,Size of shrunk classpath snapshot: 6.2 KB,Number of times classpath snapshot is loaded: 1,Number of cache hits when loading classpath entry snapshots: 33,Number of cache misses when loading classpath entry snapshots: 3,Start time of task action: 1694473298557]"
                )
            )
        )

        val kotlinBuildReports = KotlinBuildReportsParserCustomValues(listOf(build), listOf(build)).parse()
        assertTrue(kotlinBuildReports.variantA.isEmpty())
    }
}
