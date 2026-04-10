package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.model.SingleMeasurement
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class HtmlGenerator(
    private val report: Report,
    private val measurementProcessor: MeasurementProcessor = MeasurementProcessor(report),
    private val chartGenerator: ChartGenerator,
) {
    fun generate(
        measurements: Map<String, List<SingleMeasurement>>,
        variants: List<String>,
        header: Header,
        buildVariants: Map<String, List<BuildWithResourceUsage>>,
    ): String =
        generateHtmlDocument(
            generateHtmlContent(measurements, variants, header, buildVariants),
        )

    fun generateTableSummary(
        measurements: Map<String, List<SingleMeasurement>>,
        variants: List<String>,
        header: Header,
        buildVariants: Map<String, List<BuildWithResourceUsage>>,
    ): String = generateHtmlContentSummary(measurements, variants, header, buildVariants)

    private fun generateHtmlDocument(content: String): String =
        """
        <!DOCTYPE html>
        <html>
        <head>
            ${getHtmlHead()}
        </head>
        <body>
            <div class="container">
                <h1>Gradle Build Performance Report</h1>
                $content
            </div>
        </body>
        </html>
        """.trimIndent()

    private fun generateHtmlContent(
        measurements: Map<String, List<SingleMeasurement>>,
        variants: List<String>,
        header: Header,
        buildVariants: Map<String, List<BuildWithResourceUsage>>,
    ): String {
        val experimentInfo = generateExperimentInfo(variants, header, buildVariants)
        val charts = chartGenerator.generateCharts(buildVariants, header)
        val metricsTable = generateMetricsTable(measurements, variants, false, header)
        return "$experimentInfo$charts$metricsTable"
    }

    private fun generateHtmlContentSummary(
        measurements: Map<String, List<SingleMeasurement>>,
        variants: List<String>,
        header: Header,
        buildVariants: Map<String, List<BuildWithResourceUsage>>,
    ): String {
        val experimentInfo = generateExperimentInfo(variants, header, buildVariants)
        val metricsTable = generateMetricsTable(measurements, variants, true, header)
        return "$experimentInfo$metricsTable"
    }

    private fun getHtmlHead(): String =
        """
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Gradle Build Performance Report</title>
        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500&display=swap" rel="stylesheet">
        <style>
            body {
                font-family: 'Roboto', sans-serif;
                margin: 0;
                padding: 12px;
                background-color: #f5f5f5;
            }
            .container {
                max-width: 1400px;
                margin: 0 auto;
                background-color: white;
                padding: 16px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            .charts-grid {
                display: grid;
                grid-template-columns: repeat(2, minmax(0, 1fr));
                gap: 16px;
                margin-bottom: 24px;
            }
            .chart-container {
                background-color: white;
                padding: 12px;
                border-radius: 8px;
                box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                min-height: 260px;
            }
            .chart-container canvas {
                width: 100% !important;
                height: 240px !important;
            }
            .controls {
                display: flex;
                flex-wrap: wrap;
                gap: 8px;
                align-items: center;
                margin: 8px 0 16px;
            }
            .controls select,
            .task-picker input[type="search"] {
                max-width: 100%;
                min-width: 220px;
                padding: 6px;
                font: inherit;
                box-sizing: border-box;
            }
            .task-picker {
                position: relative;
                flex: 1;
                min-width: 220px;
                max-width: min(100%, 720px);
            }
            .task-dropdown {
                display: none;
                position: absolute;
                left: 0;
                right: 0;
                top: 100%;
                margin-top: 2px;
                max-height: 280px;
                overflow-y: auto;
                background: #fff;
                border: 1px solid #ccc;
                border-radius: 4px;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                z-index: 100;
            }
            .task-dropdown.open {
                display: block;
            }
            .task-dropdown-hint {
                padding: 6px 10px;
                font-size: 11px;
                color: #666;
                border-bottom: 1px solid #eee;
                line-height: 1.35;
            }
            .task-dropdown-item {
                display: block;
                width: 100%;
                text-align: left;
                padding: 6px 10px;
                border: none;
                background: none;
                font: 12px 'Roboto Mono', monospace;
                cursor: pointer;
                color: #222;
            }
            .task-dropdown-item:hover,
            .task-dropdown-item:focus {
                background: #f0f0f0;
                outline: none;
            }
            .task-dropdown-item-active {
                background: #e8f0fe !important;
            }
            .table-wrapper {
                overflow-x: auto;
                -webkit-overflow-scrolling: touch;
            }
            table {
                border-collapse: collapse;
                width: 100%;
                min-width: 760px;
                margin: 20px 0;
                font-size: 14px;
                background-color: white;
            }
            th, td {
                border: 1px solid #e0e0e0;
                padding: 8px;
                text-align: left;
                white-space: nowrap;
            }
            th {
                background-color: #f5f5f5;
                color: #333;
                font-weight: 500;
                text-align: center;
            }
            .stats-header {
                background-color: #f0f0f0;
            }
            tr:nth-child(even) {
                background-color: #fafafa;
            }
            .metric-value {
                text-align: right;
                font-family: 'Roboto Mono', monospace;
            }
            .unit-column {
                color: #666;
                font-size: 12px;
            }
            h1, h2 {
                color: #333;
            }
            h1 {
                text-align: center;
                margin-bottom: 20px;
                font-size: 24px;
            }
            h2 {
                margin-bottom: 10px;
                font-size: 18px;
            }
            @media (max-width: 768px) {
                body {
                    padding: 8px;
                }
                .container {
                    padding: 10px;
                }
                .charts-grid {
                    grid-template-columns: 1fr;
                }
                h1 {
                    font-size: 20px;
                }
                h2 {
                    font-size: 16px;
                }
                table {
                    font-size: 12px;
                    min-width: 640px;
                }
            }
        </style>
        """.trimIndent()

    private fun generateExperimentInfo(
        variants: List<String>,
        header: Header,
        buildVariants: Map<String, List<BuildWithResourceUsage>>,
    ): String {
        val timestamp =
            buildVariants.values
                .first()
                .first()
                .buildStartTime
        val oneWeekMillis = 2 * 24 * 60 * 60 * 1000L
        val oneWeekBefore = timestamp - oneWeekMillis
        val oneWeekAfter = timestamp + oneWeekMillis
        var output = "<div class='table-wrapper'><table><tr><td colspan=${6 + variants.size * 3}>Results</td></tr>"
        if (header.repository != null) {
            output += "<tr><td>Repository</td><td colspan=${5 + variants.size * 3}>${header.repository}</td></tr>"
        }
        output += "<tr><td>Task</td><td colspan=${5 + variants.size * 3}>${header.task}</td></tr>"

        buildVariants.forEach { (variant, builds) ->
            val url = "${header.url}scans?search.startTimeMax=$oneWeekAfter&search.startTimeMin=$oneWeekBefore&search.tags=$variant"
            output +=
                "<tr><td>${variant.removeExperimentId(
                    header.experimentId,
                )}</td><td>${builds.size} builds processed</td><td colspan=${4 + variants.size * 3}><a href=\"$url\">Build Scans</a></td></tr>"
        }
        if (header.linkCsv.isNotEmpty() && header.htmlSummary) {
            output +=
                "<tr><td>Execution raw data</td><td colspan=${5 + variants.size * 3}><a href=\"${header.linkCsv}\">Download csv</a></td></tr>"
        }
        if (header.experimentRunId != null && header.htmlSummary) {
            output +=
                "<tr><td>Experiment run execution</td><td colspan=${5 + variants.size * 3}><a href=\"https://github.com/cdsap/Telltale/actions/runs/${header.experimentRunId}\">Workflow</a></td></tr>"
        }
        output += "</table></div>"
        return output
    }

    private fun generateMetricsTable(
        measurements: Map<String, List<SingleMeasurement>>,
        variants: List<String>,
        filterMetrics: Boolean,
        header: Header,
    ): String {
        var output = "<div class='table-wrapper'><table id='metricsTable' class='metric-table'>"

        // Main headers
        output += "<tr>"
        output += "<th>Category</th>"
        output += "<th>Metric</th>"
        output += "<th colspan=${variants.size * 3 + 1} class='stats-header'>Statistics</th>"
        output += "</tr>"

        // Metric headers
        output += "<tr>"
        output += "<th colspan='2'></th>" // Empty cells for Category and Metric
        output += "<th colspan=${variants.size}>Mean</th>"
        output += "<th colspan=${variants.size}>P50</th>"
        output += "<th colspan=${variants.size}>P90</th>"
        output += "<th>Unit</th>"
        output += "</tr>"

        // Variant headers
        output += "<tr class='variant-headers'>"
        output += "<th colspan='2'></th>" // Empty cells for Category and Metric
        repeat(3) {
            // For Mean, P50, P90
            variants.forEach { variant ->
                output += "<th>${variant.removeExperimentId(header.experimentId)}</th>"
            }
        }
        output += "<th></th>" // For Unit
        output += "</tr>"

        // Data rows
        measurementProcessor
            .processMeasurements(measurements)
            .filter {
                if (filterMetrics) {
                    measurementProcessor.filterUnwantedMetrics(it)
                } else {
                    true
                }
            }.forEach { rowData ->
                output += "<tr>"
                output += "<td>${rowData["category"]}</td>"
                output += "<td>${rowData["name"]}</td>"

                // Mean values
                variants.forEach { variant ->
                    output += "<td class='metric-value'>${formatValue(rowData["$variant-mean"])}</td>"
                }
                // P50 values
                variants.forEach { variant ->
                    output += "<td class='metric-value'>${formatValue(rowData["$variant-median"])}</td>"
                }
                // P90 values
                variants.forEach { variant ->
                    output += "<td class='metric-value'>${formatValue(rowData["$variant-p90"])}</td>"
                }
                output += "<td class='unit-column'>${rowData["${variants[0]}-unit"] ?: ""}</td>"
                output += "</tr>"
            }

        output += "</table></div>"
        return output
    }

    private fun formatValue(value: Any?): String =
        when (value) {
            is Double -> String.format("%.0f", value)
            else -> value?.toString() ?: "-"
        }
}
