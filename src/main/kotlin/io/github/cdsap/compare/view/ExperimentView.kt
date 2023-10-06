package io.github.cdsap.compare.view

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.compare.model.*

import java.io.File

class ExperimentView(
    val report: Report,
    val task: String
) {

    fun print(measurement: List<MeasurementWithPercentiles>, variants: BuildsPerVariant) {
        val header = header(variants)
        println(generateTable(measurement, report.variants[0], report.variants[1], header))
        File("results_experiment").writeText(
            generateHtmlTable(
                measurement,
                report.variants[0],
                report.variants[1],
                header
            )
        )
    }

    private fun header(variants: BuildsPerVariant) = Header(
        task = task,
        numberOfBuildsForExperimentA = variants.variantA.size,
        numberOfBuildsForExperimentB = variants.variantB.size,
        experiment = report.experimentId
    )

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
                row {
                    cell("Experiment id") {
                    }
                    cell("${header.experiment}") {
                        columnSpan = 7
                    }
                }
                row {
                    cell("Experiment task") {
                    }
                    cell("${header.task}") {
                        columnSpan = 7
                    }
                }
                row {
                    cell("$varianta") {
                    }
                    cell("Builds processed: ${header.numberOfBuildsForExperimentA}") {
                        columnSpan = 7
                    }
                }
                row {
                    cell("$variantb") {
                    }
                    cell("Builds processed: ${header.numberOfBuildsForExperimentB}") {
                        columnSpan = 7
                    }
                }
                measurement
                    .groupBy {
                        it.OS
                    }.forEach {


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
                            cell("$varianta".splitString()) { alignment = TextAlignment.MiddleCenter }
                            cell("$variantb".splitString()) { alignment = TextAlignment.MiddleCenter }
                            cell("$varianta".splitString()) { alignment = TextAlignment.MiddleCenter }
                            cell("$variantb".splitString()) { alignment = TextAlignment.MiddleCenter }
                            cell("$varianta".splitString()) { alignment = TextAlignment.MiddleCenter }
                            cell("$variantb".splitString()) { alignment = TextAlignment.MiddleCenter }
                        }
                        it.value.forEach {
                            row {
                                cell(it.category.splitString())
                                cell(it.name.splitString())
                                cell("${it.variantAMean} ${it.qualifier}".splitSmallString()) {
                                    alignment = TextAlignment.MiddleRight
                                }
                                cell("${it.variantBMean} ${it.qualifier}".splitSmallString()) {
                                    alignment = TextAlignment.MiddleRight
                                }
                                cell("${it.variantAP50} ${it.qualifier}".splitSmallString()) {
                                    alignment = TextAlignment.MiddleRight
                                }
                                cell("${it.variantBP50} ${it.qualifier}".splitSmallString()) {
                                    alignment = TextAlignment.MiddleRight
                                }
                                cell("${it.variantAP90} ${it.qualifier}".splitSmallString()) {
                                    alignment = TextAlignment.MiddleRight
                                }
                                cell("${it.variantBP90} ${it.qualifier}".splitSmallString()) {
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
        output += "<table><tr><td colspan=8>Experiment</td></tr>"
        output += "<tr><td>Task experiment</td><td colspan=7>${header.task}</td></tr>"
        output += "<tr><td>$varianta</td><td colspan=7>${header.numberOfBuildsForExperimentA} builds processed</td></tr>"
        output += "<tr><td>$variantb</td><td colspan=7>${header.numberOfBuildsForExperimentB} builds processed</td></tr>"
        output += "<tr><td rowspan=2>Category</td><td rowspan=2>Metric</td><td colspan=2>Mean</td><td colspan=2>P50</td><td colspan=2>P90</td></tr>"
        output += "<tr><td>$varianta</td><td>$variantb</td><td>$varianta</td><td>$variantb</td><td>$varianta</td><td>$variantb</td></tr>"

        var buildReport = ""
        measurement.filter { it.metric == Metric.BUILD }.forEach {
            buildReport += "<tr><td>${it.category}</td><td>${it.name}</td><td>${it.variantAMean} ${it.qualifier}</td><td>${it.variantBMean} ${it.qualifier}</td><td>${it.variantAP50} ${it.qualifier}</td><td>${it.variantBP50} ${it.qualifier}</td><td>${it.variantAP90} ${it.qualifier}</td><td>${it.variantBP90} ${it.qualifier}</td></tr>"
        }

        var outputTaskPath = ""
        measurement.filter { it.metric == Metric.TASK_PATH }.forEach {
            outputTaskPath += "<tr><td>${it.category}</td><td>${it.name}</td><td>${it.variantAMean} ${it.qualifier}</td><td>${it.variantBMean} ${it.qualifier}</td><td>${it.variantAP50} ${it.qualifier}</td><td>${it.variantBP50} ${it.qualifier}</td><td>${it.variantAP90} ${it.qualifier}</td><td>${it.variantBP90} ${it.qualifier}</td></tr>"
        }

        var outputTaskType = ""
        measurement.filter { it.metric == Metric.TASK_TYPE }.forEach {
            outputTaskType += "<tr><td>${it.category}</td><td>${it.name}</td><td>${it.variantAMean} ${it.qualifier}</td><td>${it.variantBMean} ${it.qualifier}</td><td>${it.variantAP50} ${it.qualifier}</td><td>${it.variantBP50} ${it.qualifier}</td><td>${it.variantAP90} ${it.qualifier}</td><td>${it.variantBP90} ${it.qualifier}</td></tr>"
        }

        var outputProcesses = ""
        measurement.filter { it.metric == Metric.PROCESS }.forEach {
            outputProcesses += "<tr><td>${it.category}</td><td>${it.name}</td><td>${it.variantAMean} ${it.qualifier}</td><td>${it.variantBMean} ${it.qualifier}</td><td>${it.variantAP50} ${it.qualifier}</td><td>${it.variantBP50} ${it.qualifier}</td><td>${it.variantAP90} ${it.qualifier}</td><td>${it.variantBP90} ${it.qualifier}</td></tr>"
        }

        var outputKotlinBuildReports = ""
        measurement.filter { it.metric == Metric.KOTLIN_BUILD_REPORT }.forEach {
            outputKotlinBuildReports += "<tr><td>${it.category}</td><td>${it.name}</td><td>${it.variantAMean} ${it.qualifier}</td><td>${it.variantBMean} ${it.qualifier}</td><td>${it.variantAP50} ${it.qualifier}</td><td>${it.variantBP50} ${it.qualifier}</td><td>${it.variantAP90} ${it.qualifier}</td><td>${it.variantBP90} ${it.qualifier}</td></tr>"
        }

        var tasksOutputKotlinBuildReports = ""
        measurement.filter { it.metric == Metric.TASK_KOTLIN_BUILD_REPORT }.forEach {
            tasksOutputKotlinBuildReports += "<tr><td>${it.category}</td><td>${it.name}</td><td>${it.variantAMean} ${it.qualifier}</td><td>${it.variantBMean} ${it.qualifier}</td><td>${it.variantAP50} ${it.qualifier}</td><td>${it.variantBP50} ${it.qualifier}</td><td>${it.variantAP90} ${it.qualifier}</td><td>${it.variantBP90} ${it.qualifier}</td></tr>"
        }

        if (output.length + outputTaskPath.length + outputTaskType.length + outputProcesses.length + outputKotlinBuildReports.length + tasksOutputKotlinBuildReports.length > 1000000) {
            if (output.length + outputTaskPath.length + outputTaskType.length + outputProcesses.length + outputKotlinBuildReports.length > 1000000) {
                output += buildReport + outputTaskType + outputTaskPath + outputProcesses
            } else {
                output += buildReport + outputTaskType + outputTaskPath + outputProcesses + outputKotlinBuildReports
            }
        } else {
            output += buildReport + outputTaskType + outputTaskPath + outputProcesses + outputKotlinBuildReports + tasksOutputKotlinBuildReports
        }

        output += "</table>"
        return output
    }
}

fun String.splitString() = this.chunked(22).joinToString("\n")

fun String.splitSmallString() = this.chunked(15).joinToString("\n")
