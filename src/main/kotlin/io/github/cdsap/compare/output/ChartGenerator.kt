package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Header
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class ChartGenerator {
    private val variantColors = mutableMapOf<String, String>()
    private val predefinedColors = listOf(
        "#36A2EB", // Light blue
        "#FF6384", // Pink/Red
        "#4BC0C0", // Teal
        "#FF9F40", // Orange
        "#9966FF", // Purple
        "#FFCD56", // Yellow
        "#C9CBCF" // Grey
    )

    fun generateCharts(variants: Map<String, List<BuildWithResourceUsage>>, header: Header): String {
        val mostExpensiveTaskPath = findMostExpensiveTask(variants)
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
            </div>
            <script>
                ${generateChartScripts(variants, mostExpensiveTaskPath, header)}
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

    private fun generateChartContainers(): String {
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
                <h2>Most Expensive Task</h2>
                <canvas id="expensiveTaskChart"></canvas>
            </div>
        """.trimIndent()
    }

    private fun generateChartScripts(
        variants: Map<String, List<BuildWithResourceUsage>>,
        mostExpensiveTaskPath: String,
        header: Header
    ): String {
        return """
            ${generateBuildDurationChart(variants, header)}
            ${generateProcessMemoryChart(variants, header)}
            ${generateChildProcessMemoryChart(variants, header)}
            ${generateExpensiveTaskChart(variants, mostExpensiveTaskPath, header)}
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
            isDuration = true,
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
                isMemory = true,
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
                isMemory = true,
                header = header
            )
        }
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
            isDuration = true,
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
        isDuration: Boolean = false,
        isMemory: Boolean = false,
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
