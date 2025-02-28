package io.github.cdsap.compare.view

import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class ChartGenerator {
    fun generateCharts(variants: Map<String, List<BuildWithResourceUsage>>): String {
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
                    <h2>Most Expensive Task: ${mostExpensiveTaskPath}</h2>
                    <canvas id="expensiveTaskChart"></canvas>
                </div>
            </div>
            <script>
                ${generateChartScripts(variants, mostExpensiveTaskPath)}
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
        mostExpensiveTaskPath: String
    ): String {
        return """
            ${generateBuildDurationChart(variants)}
            ${generateProcessMemoryChart(variants)}
            ${generateChildProcessMemoryChart(variants)}
            ${generateExpensiveTaskChart(variants, mostExpensiveTaskPath)}
        """.trimIndent()
    }

    private fun generateBuildDurationChart(variants: Map<String, List<BuildWithResourceUsage>>): String {
        return generateChart(
            "buildDurationChart",
            variants,
            { it.buildDuration },
            "Build Duration (ms)",
            200000
        )
    }

    private fun generateProcessMemoryChart(variants: Map<String, List<BuildWithResourceUsage>>): String {
        return generateChart(
            "buildProcessMemoryChart",
            variants,
            { it.total.buildProcessMemory.max },
            "Memory Usage (bytes)",
            null
        )
    }

    private fun generateChildProcessMemoryChart(variants: Map<String, List<BuildWithResourceUsage>>): String {
        return generateChart(
            "buildChildProcessMemoryChart",
            variants,
            { it.total.buildChildProcessesMemory.max },
            "Memory Usage (bytes)",
            null
        )
    }

    private fun generateExpensiveTaskChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        mostExpensiveTaskPath: String
    ): String {
        return generateChart(
            "expensiveTaskChart",
            variants,
            { build ->
                build.taskExecution
                    .find { it.taskPath == mostExpensiveTaskPath }
                    ?.duration ?: 0.0
            },
            "Duration (ms)",
            null
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
                        ${variants.map { (variant, builds) ->
            """
                            {
                                label: '$variant',
                                data: [${builds.map { valueExtractor(it) }.joinToString(",")}],
                                fill: false,
                                borderColor: '${getRandomColor(variant)}',
                                tension: 0.1
                            }
            """.trimIndent()
        }.joinToString(",")}
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
                        callback: function(value) {
                            return (value / 1000).toFixed(0) + 'k';
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

    private fun getRandomColor(seed: String): String {
        val colors = listOf(
            "#FF6384",
            "#36A2EB",
            "#FFCE56",
            "#4BC0C0",
            "#9966FF",
            "#FF9F40",
            "#FF6384",
            "#C9CBCF"
        )
        return colors[Math.abs(seed.hashCode()) % colors.size]
    }
}
