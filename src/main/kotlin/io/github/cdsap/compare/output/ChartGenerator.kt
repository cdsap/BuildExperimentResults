package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.output.chart.ChartColors
import io.github.cdsap.compare.output.chart.ChartDataGenerator
import io.github.cdsap.compare.output.chart.ChartOptions
import io.github.cdsap.compare.output.chart.KotlinBuildReportsConstants
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import org.nield.kotlinstatistics.median

class ChartGenerator(
    val report: Report,
) {
    private val chartColors = ChartColors()
    private val chartOptions = ChartOptions()
    private val chartDataGenerator = ChartDataGenerator(chartColors, chartOptions)

    /** Task durations are in milliseconds; exclude tasks whose global average is below this. */
    private val minTaskAverageDurationMs = 1000.0

    fun format(value: String) = value.replace(",", "").replace("ms", "").split(" ")[0]

    fun generateCharts(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
    ): String {
        val commonTaskPathsRaw = getCommonTaskPaths(variants)
        val commonTaskPaths = filterTasksByMinAverageDuration(variants, commonTaskPathsRaw)
        val mostExpensiveTaskPath = findMostExpensiveTask(variants, commonTaskPaths)

        val containsKotlinProcess = report.processesReport && hasKotlinProcess(variants)
        val containsGradleProcess = report.processesReport && hasGradleProcess(variants)
        val containsBuildReports = report.kotlinBuildReport && hasBuildReports(variants)
        val containsResourceUsageReports = hasResourceUsageReports(variants)

        val uniqueTotalCollections = getUniqueTotalCollections(variants)

        return generateChart(
            mostExpensiveTaskPath,
            commonTaskPathsRaw,
            commonTaskPaths,
            containsResourceUsageReports,
            uniqueTotalCollections,
            containsKotlinProcess,
            containsGradleProcess,
            containsBuildReports,
            variants,
            header,
        )
    }

    private fun filterTasksByMinAverageDuration(
        variants: Map<String, List<BuildWithResourceUsage>>,
        commonTaskPaths: List<String>,
    ): List<String> =
        commonTaskPaths.filter { taskPath ->
            averageTaskDurationMs(variants, taskPath) >= minTaskAverageDurationMs
        }

    private fun averageTaskDurationMs(
        variants: Map<String, List<BuildWithResourceUsage>>,
        taskPath: String,
    ): Double =
        variants.values
            .flatten()
            .flatMap { it.taskExecution.toList() }
            .filter { it.taskPath == taskPath }
            .map { it.duration.toDouble() }
            .average()
            .takeUnless { it.isNaN() } ?: 0.0

    private fun generateChart(
        mostExpensiveTaskPath: String,
        commonTaskPathsUnfiltered: List<String>,
        commonTaskPaths: List<String>,
        containsResourceUsageReports: Boolean,
        uniqueTotalCollections: Set<String>,
        containsKotlinProcess: Boolean,
        containsGradleProcess: Boolean,
        containsBuildReports: Boolean,
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
    ): String =
        """
        <div class="charts-grid">
        ${
            generateBasicCharts(
                mostExpensiveTaskPath,
                commonTaskPathsUnfiltered,
                commonTaskPaths,
                containsResourceUsageReports,
            )
        }
        ${generateGCCollectionsCharts(uniqueTotalCollections)}
        ${
            generateProcessCharts(
                containsKotlinProcess,
                containsGradleProcess,
            )
        }
        ${generateBuildReportsCharts(containsBuildReports)}
          </div>
                                                                            <script>
                                                                                ${
            generateChartScripts(
                variants,
                mostExpensiveTaskPath,
                commonTaskPaths,
                header,
                containsKotlinProcess,
                containsGradleProcess,
                containsBuildReports,
                uniqueTotalCollections,
            )
        }
                                                                            </script>
        """.trimIndent()

    private fun generateBasicCharts(
        mostExpensiveTaskPath: String,
        commonTaskPathsUnfiltered: List<String>,
        commonTaskPaths: List<String>,
        containsResourceUsageReports: Boolean,
    ): String {
        val divResourceUsage =
            if (containsResourceUsageReports) {
                """
                <div class="chart-container">
                    <h2>Build Process Memory</h2>
                    <canvas id="buildProcessMemoryChart"></canvas>
                </div>
                <div class="chart-container">
                    <h2>Build Child Processes Memory</h2>
                    <canvas id="buildChildProcessMemoryChart"></canvas>
                </div>
                """.trimIndent()
            } else {
                ""
            }
        val taskChartBlock =
            if (commonTaskPaths.isNotEmpty()) {
                """
            <div class="chart-container">
                <h2 id="taskChartTitle">Task Duration: $mostExpensiveTaskPath</h2>
                <div class="controls">
                    <label for="taskSearchInput">Task:</label>
                    <div class="task-picker">
                        <input type="search" id="taskSearchInput" autocomplete="off" spellcheck="false" placeholder="Search tasks — type to filter" />
                        <div id="taskDropdown" class="task-dropdown"></div>
                    </div>
                </div>
                <canvas id="expensiveTaskChart"></canvas>
            </div>
                """.trimIndent()
            } else if (commonTaskPathsUnfiltered.isNotEmpty()) {
                """
            <div class="chart-container">
                <h2>Task Duration</h2>
                <p>No tasks with average duration ≥ 1s (common across variants).</p>
            </div>
                """.trimIndent()
            } else {
                """
            <div class="chart-container">
                <h2>Task Duration</h2>
                <p>No common task paths across variants.</p>
            </div>
                """.trimIndent()
            }
        return """
            <div class="chart-container">
                <h2>Build Duration Time Series</h2>
                <canvas id="buildDurationChart"></canvas>
            </div>
             <div class="chart-container">
                <h2>Configuration Time</h2>
                <canvas id="configurationTimeChart"></canvas>
            </div>
            $divResourceUsage
            $taskChartBlock
            """.trimIndent()
    }

    private fun generateProcessCharts(
        containsKotlinProcess: Boolean,
        containsGradleProcess: Boolean,
    ): String {
        val kotlinDiv =
            if (containsKotlinProcess && report.processesReport) {
                """
                <div class="chart-container">
                    <h2>Time Kotlin Garbage Collection Process</h2>
                    <canvas id="kotlinGCChart"></canvas>
                </div>
                """.trimIndent()
            } else {
                ""
            }

        val gradleDiv =
            if (containsGradleProcess && report.processesReport) {
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

    private fun generateBuildReportsCharts(containsBuildReports: Boolean): String {
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

    private fun generateGCCollectionsCharts(uniqueTotalCollections: Set<String>): String {
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

    private fun getCommonTaskPaths(variants: Map<String, List<BuildWithResourceUsage>>): List<String> {
        val taskPathsPerVariant =
            variants.values.map { builds ->
                builds.flatMap { it.taskExecution.map { task -> task.taskPath } }.toSet()
            }

        val commonTaskPaths =
            if (taskPathsPerVariant.isNotEmpty()) {
                taskPathsPerVariant.reduce { acc, set -> acc.intersect(set) }
            } else {
                emptySet()
            }

        return commonTaskPaths.sorted()
    }

    private fun findMostExpensiveTask(
        variants: Map<String, List<BuildWithResourceUsage>>,
        commonTaskPaths: List<String>,
    ): String {
        if (commonTaskPaths.isEmpty()) return "Unknown"
        return variants.values
            .flatten()
            .flatMap { it.taskExecution.toList() }
            .filter { it.taskPath in commonTaskPaths }
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

    private fun hasBuildReports(variants: Map<String, List<BuildWithResourceUsage>>): Boolean =
        variants.all { (_, builds) ->
            builds.any { build ->
                build.values.any { value ->
                    value.value.contains("Kotlin language version:") &&
                        value.value.contains("Performance: [")
                }
            }
        }

    private fun hasResourceUsageReports(variants: Map<String, List<BuildWithResourceUsage>>): Boolean =
        !variants.any {
            it.value.any { it.total == null }
        }

    private fun getUniqueTotalCollections(variants: Map<String, List<BuildWithResourceUsage>>): Set<String> {
        val uniqueCollections = mutableSetOf<String>()
        val containsGCCollections =
            variants.all { (_, builds) ->
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
                            .map { it.name },
                    )
                }
            }
        }
        return uniqueCollections
    }

    private fun generateChartScripts(
        variants: Map<String, List<BuildWithResourceUsage>>,
        mostExpensiveTaskPath: String,
        commonTaskPaths: List<String>,
        header: Header,
        containsKotlinProcess: Boolean,
        containsGradleProcess: Boolean,
        containsBuildReports: Boolean,
        uniqueTotalCollections: Set<String>,
    ): String {
        val scripts = mutableListOf<String>()
        // Basic charts
        scripts.add(generateBuildDurationChart(variants, header))
        scripts.add(generateConfigurationTimeChart(variants, header))
        scripts.add(generateProcessMemoryChart(variants, header))
        scripts.add(generateChildProcessMemoryChart(variants, header))
        if (commonTaskPaths.isNotEmpty()) {
            scripts.add(generateExpensiveTaskChart(variants, mostExpensiveTaskPath, commonTaskPaths, header))
        }

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
        header: Header,
    ): String =
        chartDataGenerator.generateChartData(
            "buildDurationChart",
            variants,
            "Build Duration (seconds)",
            { it.buildDuration.toDouble() },
            header,
        )

    private fun generateConfigurationTimeChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
    ): String =
        chartDataGenerator.generateChartData(
            "configurationTimeChart",
            variants,
            "Configuration time Duration (seconds)",
            { it.configuration.toDouble() },
            header,
        )

    private fun generateProcessMemoryChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
    ): String {
        if (variants.any { it.value.any { it.total == null } }) return ""
        return chartDataGenerator.generateChartData(
            "buildProcessMemoryChart",
            variants,
            "Memory Usage (MB)",
            {
                it.total!!
                    .buildProcessMemory.max
                    .toDouble()
            },
            header,
        )
    }

    private fun generateChildProcessMemoryChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
    ): String {
        if (variants.any { it.value.any { it.total == null } }) return ""
        return chartDataGenerator.generateChartData(
            "buildChildProcessMemoryChart",
            variants,
            "Memory Usage (MB)",
            {
                it.total!!
                    .buildChildProcessesMemory.max
                    .toDouble()
            },
            header,
        )
    }

    private fun generateExpensiveTaskChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        mostExpensiveTaskPath: String,
        commonTaskPaths: List<String>,
        header: Header,
    ): String {
        val taskPaths = if (commonTaskPaths.isNotEmpty()) commonTaskPaths else listOf(mostExpensiveTaskPath)
        val defaultTask = taskPaths.firstOrNull { it == mostExpensiveTaskPath } ?: taskPaths.firstOrNull() ?: "Unknown"
        val chartOptionsTask = chartOptions.getChartOptions("Task Duration (seconds)")

        val taskData =
            taskPaths.joinToString(",\n") { taskPath ->
                val perVariant =
                    variants
                        .map { (variant, builds) ->
                            val cleanedVariant = variant.removeExperimentId(header.experimentId)
                            val values =
                                builds.joinToString(",") { build ->
                                    build.taskExecution
                                        .find { it.taskPath == taskPath }
                                        ?.duration
                                        ?.toDouble()
                                        ?.toString() ?: "0.0"
                                }
                            "'${escapeForJs(cleanedVariant)}': [$values]"
                        }.joinToString(",")
                "'${escapeForJs(taskPath)}': {$perVariant}"
            }

        val variantColors =
            variants.keys
                .joinToString(",") { variant ->
                    val cleanedVariant = variant.removeExperimentId(header.experimentId)
                    "'${escapeForJs(cleanedVariant)}': '${chartColors.getColorForVariant(variant)}'"
                }

        return """
            const taskData = {$taskData};
            const taskSearchInput = document.getElementById('taskSearchInput');
            const taskDropdown = document.getElementById('taskDropdown');
            const taskChartTitle = document.getElementById('taskChartTitle');
            const variantColors = {$variantColors};
            let selectedTask = '${escapeForJs(defaultTask)}';
            const allTaskPaths = Object.keys(taskData).sort(function(a, b) { return a.localeCompare(b); });
            const MAX_TASK_SUGGESTIONS = 100;
            let highlightIndex = -1;
            let currentMatches = [];

            function createTaskDatasets(taskPath) {
                const series = taskData[taskPath] || {};
                return Object.keys(series).map(variant => ({
                    label: variant,
                    data: series[variant],
                    borderColor: variantColors[variant],
                    tension: 0.1
                }));
            }

            const expensiveTaskChart = new Chart(document.getElementById('expensiveTaskChart'), {
                type: 'line',
                data: {
                    labels: ${(1..variants.values.first().size).toList()},
                    datasets: createTaskDatasets('${escapeForJs(defaultTask)}')
                },
                options: $chartOptionsTask
            });

            function applyTask(taskPath) {
                if (!taskData[taskPath]) return;
                selectedTask = taskPath;
                if (taskSearchInput) taskSearchInput.value = taskPath;
                expensiveTaskChart.data.datasets = createTaskDatasets(taskPath);
                expensiveTaskChart.update();
                if (taskChartTitle) {
                    taskChartTitle.textContent = 'Task Duration: ' + taskPath;
                }
                hideTaskDropdown();
            }

            function hideTaskDropdown() {
                if (!taskDropdown) return;
                taskDropdown.classList.remove('open');
                taskDropdown.innerHTML = '';
                highlightIndex = -1;
                currentMatches = [];
            }

            function getFilteredMatches(query) {
                const q = query.trim().toLowerCase();
                if (!q) {
                    return allTaskPaths.slice(0, MAX_TASK_SUGGESTIONS);
                }
                const out = [];
                for (let i = 0; i < allTaskPaths.length && out.length < MAX_TASK_SUGGESTIONS; i++) {
                    if (allTaskPaths[i].toLowerCase().indexOf(q) !== -1) {
                        out.push(allTaskPaths[i]);
                    }
                }
                return out;
            }

            function updateTaskHighlight() {
                if (!taskDropdown) return;
                const items = taskDropdown.querySelectorAll('.task-dropdown-item');
                for (let i = 0; i < items.length; i++) {
                    items[i].classList.toggle('task-dropdown-item-active', i === highlightIndex);
                }
                if (highlightIndex >= 0 && items[highlightIndex]) {
                    items[highlightIndex].scrollIntoView({ block: 'nearest' });
                }
            }

            function renderTaskDropdown(matches, query) {
                if (!taskDropdown) return;
                taskDropdown.innerHTML = '';
                const hint = document.createElement('div');
                hint.className = 'task-dropdown-hint';
                const total = allTaskPaths.length;
                if (matches.length === 0) {
                    hint.textContent = 'No tasks match your filter.';
                    taskDropdown.appendChild(hint);
                } else {
                    if (!query.trim()) {
                        hint.textContent = 'Showing first ' + matches.length + ' of ' + total + ' tasks — type to narrow the list';
                    } else if (matches.length === MAX_TASK_SUGGESTIONS) {
                        hint.textContent = 'Showing first ' + MAX_TASK_SUGGESTIONS + ' matches — refine your search';
                    } else {
                        hint.textContent = matches.length + (matches.length === 1 ? ' match' : ' matches');
                    }
                    taskDropdown.appendChild(hint);
                    for (let j = 0; j < matches.length; j++) {
                        const path = matches[j];
                        const btn = document.createElement('button');
                        btn.type = 'button';
                        btn.className = 'task-dropdown-item';
                        btn.textContent = path;
                        btn.addEventListener('mousedown', function(e) {
                            e.preventDefault();
                            applyTask(path);
                        });
                        taskDropdown.appendChild(btn);
                    }
                }
                taskDropdown.classList.add('open');
                highlightIndex = matches.length > 0 ? 0 : -1;
                updateTaskHighlight();
            }

            if (taskSearchInput && taskDropdown) {
                taskSearchInput.value = selectedTask;

                taskSearchInput.addEventListener('focus', function() {
                    const q = taskSearchInput.value;
                    currentMatches = getFilteredMatches(q);
                    renderTaskDropdown(currentMatches, q);
                });

                taskSearchInput.addEventListener('input', function() {
                    const q = taskSearchInput.value;
                    currentMatches = getFilteredMatches(q);
                    renderTaskDropdown(currentMatches, q);
                });

                taskSearchInput.addEventListener('keydown', function(e) {
                    if (!taskDropdown.classList.contains('open')) return;
                    const items = taskDropdown.querySelectorAll('.task-dropdown-item');
                    if (e.key === 'ArrowDown') {
                        e.preventDefault();
                        highlightIndex = Math.min(highlightIndex + 1, items.length - 1);
                        updateTaskHighlight();
                    } else if (e.key === 'ArrowUp') {
                        e.preventDefault();
                        highlightIndex = Math.max(highlightIndex - 1, 0);
                        updateTaskHighlight();
                    } else if (e.key === 'Enter') {
                        e.preventDefault();
                        if (highlightIndex >= 0 && items[highlightIndex]) {
                            applyTask(items[highlightIndex].textContent);
                        } else if (currentMatches.length === 1) {
                            applyTask(currentMatches[0]);
                        } else {
                            const v = taskSearchInput.value.trim();
                            if (taskData[v]) applyTask(v);
                        }
                    } else if (e.key === 'Escape') {
                        e.preventDefault();
                        taskSearchInput.value = selectedTask;
                        hideTaskDropdown();
                    }
                });

                taskSearchInput.addEventListener('blur', function() {
                    setTimeout(function() {
                        const v = taskSearchInput.value.trim();
                        if (taskData[v]) {
                            if (v !== selectedTask) applyTask(v);
                        } else {
                            taskSearchInput.value = selectedTask;
                        }
                        hideTaskDropdown();
                    }, 200);
                });

                document.addEventListener('click', function(e) {
                    if (taskSearchInput.contains(e.target) || taskDropdown.contains(e.target)) return;
                    hideTaskDropdown();
                });
            }
            """.trimIndent()
    }

    private fun generateKotlinGCChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
    ): String {
        val regex = Regex("^Kotlin-Process-\\d+-gcTime$")
        val uptimeRegex = Regex("Kotlin-Process-(\\d+)-uptime")

        return chartDataGenerator.generateChartData(
            "kotlinGCChart",
            variants,
            "Kotlin GC (minutes)",
            { build ->
                // A build could have multiple Kotlin processes running during execution, for instance when
                // there is no alignment between the embedded Kotlin version in Gradle and the one used in the project.
                // The approach here is to consider the last process because the assumption is that is the most relevant one.
                val uptimeProcesses = build.values.filter { it.name.matches(uptimeRegex) }
                if (uptimeProcesses.size == 1) {
                    build.values
                        .filter { it.name.matches(regex) }
                        .last()
                        ?.value
                        ?.replace("minutes", "")
                        ?.toDouble() ?: 0.0
                } else {
                    // Get distinct process IDs from uptime entries.
                    val processIds =
                        build.values
                            .filter { it.name.matches(uptimeRegex) }
                            .mapNotNull { uptimeRegex.find(it.name)?.groupValues?.get(1) }
                            .distinct()

                    // Find the process ID with the minimum uptime using similar logic.
                    val processIdWithMinUptime =
                        processIds.minByOrNull { id ->
                            build.values
                                .filter { it.name.matches(Regex("Kotlin-Process-$id-uptime")) }
                                .last()
                                ?.value
                                ?.replace("minutes", "")
                                ?.toDouble() ?: Double.MAX_VALUE
                        } ?: ""

                    // Now get the gcTime for that process.
                    build.values
                        .filter { it.name.matches(Regex("Kotlin-Process-$processIdWithMinUptime-gcTime")) }
                        .last()
                        ?.value
                        ?.replace("minutes", "")
                        ?.toDouble() ?: 0.0
                }
            },
            header,
        )
    }

    private fun generateGradleGCChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
    ): String {
        val regex = Regex("^Gradle-Process-\\d+-gcTime$")
        return chartDataGenerator.generateChartData(
            "gradleGCChart",
            variants,
            "Gradle GC (minutes)",
            { build ->
                build.values
                    .find { it.name.matches(regex) }
                    ?.value
                    ?.replace("minutes", "")
                    ?.toDouble() ?: 0.0
            },
            header,
        )
    }

    private fun generateKotlinBuildReportsChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
        reportName: String,
    ): String =
        chartDataGenerator.generateChartData(
            reportName.replace(" ", "").lowercase(),
            variants,
            "$reportName (ms)",
            { build ->
                val buildReportValues =
                    build.values
                        .filter { it.value.contains(reportName) }
                        .mapNotNull { value ->
                            value.value
                                .split("Performance: [")[1]
                                .split("]")[0]
                                .split(",")
                                .find { it.contains("$reportName:") }
                                ?.split(":")
                                ?.get(1)
                                ?.replace("}", "")
                                ?.replace("ms", "")
                                ?.trim()
                                ?.toLongOrNull()
                        }
                buildReportValues.median() ?: 0.0
            },
            header,
        )

    private fun generateGCCollectionsChart(
        variants: Map<String, List<BuildWithResourceUsage>>,
        header: Header,
        collection: String,
    ): String =
        chartDataGenerator.generateChartData(
            collection.replace("-total-collections", "").lowercase(),
            variants,
            collection,
            { build ->
                build.values
                    .find { it.name == collection }
                    ?.value
                    ?.toDouble() ?: 0.0
            },
            header,
        )

    private fun escapeForJs(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
}

