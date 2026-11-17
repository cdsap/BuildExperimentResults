package io.github.cdsap.compare.report.measurements.gbos

import io.github.cdsap.geapi.client.model.CustomValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GbosCustomValuesNormalizerTest {
    private val normalizer = GbosCustomValuesNormalizer()

    @Test
    fun leavesLegacyOnlyValuesUnchanged() {
        val values =
            arrayOf(
                CustomValue("Gradle-Process-1-gcTime", "0.19 minutes"),
                CustomValue("gc-gradle_gc.log-total-collections", "10"),
            )

        assertEquals(values.toList(), normalizer.normalize(values).toList())
        assertFalse(GbosObservability.isEnabled(values))
    }

    @Test
    fun synthesizesGradleAndKotlinProcessLegacyValuesFromGbos() {
        val values =
            arrayOf(
                CustomValue("gbos.schema", "1.0.0"),
                CustomValue("gbos.v1.producer.info_gradle_process.name", "info-gradle-process"),
                CustomValue(
                    "gbos.v1.producer.info_gradle_process.observation",
                    """
                    {"scope":"jvm.process","aggregationScope":"entity",
                    "attributes":{"process.pid":9011,"jvm.process.role":"gradle-daemon","jvm.gc.name":"G1"},
                    "measurements":[
                      {"name":"jvm.process.memory.heap.limit","value":1073741824,"unit":"By","aggregation":"last"},
                      {"name":"jvm.process.memory.heap.used","value":536870912,"unit":"By","aggregation":"last"},
                      {"name":"jvm.process.memory.heap.committed","value":805306368,"unit":"By","aggregation":"last"},
                      {"name":"jvm.process.gc.time","value":15,"unit":"s","aggregation":"sum"},
                      {"name":"jvm.process.uptime","value":150,"unit":"s","aggregation":"last"}
                    ]}
                    """.trimIndent().replace("\n", ""),
                ),
                CustomValue("gbos.v1.producer.info_kotlin_process.name", "info-kotlin-process"),
                CustomValue(
                    "gbos.v1.producer.info_kotlin_process.observation",
                    """
                    {"scope":"jvm.process","aggregationScope":"entity",
                    "attributes":{"process.pid":4242,"jvm.process.role":"kotlin-daemon","jvm.gc.name":"G1"},
                    "measurements":[
                      {"name":"jvm.process.memory.heap.limit","value":2147483648,"unit":"By","aggregation":"last"},
                      {"name":"jvm.process.memory.heap.used","value":1073741824,"unit":"By","aggregation":"last"},
                      {"name":"jvm.process.memory.heap.committed","value":1610612736,"unit":"By","aggregation":"last"},
                      {"name":"jvm.process.gc.time","value":30,"unit":"s","aggregation":"sum"},
                      {"name":"jvm.process.uptime","value":300,"unit":"s","aggregation":"last"}
                    ]}
                    """.trimIndent().replace("\n", ""),
                ),
            )

        val normalized = normalizer.normalize(values)
        val byName = normalized.associate { it.name to it.value }

        assertTrue(GbosObservability.isEnabled(values))
        assertEquals(
            setOf("info_gradle_process", "info_kotlin_process"),
            GbosObservability.producers(values),
        )
        assertEquals("1.0 GB", byName["Gradle-Process-9011-max"])
        assertEquals("0.5 GB", byName["Gradle-Process-9011-usage"])
        assertEquals("0.75 GB", byName["Gradle-Process-9011-capacity"])
        assertEquals("0.25 minutes", byName["Gradle-Process-9011-gcTime"])
        assertEquals("2.5 minutes", byName["Gradle-Process-9011-uptime"])
        assertEquals("G1", byName["Gradle-Process-9011-gcType"])
        assertEquals("2.0 GB", byName["Kotlin-Process-4242-max"])
        assertEquals("0.5 minutes", byName["Kotlin-Process-4242-gcTime"])
    }

    @Test
    fun synthesizesGcLegacyValuesFromGbosWhenLegacyAbsent() {
        val values =
            arrayOf(
                CustomValue("gbos.schema", "1.0.0"),
                CustomValue(
                    "gbos.v1.producer.gc_report.observation",
                    """
                    {"schemaVersion":"1.0.0","producer":{"name":"gc-report","version":"0.1.0"},
                    "scope":"jvm.gc","aggregationScope":"entity",
                    "attributes":{"log.file.name":"gradle_gc.log","jvm.process.role":"gradle-daemon",
                    "jvm.gc.action":"Pause Young (Normal)"},
                    "measurements":[{"name":"jvm.gc.events","value":2,"unit":"{collection}","aggregation":"count"}]}
                    """.trimIndent().replace("\n", ""),
                ),
                CustomValue(
                    "gbos.v1.producer.gc_report.observation",
                    """
                    {"schemaVersion":"1.0.0","producer":{"name":"gc-report","version":"0.1.0"},
                    "scope":"jvm.gc","aggregationScope":"entity",
                    "attributes":{"log.file.name":"gradle_gc.log","jvm.process.role":"gradle-daemon",
                    "jvm.gc.action":"Concurrent Mark Cycle"},
                    "measurements":[{"name":"jvm.gc.events","value":1,"unit":"{collection}","aggregation":"count"}]}
                    """.trimIndent().replace("\n", ""),
                ),
            )

        val byName = normalizer.normalize(values).associate { it.name to it.value }

        assertEquals("3", byName["gc-gradle_gc.log-total-collections"])
        assertEquals("2", byName["gc-gradle_gc.log-Pause Young (Normal)"])
        assertEquals("1", byName["gc-gradle_gc.log-Concurrent Mark Cycle"])
    }

    @Test
    fun prefersLegacyWhenBothGbosAndLegacyPresent() {
        val values =
            arrayOf(
                CustomValue("gbos.schema", "1.0.0"),
                CustomValue(
                    "gbos.v1.producer.info_gradle_process.observation",
                    """
                    {"scope":"jvm.process","aggregationScope":"entity",
                    "attributes":{"process.pid":9011,"jvm.process.role":"gradle-daemon"},
                    "measurements":[
                      {"name":"jvm.process.memory.heap.limit","value":1073741824,"unit":"By","aggregation":"last"},
                      {"name":"jvm.process.gc.time","value":15,"unit":"s","aggregation":"sum"}
                    ]}
                    """.trimIndent().replace("\n", ""),
                ),
                CustomValue("Gradle-Process-1-gcTime", "0.19 minutes"),
                CustomValue(
                    "gbos.v1.producer.gc_report.observation",
                    """
                    {"scope":"jvm.gc","aggregationScope":"entity",
                    "attributes":{"log.file.name":"gradle_gc.log","jvm.gc.action":"Pause Young"},
                    "measurements":[{"name":"jvm.gc.events","value":9,"unit":"{collection}","aggregation":"count"}]}
                    """.trimIndent().replace("\n", ""),
                ),
                CustomValue("gc-gradle_gc.log-total-collections", "10"),
            )

        val normalized = normalizer.normalize(values)

        assertEquals(values.toList(), normalized.toList())
        assertFalse(normalized.any { it.name == "Gradle-Process-9011-gcTime" })
        assertEquals(
            1,
            normalized.count { it.name == "gc-gradle_gc.log-total-collections" },
        )
    }

    @Test
    fun skipsMalformedObservationsWithoutThrowing() {
        val values =
            arrayOf(
                CustomValue("gbos.schema", "1.0.0"),
                CustomValue("gbos.v1.producer.info_gradle_process.observation", "not-json"),
                CustomValue(
                    "gbos.v1.producer.info_gradle_process.observation",
                    """{"scope":"jvm.process","attributes":"broken","measurements":[]}""",
                ),
                CustomValue(
                    "gbos.v1.producer.gc_report.observation",
                    """{"scope":"jvm.gc","attributes":{"log.file.name":"gradle_gc.log"},"measurements":"nope"}""",
                ),
            )

        val normalized = normalizer.normalize(values)

        assertEquals(values.toList(), normalized.toList())
        assertFalse(normalized.any { it.name.startsWith("Gradle-Process-") })
        assertFalse(normalized.any { it.name.contains("-total-collections") })
    }

    @Test
    fun processMeasurementConsumesSynthesizedGbosValues() {
        val values =
            normalizer.normalize(
                arrayOf(
                    CustomValue("gbos.schema", "1.0.0"),
                    CustomValue(
                        "gbos.v1.producer.info_gradle_process.observation",
                        """
                        {"scope":"jvm.process","aggregationScope":"entity",
                        "attributes":{"process.pid":9011,"jvm.process.role":"gradle-daemon"},
                        "measurements":[
                          {"name":"jvm.process.memory.heap.limit","value":1073741824,"unit":"By","aggregation":"last"},
                          {"name":"jvm.process.memory.heap.used","value":536870912,"unit":"By","aggregation":"last"},
                          {"name":"jvm.process.memory.heap.committed","value":805306368,"unit":"By","aggregation":"last"},
                          {"name":"jvm.process.gc.time","value":15,"unit":"s","aggregation":"sum"},
                          {"name":"jvm.process.uptime","value":150,"unit":"s","aggregation":"last"}
                        ]}
                        """.trimIndent().replace("\n", ""),
                    ),
                ),
            )

        val parsed =
            io.github.cdsap.compare.report.measurements.parser
                .ProcessesReportParser()
                .parse(values, "Gradle")

        assertEquals("1.0 GB", parsed["Gradle-Process-max"])
        assertEquals("0.25 minutes", parsed["Gradle-Process-gcTime"])
    }
}
