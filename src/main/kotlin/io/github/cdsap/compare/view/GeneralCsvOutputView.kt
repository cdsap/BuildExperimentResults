package io.github.cdsap.compare.view

import io.github.cdsap.compare.model.MeasurementWithPercentiles
import java.io.File

class GeneralCsvOutputView(
    private val outcome: List<MeasurementWithPercentiles>,
    private val variantA: String,
    private val variantB: String
) {

    fun write() {
        val csv = "build_comparison.csv"
        val headers =
            "type,metric,mean_$variantA,mean_$variantB,mean_unit," +
                "p50_$variantA,p50_$variantB,p50_unit,p90_$variantA,p50_$variantB,p90_unit\n"

        var values = ""
        outcome.forEach {
            values += "${it.category},${it.name},${it.variantAMean},${it.variantBMean},${it.qualifier},${it.variantAP50},${it.variantBP50},${it.qualifier}," +
                "${it.variantAP90},${it.variantBP90},${it.qualifier}\n"
        }
        File(csv).writeText("""$headers$values""".trimIndent())
        println("File $csv created")
    }
}

