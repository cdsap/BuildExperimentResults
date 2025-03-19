package io.github.cdsap.compare

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import io.github.cdsap.compare.model.Report
import io.github.cdsap.compare.report.ExperimentReport
import io.github.cdsap.geapi.client.model.ClientType
import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.network.GEClient
import io.github.cdsap.geapi.client.repository.impl.GradleRepositoryImpl
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    Experiment().main(args)
}

class Experiment : CliktCommand() {
    private val apiKey: String by option().required()
    private val url by option().required()
    private val maxBuilds by option().int().default(200).check("max builds to process 1000") { it <= 1000 }
    private val project: String? by option()
    private val requestedTask: String? by option()
    private val variants: List<String> by option().multiple(default = emptyList())
    private val profile by option().flag(default = false)
    private val taskPathReport by option("--task-path-report").flag("--no-task-path-report", default = true)
    private val taskTypeReport by option("--task-type-report").flag("--no-task-type-report", default = true)
    private val kotlinBuildReport by option("--kotlin-build-report").flag("--no-kotlin-build-report", default = false)
    private val resourceUsageReport by option("--resource-usage-report").flag(
        "--no-resource-usage-report",
        default = true
    )
    private val processesReport by option("--process-report").flag("--no-process-report", default = false)
    private val buildReport by option("--build-report").flag("--no-build-report", default = true)
    private val gcReport by option("--gc-report").flag("--no-gc-report", default = false)
    private val onlyCacheableOutcome by option("--only-cacheable-outcome").flag(
        "--no-only-cacheable-outcome",
        default = false
    )
    private val warmupsToDiscard by option().int().default(2)
    private val thresholdTaskDuration by option().long().default(-1)
    private val experimentId: String? by option()
    private val repository: String? by option()
    private val experimentRunId: String? by option()
    private val openAiRequest by option("--open-ai-request").flag("--no-open-ai-request", default = false)
    private val openAiKey: String? by option()

    override fun run() {
        if (!taskPathReport && !taskTypeReport && !kotlinBuildReport && !processesReport) {
            throw IllegalArgumentException("You need to specify at least one type of report")
        }
        val filter = Filter(
            maxBuilds = maxBuilds,
            project = project,
            tags = variants,
            requestedTask = requestedTask,
            exclusiveTags = false,
            clientType = ClientType.CLI

        )
        val dvRepository = GradleRepositoryImpl(GEClient(apiKey, url))

        runBlocking {
            ExperimentReport(
                filter = filter,
                repository = dvRepository,
                Report(
                    taskPathReport = taskPathReport,
                    taskTypeReport = taskTypeReport,
                    kotlinBuildReport = kotlinBuildReport,
                    processesReport = processesReport,
                    buildReport = buildReport,
                    resourceUsageReport = resourceUsageReport,
                    gcReport = gcReport,
                    experimentId = experimentId,
                    warmupsToDiscard = warmupsToDiscard,
                    variants = variants,
                    isProfile = profile,
                    onlyCacheableOutcome = onlyCacheableOutcome,
                    thresholdTaskDuration = thresholdTaskDuration,
                    repository = repository,
                    url = url,
                    experimentRunId = experimentRunId,
                    openAiRequest = openAiRequest,
                    openAiKey = openAiKey
                )
            ).process()
        }
    }
}
