package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.report.measurements.parser.ProcessesReportParser
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.median

class ChartGenerator {
    private val variantColors = mutableMapOf<String, String>()
    private val predefinedColors = listOf(
        "#36A2EB",
        "#FF6384",
        "#4BC0C0",
        "#FF9F40",
        "#9966FF",
        "#FFCD56",
        "#C9CBCF"
    )
    val kotlinBuildReportsAllowedList = listOf(
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

    fun format(value: String) = value.replace(",", "").replace("ms", "").split(" ")[0]

    fun generateCharts(variants: Map<String, List<BuildWithResourceUsage>>, header: Header): String {
        val mostExpensiveTaskPath = findMostExpensiveTask(variants)
        val processResources = ProcessesReportParser()

        var countKotlin = 0
        var countGradle = 0
        var countKotlinBuildReports = 0
        var totalGCCollections = 0

        variants.forEach { t, u ->
            if (u.filter {
                    it.values.filter { it.name.contains("-total-collections") }.isNotEmpty()
                }.isNotEmpty()) {
                totalGCCollections++
            }
        }

        var divTotalCollections = ""
        val uniqueTotalCollections = mutableSetOf<String>()

        val containsGCCollections = variants.size == totalGCCollections
        if (containsGCCollections) {
            variants.forEach { t, u ->
                val elements = u.groupBy { it.values.filter { it.name.contains("-total-collections") }.map { it.name } }
                    .map { it.key }
                uniqueTotalCollections.addAll(elements.flatten())
            }
            uniqueTotalCollections.forEach {
                divTotalCollections += """
                 <div class="chart-container">
                    <h2>Total gc collections - ${it.replace("-total-collections","")}</h2>
                    <canvas id="${it.replace("-total-collections","")}"></canvas>
                </div>
                """.trimIndent()
            }



        }

        variants.forEach { t, u ->
            if (u.filter {
                    it.values.filter { it.value.contains("Kotlin language version:") }.isNotEmpty() &&
                        it.values.filter { it.value.contains("Performance: [") }.isNotEmpty()
                }.isNotEmpty()) {
                countKotlinBuildReports++
            }
        }

        var divKotlinBuildReports = ""
        val containsBuildReports = countKotlinBuildReports == variants.size
        if (containsBuildReports) {
            kotlinBuildReportsAllowedList.forEach {
                divKotlinBuildReports += """
               <div class="chart-container">
                    <h2>Kotlin Build Report - $it</h2>
                    <canvas id="${it.replace(" ", "").lowercase()}"></canvas>
                </div>
           """.trimIndent()
            }
        }
        variants.forEach { t, u ->
            val variantGradleValues = processResources.parse(u.first().values, "Gradle")
            val variantKotlinValues = processResources.parse(u.first().values, "Kotlin")
            if (variantGradleValues.isNotEmpty()) {
                countGradle++
            }
            if (variantKotlinValues.isNotEmpty()) {
                countKotlin++
            }
        }
        val containsKotlinProcess = variants.size == countKotlin

        val kotlinDiv = if (containsKotlinProcess) {
            """
                <div class="chart-container">
                    <h2>Time Kotlin Garbage Collection Process</h2>
                    <canvas id="kotlinGCChart"></canvas>
                </div>
            """.trimIndent()
        } else ""
        val containsGradleProcess = variants.size == countGradle

        val gradleDiv = if (containsGradleProcess) {
            """
                <div class="chart-container">
                    <h2>Time Gradle Garbage Collection Process</h2>
                    <canvas id="gradleGCChart"></canvas>
                </div>
            """.trimIndent()
        } else ""


        return """
            <div class="charts-grid">
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
                $kotlinDiv
                $gradleDiv
                $divKotlinBuildReports
                $divTotalCollections

            </div>
            <script>
                ${
            generateChartScripts(
                variants,
                mostExpensiveTaskPath,
                header,
                containsKotlinProcess,
                containsGradleProcess,
                containsBuildReports,
                uniqueTotalCollections
            )
        }
            </script>
        """.trimIndent()
    }

    private fun findMostExpensiveTask(variants: Map<String, List<BuildWithResourceUsage>>): String {
        return variants.values.flatten()
            .flatMap { it.taskExecution.toList() }
            .groupBy { it.taskPath }
            .mapValues { (_, executions) -> executions.map { it.duration }.average() }
            .maxByOrNull { it.value }
            ?.key ?: "Unknown"
    }


    private fun generateChartScripts(
        variants: Map<String, List<BuildWithResourceUsage>>,
        mostExpensiveTaskPath: String,
        header: Header,
        containsKotlinProcess: Boolean,
        containsGradleProcess: Boolean,
        containsBuildReports: Boolean,
        containsGCCollections: MutableSet<String>
    ): String {
        var kotlinProcessDiv = ""
        if (containsKotlinProcess) {
            kotlinProcessDiv = generateKotlinGCChart(variants, header)
        }

        var gradleProcessDiv = ""
        if (containsGradleProcess) {
            gradleProcessDiv = generateGradleGCChart(variants, header)
        }

        var kotlinDivBuildReports = ""
        if (containsBuildReports) {
            kotlinBuildReportsAllowedList.forEach {
                kotlinDivBuildReports += generateKotlinBuildReportsCharts(variants, header, it)
            }
        }
        var divGCCollections = ""
        containsGCCollections.forEach {
            divGCCollections += generateGCCollections(variants, header, it)
        }

        return """
            ${generateBuildDurationChart(variants, header)}
            ${generateProcessMemoryChart(variants, header)}
            ${generateChildProcessMemoryChart(variants, header)}
            ${generateExpensiveTaskChart(variants, mostExpensiveTaskPath, header)}
            $kotlinProcessDiv
            $gradleProcessDiv
           $kotlinDivBuildReports
           $divGCCollections
        """.trimIndent()
    }

    private fun generateBuildDurationChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header
    ): String {
        return generateChartData(
            "buildDurationChart",
            variants,
            "Build Duration (seconds)",
            { it.buildDuration.toDouble() },
            header = header
        )
    }

