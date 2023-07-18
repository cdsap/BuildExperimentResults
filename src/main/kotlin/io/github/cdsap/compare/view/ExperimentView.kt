package io.github.cdsap.compare.view

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.compare.model.Measurement
import java.io.File

class ExperimentView {

    private val LIMIT_DIFFERENCE_LONG = 1000L
    private val LIMIT_DIFFERENCE_INT = 1000

    fun print(measurement: List<Measurement>, varianta: String, variantb: String) {
        println(generateTable(measurement, varianta, variantb))

        File("results_experiment").writeText(generateHtmlTable(measurement, varianta, variantb))
    }

    private fun generateTable(measurement: List<Measurement>, varianta: String, variantb: String) =
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
                        columnSpan = 4
                        alignment = TextAlignment.MiddleCenter
                    }
                }

                measurement.groupBy {
                    it.OS
                }.forEach {
                    row {
                        cell(it.key.name) {
                            columnSpan = 4
                            alignment = TextAlignment.MiddleCenter
                        }
                    }
                    row {
                        cell("Category")
                        cell("Metric")
                        cell(varianta)
                        cell(variantb)
                    }
                    it.value.forEach {
                        if (it.variantA is Long) {

                            row {
                                cell(it.category)
                                cell(it.name)
                                cell(it.variantA) {
                                    alignment = TextAlignment.MiddleRight
                                }
                                cell(it.variantB) {
                                    alignment = TextAlignment.MiddleRight
                                }
                            }

                        }
                        if (it.variantA is Int) {
                            row {
                                cell(it.category)
                                cell(it.name)
                                cell(it.variantA) {
                                    alignment = TextAlignment.MiddleRight
                                }
                                cell(it.variantB) {
                                    alignment = TextAlignment.MiddleRight
                                }
                            }
                        }
                        if (it.variantA is String) {
                            row {
                                cell(it.category)
                                cell(it.name)
                                cell(it.variantA) {
                                    alignment = TextAlignment.MiddleRight
                                }
                                cell(it.variantB) {
                                    alignment = TextAlignment.MiddleRight
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun generateHtmlTable(measurement: List<Measurement>, varianta: String, variantb: String): String {
        var output = ""
        output += "<table><tr><td colspan=4>Experiment</td></tr>"
        output += "<tr><td>Category</td><td>Metric</td><td>$varianta</td><td>$variantb</td></tr>"

        measurement.groupBy {
            it.OS
        }.forEach {
            it.value.forEach {
                output += "<tr><td>${it.category}</td><td>${it.name}</td><td>${it.variantA}</td><td>${it.variantB}</td></tr>"
            }
        }
        output += "</table>"
        return output
    }
}
