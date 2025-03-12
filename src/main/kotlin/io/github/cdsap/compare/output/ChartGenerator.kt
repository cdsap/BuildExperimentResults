package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.output.chart.ChartColors
import io.github.cdsap.compare.output.chart.ChartDataGenerator
import io.github.cdsap.compare.output.chart.ChartOptions
import io.github.cdsap.compare.output.chart.KotlinBuildReportsConstants
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.median

class ChartGenerator(val report: Report) {
    private val chartColors = ChartColors()
    private val chartOptions = ChartOptions()
    private val chartDataGenerator = ChartDataGenerator(chartColors, chartOptions)

    fun format(value: String) = value.replace(",", "").replace("ms", "").split(" ")[0]

    fun generateCharts(variants: Map<String, List<BuildWithResourceUsage>>, header: Header): String {
        val mostExpensiveTaskPath = findMostExpensiveTask(variants)

        val containsKotlinProcess = report.processesReport && hasKotlinProcess(variants)
        val containsGradleProcess = report.processesReport && hasGradleProcess(variants)
        val containsBuildReports = report.kotlinBuildReport && hasBuildReports(variants)
        val uniqueTotalCollections = getUniqueTotalCollections(variants)

        return """
            <div class="charts-grid">
                ${generateBasicCharts(mostExpensiveTaskPath)}
                ${generateGCCollectionsCharts(uniqueTotalCollections)}
                ${generateProcessCharts(containsKotlinProcess, containsGradleProcess)}
                ${generateBuildReportsCharts(containsBuildReports)}
            </div>
            <script>
                ${generateChartScripts(variants, mostExpensiveTaskPath, header, containsKotlinProcess, containsGradleProcess, containsBuildReports, uniqueTotalCollections)}
            </script>
        """.trimIndent()
    }

    private fun generateBasicCharts(mostExpensiveTaskPath: String): String {
        return """
            <div class="chart-container">
                <h2>Build Duration Time Series</h2>
                <canvas id="buildDurationChart"></canvas>
            </div>
            <div class="chart-container">
                <h2>Build Process Memory</h2>
                <canvas id="buildProcessMemoryChart"></canvas>
            </div>
            <div class="chart-container">
                <h2>Build Child Processes Memory</h2>
                <canvas id="buildChildProcessMemoryChart"></canvas>
            </div>
            <div class="chart-container">
                <h2>Most Expensive Task: $mostExpensiveTaskPath</h2>
                <canvas id="expensiveTaskChart"></canvas>
            </div>
        """.trimIndent()
    }

    private fun generateProcessCharts(
        containsKotlinProcess: Boolean,
        containsGradleProcess: Boolean
    ): String {
        val kotlinDiv = if (containsKotlinProcess && report.processesReport) {
            """
                <div class="chart-container">
                    <h2>Time Kotlin Garbage Collection Process</h2>
                    <canvas id="kotlinGCChart"></canvas>
                </div>
            """.trimIndent()
        } else {
            ""
        }

        val gradleDiv = if (containsGradleProcess && report.processesReport) {
            """
                <div class="chart-container">
                    <h2>Time Gradle Garbage Collection Process</h2>
                    <canvas id="gradleGCChart"></canvas>
                </div>
            """.trimIndent()
        } else {
            ""
        }

        return kotlinDiv + gradleDiv
    }

    private fun generateBuildReportsCharts(
        containsBuildReports: Boolean
    ): String {
        if (!report.kotlinBuildReport || !containsBuildReports) return ""

        return KotlinBuildReportsConstants.ALLOWED_LIST.joinToString("") { report ->
            """
            <div class="chart-container">
                <h2>Kotlin Build Report - $report</h2>
                <canvas id="${report.replace(" ", "").lowercase()}"></canvas>
            </div>
            """.trimIndent()
        }
    }

    private fun generateGCCollectionsCharts(
        uniqueTotalCollections: Set<String>
    ): String {
        if (report.gcReport) {
            return uniqueTotalCollections.joinToString("") { collection ->
                """
            <div class="chart-container">
                <h2>Total gc collections - ${collection.replace("-total-collections", "")}</h2>
                <canvas id="${collection.replace("-total-collections", "")}"></canvas>
            </div>
                """.trimIndent()
            }
        } else {
            return ""
        }
    }

    private fun findMostExpensiveTask(variants: Map<String, List<BuildWithResourceUsage>>): String {
        return variants.values.flatten()
            .flatMap { it.taskExecution.toList() }
            .groupBy { it.taskPath }
            .mapValues { (_, executions) -> executions.map { it.duration }.average() }
            .maxByOrNull { it.value }
            ?.key ?: "Unknown"
    }

    private fun hasKotlinProcess(variants: Map<String, List<BuildWithResourceUsage>>): Boolean {
        val regex = Regex("^Kotlin-Process-\\d+-gcTime$")
        return variants.values.flatten().any { build ->
            build.values.any { it.name.matches(regex) }
        }
    }

    private fun hasGradleProcess(variants: Map<String, List<BuildWithResourceUsage>>): Boolean {
        val regex = Regex("^Gradle-Process-\\d+-gcTime$")
        return variants.values.flatten().any { build ->
            build.values.any { it.name.matches(regex) }
        }
    }