    private fun generateProcessMemoryChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header
    ): String {
        if (variants.any { it.value.any { it.total == null } }) {
            return ""
        } else {
            return generateChartData(
                "buildProcessMemoryChart",
                variants,
                "Memory Usage (MB)",
                { it.total.buildProcessMemory.max.toDouble() },
                header = header
            )
        }
    }

    private fun generateChildProcessMemoryChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header
    ): String {
        if (variants.any { it.value.any { it.total == null } }) {
            return ""
        } else {
            return generateChartData(
                "buildChildProcessMemoryChart",
                variants,
                "Memory Usage (MB)",
                { it.total.buildChildProcessesMemory.max.toDouble() },
                header = header
            )
        }
    }

    private fun generateKotlinGCChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header
    ): String {

        val regex = Regex("^Kotlin-Process-\\d+-gcTime$")
        return generateChartData(
            "kotlinGCChart",
            variants,
            "Kotlin GC (minutes)",
            { it.values.filter { it.name.contains(regex) }.last().value.replace("minutes", "").toDouble() },
            header = header
        )
    }

    private fun generateKotlinBuildReportsCharts(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
        s: String
    ): String {
        return generateChartData(
            s.replace(" ", "").lowercase(),
            variants,
            "$s (ms)",
            {
                val elementsWithCustomValue = it.values.filter { it.value.contains(s) }
                val buildReportValues = mutableListOf<Long>()
                elementsWithCustomValue.forEach {

                    val values = it.value.split("Performance: [")[1].split("]")[0].split(",")

                    val element = values.filter { it.contains("$s:") }.first()
                    val valuePositon = element.split(":")[1].replace("}", "").replace("ms", "").replace(" ", "")
                    buildReportValues.add(valuePositon.toLong())

                }

                if (buildReportValues.isEmpty()) 0.0 else buildReportValues.median()
            },
            header = header
        )
    }

    private fun generateGCCollections(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
        s: String
    ): String {
        return generateChartData(
            s.replace("-total-collections","").lowercase(),
            variants,
            "$s",
            { it.values.filter { it.name == s }.last().value.toDouble()},
            header = header
        )
    }

    private fun generateGradleGCChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header
    ): String {

        val regex = Regex("^Gradle-Process-\\d+-gcTime$")
        return generateChartData(
            "gradleGCChart",
            variants,
            "Gradle GC (minutes)",
            { it.values.filter { it.name.contains(regex) }.last().value.replace("minutes", "").toDouble() },
            header = header
        )
    }

    private fun generateExpensiveTaskChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        mostExpensiveTaskPath: String,
        header: Header
    ): String {
        return generateChartData(
            "expensiveTaskChart",
            variants,
            "Task Duration (seconds)",
            { build ->
                build.taskExecution
                    .find { it.taskPath == mostExpensiveTaskPath }
                    ?.duration?.toDouble() ?: 0.0
            },
            header = header
        )
    }

    private fun generateChart(
        chartId: String,
        variants: Map<String, List<BuildWithResourceUsage>>,
        valueExtractor: (BuildWithResourceUsage) -> Number,
        yAxisLabel: String,
        minY: Int?
    ): String {
        return """
            new Chart(document.getElementById('$chartId'), {
                type: 'line',
                data: {
                    labels: [${(1..variants.values.first().size).joinToString(",")}],
                    datasets: [
                        ${
            variants.map { (variant, builds) ->
                """
                            {
                                label: '$variant',
                                data: [${builds.map { valueExtractor(it) }.joinToString(",")}],
                                fill: false,
                                borderColor: '${getRandomColor(variant)}',
                                tension: 0.1
                            }
            """.trimIndent()
            }.joinToString(",")
        }
                    ]
                },
                options: ${getChartOptions(yAxisLabel, minY)}
            });
        """.trimIndent()
    }

    private fun getChartOptions(yAxisLabel: String, minY: Int?): String {
        return """
        {
            responsive: true,
            plugins: {
                title: {
                    display: true
                }
            },
            scales: {
                y: {
                    ${if (minY != null) "min: $minY," else ""}
                    ticks: {
                        callback: function(value, index, ticks) {
                            if (this.chart.options.scales.y.title.text.includes('Memory')) {
                                return (value / (1024 * 1024)).toFixed(2) + ' MB';
                            } else if (this.chart.options.scales.y.title.text.includes('Duration')) {
                                return (value / 1000).toFixed(2) + ' s';
                            }
                            return value;
                        }
                    },
                    title: {
                        display: true,
                        text: '$yAxisLabel'
                    }
                },
                x: {
                    title: {
                        display: true,
                        text: 'Build Number'
                    }
                }
            }
        }
        """.trimIndent()
    }

    private fun generateChartData(
        chartId: String,
        variants: Map<String, List<BuildWithResourceUsage>>,
        yAxisLabel: String,
        valueSelector: (BuildWithResourceUsage) -> Double,
        header: Header
    ): String {
        val datasets = variants.map { (variant, builds) ->
            """
            {
                label: '${variant.removeExperimentId(header.experimentId)}',
                data: [${builds.map { valueSelector(it) }.joinToString(",")}],
                borderColor: '${getRandomColor(variant)}',
                tension: 0.1
            }
            """.trimIndent()
        }.joinToString(",")

        return """
        new Chart(document.getElementById('$chartId'), {
            type: 'line',
            data: {
                labels: ${(1..variants.values.first().size).toList()},
                datasets: [$datasets]
            },
            options: ${getChartOptions(yAxisLabel, null)}
        });
        """.trimIndent()
    }

    private fun getRandomColor(variant: String): String {
        return variantColors.getOrPut(variant) {
            if (variantColors.size < predefinedColors.size) {
                predefinedColors[variantColors.size]
            } else {
                // Fallback to random color if we run out of predefined colors
                "#" + (variant.hashCode() and 0xFFFFFF).toString(16).padStart(6, '0')
            }
        }
    }
}
