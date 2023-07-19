package io.github.cdsap.compare.view

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.compare.model.MeasurementWithPercentiles
import io.github.cdsap.compare.report.Header
import java.io.File

class ExperimentView {

    fun print(measurement: List<MeasurementWithPercentiles>, varianta: String, variantb: String, header: Header) {
        println(generateTable(measurement, varianta, variantb,header))

        File("results_experiment").writeText(generateHtmlTable(measurement, varianta, variantb,header))
    }

    private fun generateTable(
        measurement: List<MeasurementWithPercentiles>,
        varianta: String,
        variantb: String,
        header: Header
    ) =
        table {
            cellStyle {
                border = true
                alignment = TextAlignment.MiddleLeft
                paddingLeft = 1
                paddingRight = 1
            }
            body {
                row {
                    cell("Experiment") {
                        columnSpan = 8
                        alignment = TextAlignment.MiddleCenter
                    }
                }

                measurement.groupBy {
                    it.OS
                }.forEach {
                    row {
                        cell("Experiment id") {
                        }
                        cell("${header.experiment}"){
                            columnSpan = 7
                        }
                    }
                    row {
                        cell("Experiment task") {
                        }
                        cell("${header.task}"){
                            columnSpan = 7
                        }
                    }
                    row {
                        cell("$varianta") {
                        }
                        cell("Builds processed: ${header.numberOfBuildsForExperimentA}"){
                            columnSpan = 7
                        }
                    }
                    row {
                        cell("$variantb") {
                        }
                        cell("Builds processed: ${header.numberOfBuildsForExperimentB}"){
                            columnSpan = 7
                        }
                    }
                    row {
                        cell("Category") {
                            rowSpan = 2
                            alignment = TextAlignment.MiddleCenter
                        }
                        cell("Metric") {
                            rowSpan = 2
                            alignment = TextAlignment.MiddleCenter
                        }
                        cell(" Mean") {
                            columnSpan = 2
                            alignment = TextAlignment.MiddleCenter
                        }

                        cell("P50")
                        {
                            columnSpan = 2
                            alignment = TextAlignment.MiddleCenter
                        }
                        cell(" P90")
                        {
                            columnSpan = 2
                            alignment = TextAlignment.MiddleCenter
                        }


                    }
                    row {
                        cell("$varianta") { alignment = TextAlignment.MiddleCenter }
                        cell("$variantb") { alignment = TextAlignment.MiddleCenter }
                        cell("$varianta") { alignment = TextAlignment.MiddleCenter }
                        cell("$variantb") { alignment = TextAlignment.MiddleCenter }
                        cell("$varianta") { alignment = TextAlignment.MiddleCenter }
                        cell("$variantb") { alignment = TextAlignment.MiddleCenter }
                    }
                    it.value.forEach {
                        row {
                            cell(it.category)
                            cell(it.name)
                            cell(it.variantAMean) {
                                alignment = TextAlignment.MiddleRight
                            }
                            cell(it.variantBMean) {
                                alignment = TextAlignment.MiddleRight
                            }
                            cell(it.variantAP50) {
                                alignment = TextAlignment.MiddleRight
                            }
                            cell(it.variantBP50) {
                                alignment = TextAlignment.MiddleRight
                            }
                            cell(it.variantAP90) {
                                alignment = TextAlignment.MiddleRight
                            }
                            cell(it.variantBP90) {
                                alignment = TextAlignment.MiddleRight
                            }
                        }

                    }
                }
            }
        }

    private fun generateHtmlTable(
        measurement: List<MeasurementWithPercentiles>,
        varianta: String,
        variantb: String,
        header: Header
    ): String {
        var output = ""
        output += "<table><tr><td colspan=4>Experiment</td></tr>"
        output += "<tr><td>Task experiment</td><td colspan=7>${header.task}</td></tr>"
        output += "<tr><td>$varianta</td><td colspan=7>${header.numberOfBuildsForExperimentA} builds processed</td></tr>"
        output += "<tr><td>$variantb</td><td colspan=7>${header.numberOfBuildsForExperimentB} builds processed</td></tr>"
        output += "<tr><td rowspan=2>Category</td><td rowspan=2>Metric</td><td colspan=2>Mean</td><td colspan=2>P50</td><td colspan=2>P90</td></tr>"
        output += "<tr><td>$varianta</td><td>$variantb</td><td>$varianta</td><td>$variantb</td><td>$varianta</td><td>$variantb</td></tr>"

        measurement.groupBy {
            it.OS
        }.forEach {
            it.value.forEach {
                output += "<tr><td>${it.category}</td><td>${it.name}</td><td>${it.variantAMean}</td><td>${it.variantBMean}</td><td>${it.variantAP50}</td><td>${it.variantBP50}</td><td>${it.variantAP90}</td><td>${it.variantBP90}</td></tr>"
            }
        }
        output += "</table>"
        return output
    }
}