    private fun hasBuildReports(variants: Map<String, List<BuildWithResourceUsage>>): Boolean {
        return variants.all { (_, builds) ->
            builds.any { build ->
                build.values.any { value ->
                    value.value.contains("Kotlin language version:") &&
                        value.value.contains("Performance: [")
                }
            }
        }
    }

    private fun getUniqueTotalCollections(variants: Map<String, List<BuildWithResourceUsage>>): Set<String> {
        val uniqueCollections = mutableSetOf<String>()
        val containsGCCollections = variants.all { (_, builds) ->
            builds.any { build ->
                build.values.any { it.name.contains("-total-collections") }
            }
        }

        if (containsGCCollections) {
            variants.forEach { (_, builds) ->
                builds.forEach { build ->
                    uniqueCollections.addAll(
                        build.values
                            .filter { it.name.contains("-total-collections") }
                            .map { it.name }
                    )
                }
            }
        }
        return uniqueCollections
    }

    private fun generateChartScripts(
        variants: Map<String, List<BuildWithResourceUsage>>,
        mostExpensiveTaskPath: String,
        header: Header,
        containsKotlinProcess: Boolean,
        containsGradleProcess: Boolean,
        containsBuildReports: Boolean,
        uniqueTotalCollections: Set<String>
    ): String {
        val scripts = mutableListOf<String>()
        // Basic charts
        scripts.add(generateBuildDurationChart(variants, header))
        scripts.add(generateProcessMemoryChart(variants, header))
        scripts.add(generateChildProcessMemoryChart(variants, header))
        scripts.add(generateExpensiveTaskChart(variants, mostExpensiveTaskPath, header))

        // Process charts
        if (containsKotlinProcess) {
            scripts.add(generateKotlinGCChart(variants, header))
        }
        if (containsGradleProcess) {
            scripts.add(generateGradleGCChart(variants, header))
        }

        // Build reports
        if (containsBuildReports) {
            KotlinBuildReportsConstants.ALLOWED_LIST.forEach { report ->
                scripts.add(generateKotlinBuildReportsChart(variants, header, report))
            }
        }

        // GC collections
        uniqueTotalCollections.forEach { collection ->
            scripts.add(generateGCCollectionsChart(variants, header, collection))
        }

        return scripts.joinToString("\n")
    }

    private fun generateBuildDurationChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header
    ): String = chartDataGenerator.generateChartData(
        "buildDurationChart",
        variants,
        "Build Duration (seconds)",
        { it.buildDuration.toDouble() },
        header
    )

    private fun generateProcessMemoryChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header
    ): String {
        if (variants.any { it.value.any { it.total == null } }) return ""
        return chartDataGenerator.generateChartData(
            "buildProcessMemoryChart",
            variants,
            "Memory Usage (MB)",
            { it.total!!.buildProcessMemory.max.toDouble() },
            header
        )
    }

    private fun generateChildProcessMemoryChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header
    ): String {
        if (variants.any { it.value.any { it.total == null } }) return ""
        return chartDataGenerator.generateChartData(
            "buildChildProcessMemoryChart",
            variants,
            "Memory Usage (MB)",
            { it.total!!.buildChildProcessesMemory.max.toDouble() },
            header
        )
    }

    private fun generateExpensiveTaskChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        mostExpensiveTaskPath: String,
        header: Header
    ): String = chartDataGenerator.generateChartData(
        "expensiveTaskChart",
        variants,
        "Task Duration (seconds)",
        { build ->
            build.taskExecution
                .find { it.taskPath == mostExpensiveTaskPath }
                ?.duration?.toDouble() ?: 0.0
        },
        header
    )

    private fun generateKotlinGCChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header
    ): String {
        val regex = Regex("^Kotlin-Process-\\d+-gcTime$")
        return chartDataGenerator.generateChartData(
            "kotlinGCChart",
            variants,
            "Kotlin GC (minutes)",
            { build ->
                build.values.find { it.name.matches(regex) }
                    ?.value?.replace("minutes", "")?.toDouble() ?: 0.0
            },
            header
        )
    }

    private fun generateGradleGCChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header
    ): String {
        val regex = Regex("^Gradle-Process-\\d+-gcTime$")
        return chartDataGenerator.generateChartData(
            "gradleGCChart",
            variants,
            "Gradle GC (minutes)",
            { build ->
                build.values.find { it.name.matches(regex) }
                    ?.value?.replace("minutes", "")?.toDouble() ?: 0.0
            },
            header
        )
    }

    private fun generateKotlinBuildReportsChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
        reportName: String
    ): String = chartDataGenerator.generateChartData(
        reportName.replace(" ", "").lowercase(),
        variants,
        "$reportName (ms)",
        { build ->
            val buildReportValues = build.values
                .filter { it.value.contains(reportName) }
                .mapNotNull { value ->
                    value.value.split("Performance: [")[1]
                        .split("]")[0]
                        .split(",")
                        .find { it.contains("$reportName:") }
                        ?.split(":")?.get(1)
                        ?.replace("}", "")
                        ?.replace("ms", "")
                        ?.trim()
                        ?.toLongOrNull()
                }
            buildReportValues.median() ?: 0.0
        },
        header
    )

    private fun generateGCCollectionsChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
        collection: String
    ): String {
        return chartDataGenerator.generateChartData(
            collection.replace("-total-collections", "").lowercase(),
            variants,
            collection,
            { build ->
                build.values.find { it.name == collection }
                    ?.value?.toDouble() ?: 0.0
            },
            header
        )
    }
}
