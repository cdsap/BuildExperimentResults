package io.github.cdsap.compare

import io.github.cdsap.geapi.client.model.Filter
import io.github.cdsap.geapi.client.network.GEClient
import io.github.cdsap.geapi.client.repository.impl.GradleRepositoryImpl

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import io.github.cdsap.geapi.client.network.ClientConf


import kotlinx.coroutines.runBlocking
import io.github.cdsap.compare.report.ExperimentReport

fun main(args: Array<String>) {
    Experiment().main(args)
}

class Experiment : CliktCommand() {
    private val apiKey: String by option().required()
    private val url by option().required()
    private val maxBuilds by option().int().default(1000).check("max builds to process 30000") { it <= 30000 }
    private val project: String? by option()
    private val tags: List<String> by option().multiple(default = emptyList())
    private val concurrentCalls by option().int().default(150)
    private val concurrentCallsCache by option().int().default(10)
    private val tasks: String? by option()
    private val experimentId: String? by option()
    private val user: String? by option()

    override fun run() {
        val filter = Filter(
            url = url,
            maxBuilds = maxBuilds,
            project = project,
            tags = tags,
            variants = tags.joinToString (","),
            initFilter = System.currentTimeMillis(),
            user = user,
            requestedTask = tasks,
            experimentId =  experimentId,
            concurrentCalls = concurrentCalls,
            concurrentCallsConservative = concurrentCallsCache
        )
        val repository = GradleRepositoryImpl(
            GEClient(
                apiKey, url, ClientConf(
                    maxRetries = 300,
                    exponentialBase = 1.0,
                    exponentialMaxDelay = 5000
                )
            )
        )

        runBlocking {
            ExperimentReport(filter, repository).process()
        }
    }
}
