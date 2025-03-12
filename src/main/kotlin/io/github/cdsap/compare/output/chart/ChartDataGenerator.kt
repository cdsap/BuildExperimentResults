package io.github.cdsap.compare.output.chart

import io.github.cdsap.compare.model.Header
import io.github.cdsap.compare.output.removeExperimentId
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage

class ChartDataGenerator(
    private val chartColors: ChartColors,
    private val chartOptions: ChartOptions
) {
    fun generateChartData(
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
                borderColor: '${chartColors.getColorForVariant(variant)}',
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
            options: ${chartOptions.getChartOptions(yAxisLabel)}
        });
        """.trimIndent()
    }
}
