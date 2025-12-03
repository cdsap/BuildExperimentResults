package io.github.cdsap.compare.output.chart

class ChartColors {
    private val variantColors = mutableMapOf<String, String>()
    private val predefinedColors =
        listOf(
            "#36A2EB",
            "#FF6384",
            "#4BC0C0",
            "#FF9F40",
            "#9966FF",
            "#FFCD56",
            "#C9CBCF",
        )

    fun getColorForVariant(variant: String): String =
        variantColors.getOrPut(variant) {
            if (variantColors.size < predefinedColors.size) {
                predefinedColors[variantColors.size]
            } else {
                // Fallback to random color if we run out of predefined colors
                "#" + (variant.hashCode() and 0xFFFFFF).toString(16).padStart(6, '0')
            }
        }
}
