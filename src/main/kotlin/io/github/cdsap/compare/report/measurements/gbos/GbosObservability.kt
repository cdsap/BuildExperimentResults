package io.github.cdsap.compare.report.measurements.gbos

import io.github.cdsap.geapi.client.model.CustomValue

/**
 * Detects whether a build published GBOS (build-observability) custom values
 * from the cdsap process-plugin family.
 */
object GbosObservability {
    const val SCHEMA = "gbos.schema"
    const val PRODUCER_PREFIX = "gbos.v1.producer."

    const val PRODUCER_INFO_GRADLE = "info_gradle_process"
    const val PRODUCER_INFO_KOTLIN = "info_kotlin_process"
    const val PRODUCER_GC_REPORT = "gc_report"

    fun isEnabled(values: Array<CustomValue>): Boolean = values.any { it.name == SCHEMA || it.name.startsWith(PRODUCER_PREFIX) }

    fun hasProducer(
        values: Array<CustomValue>,
        producerSlug: String,
    ): Boolean {
        val observation = observationName(producerSlug)
        val nameKey = "$PRODUCER_PREFIX$producerSlug.name"
        return values.any { it.name == observation || it.name == nameKey }
    }

    fun producers(values: Array<CustomValue>): Set<String> =
        values
            .asSequence()
            .map { it.name }
            .filter { it.startsWith(PRODUCER_PREFIX) }
            .mapNotNull { name ->
                val rest = name.removePrefix(PRODUCER_PREFIX)
                val slug = rest.substringBefore('.')
                slug.takeIf { it.isNotBlank() }
            }.toSet()

    fun observationName(producerSlug: String): String = "$PRODUCER_PREFIX$producerSlug.observation"

    fun hasLegacyGradleProcess(values: Array<CustomValue>): Boolean = values.any { it.name.startsWith("Gradle-Process-") }

    fun hasLegacyKotlinProcess(values: Array<CustomValue>): Boolean = values.any { it.name.startsWith("Kotlin-Process-") }

    fun hasLegacyGcReport(values: Array<CustomValue>): Boolean = values.any { it.name.contains("-total-collections") }
}
