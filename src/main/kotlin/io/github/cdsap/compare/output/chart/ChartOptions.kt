package io.github.cdsap.compare.output.chart

class ChartOptions {
    fun getChartOptions(
        yAxisLabel: String,
        minY: Int? = null,
    ): String =
        """
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
