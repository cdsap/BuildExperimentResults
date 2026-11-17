package io.github.cdsap.compare.report.measurements.gbos

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.CustomValue
import kotlin.math.roundToLong

/**
 * When GBOS producers replace legacy Develocity custom values, synthesize the
 * legacy names BuildExperimentResults already aggregates and charts.
 *
 * Prefer legacy values when both are present (e.g. older GCReport dual-publish).
 */
class GbosCustomValuesNormalizer {
    fun normalize(values: Array<CustomValue>): Array<CustomValue> {
        if (!GbosObservability.isEnabled(values)) return values

        val synthesized = mutableListOf<CustomValue>()
        if (!GbosObservability.hasLegacyGradleProcess(values) &&
            GbosObservability.hasProducer(values, GbosObservability.PRODUCER_INFO_GRADLE)
        ) {
            synthesized +=
                synthesizeProcess(
                    values = values,
                    producerSlug = GbosObservability.PRODUCER_INFO_GRADLE,
                    legacyPrefix = "Gradle",
                    expectedRole = "gradle-daemon",
                )
        }
        if (!GbosObservability.hasLegacyKotlinProcess(values) &&
            GbosObservability.hasProducer(values, GbosObservability.PRODUCER_INFO_KOTLIN)
        ) {
            synthesized +=
                synthesizeProcess(
                    values = values,
                    producerSlug = GbosObservability.PRODUCER_INFO_KOTLIN,
                    legacyPrefix = "Kotlin",
                    expectedRole = "kotlin-daemon",
                )
        }
        if (!GbosObservability.hasLegacyGcReport(values) &&
            GbosObservability.hasProducer(values, GbosObservability.PRODUCER_GC_REPORT)
        ) {
            synthesized += synthesizeGc(values)
        }

        if (synthesized.isEmpty()) return values
        return values + synthesized.toTypedArray()
    }

    fun normalizeInPlace(builds: Iterable<BuildWithResourceUsage>) {
        builds.forEach { build ->
            build.values = normalize(build.values)
        }
    }

    private fun synthesizeProcess(
        values: Array<CustomValue>,
        producerSlug: String,
        legacyPrefix: String,
        expectedRole: String,
    ): List<CustomValue> {
        val observationName = GbosObservability.observationName(producerSlug)
        val result = mutableListOf<CustomValue>()
        values.filter { it.name == observationName }.forEach { customValue ->
            val json = parseObject(customValue.value) ?: return@forEach
            val attributes = jsonObjectMember(json, "attributes") ?: return@forEach
            val role = textAttribute(attributes, "jvm.process.role")
            if (role != null && role != expectedRole) return@forEach
            val pid = numberAttribute(attributes, "process.pid")?.roundToLong()?.toString() ?: return@forEach
            val measurements = measurementMap(json)

            bytesToGib(measurements["jvm.process.memory.heap.limit"])?.let {
                result += CustomValue("$legacyPrefix-Process-$pid-max", "$it GB")
            }
            bytesToGib(measurements["jvm.process.memory.heap.used"])?.let {
                result += CustomValue("$legacyPrefix-Process-$pid-usage", "$it GB")
            }
            bytesToGib(measurements["jvm.process.memory.heap.committed"])?.let {
                result += CustomValue("$legacyPrefix-Process-$pid-capacity", "$it GB")
            }
            secondsToMinutes(measurements["jvm.process.gc.time"])?.let {
                result += CustomValue("$legacyPrefix-Process-$pid-gcTime", "$it minutes")
            }
            secondsToMinutes(measurements["jvm.process.uptime"])?.let {
                result += CustomValue("$legacyPrefix-Process-$pid-uptime", "$it minutes")
            }
            textAttribute(attributes, "jvm.gc.name")?.let { gcName ->
                result += CustomValue("$legacyPrefix-Process-$pid-gcType", gcName)
            }
        }
        return result
    }

    private fun synthesizeGc(values: Array<CustomValue>): List<CustomValue> {
        val observationName = GbosObservability.observationName(GbosObservability.PRODUCER_GC_REPORT)
        val byLog = linkedMapOf<String, MutableMap<String, Double>>()
        values.filter { it.name == observationName }.forEach { customValue ->
            val json = parseObject(customValue.value) ?: return@forEach
            val attributes = jsonObjectMember(json, "attributes") ?: return@forEach
            val logFile = textAttribute(attributes, "log.file.name") ?: return@forEach
            val action = textAttribute(attributes, "jvm.gc.action") ?: return@forEach
            val count = measurementMap(json)["jvm.gc.events"] ?: return@forEach
            val actions = byLog.getOrPut(logFile) { linkedMapOf() }
            actions[action] = (actions[action] ?: 0.0) + count
        }

        val result = mutableListOf<CustomValue>()
        byLog.forEach { (logFile, actions) ->
            val total = actions.values.sum().roundToLong()
            if (total > 0) {
                result += CustomValue("gc-$logFile-total-collections", "$total")
            }
            actions.toSortedMap().forEach { (action, count) ->
                result += CustomValue("gc-$logFile-$action", "${count.roundToLong()}")
            }
        }
        return result
    }

    private fun measurementMap(observation: JsonObject): Map<String, Double> {
        val measurements = observation.get("measurements")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyMap()
        val result = linkedMapOf<String, Double>()
        measurements.forEach { element ->
            if (!element.isJsonObject) return@forEach
            val measurement = element.asJsonObject
            val name =
                measurement
                    .get("name")
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asString
                    ?: return@forEach
            val value = primitiveNumber(measurement.get("value")) ?: return@forEach
            result[name] = value
        }
        return result
    }

    private fun parseObject(raw: String): JsonObject? =
        runCatching {
            JsonParser.parseString(raw).takeIf { it.isJsonObject }?.asJsonObject
        }.getOrNull()

    private fun jsonObjectMember(
        parent: JsonObject,
        name: String,
    ): JsonObject? = parent.get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun textAttribute(
        attributes: JsonObject,
        name: String,
    ): String? {
        val element = attributes.get(name) ?: return null
        return when {
            element.isJsonPrimitive && element.asJsonPrimitive.isString -> element.asString
            element.isJsonPrimitive -> element.asString
            else -> null
        }
    }

    private fun numberAttribute(
        attributes: JsonObject,
        name: String,
    ): Double? = primitiveNumber(attributes.get(name))

    private fun primitiveNumber(element: com.google.gson.JsonElement?): Double? {
        if (element == null || !element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        return when {
            primitive.isNumber -> primitive.asDouble
            primitive.isString -> primitive.asString.toDoubleOrNull()
            else -> null
        }
    }

    private fun bytesToGib(bytes: Double?): Double? {
        if (bytes == null || !bytes.isFinite()) return null
        return ((bytes / BYTES_PER_GIB) * 100.0).roundToLong() / 100.0
    }

    private fun secondsToMinutes(seconds: Double?): Double? {
        if (seconds == null || !seconds.isFinite()) return null
        return ((seconds / SECONDS_PER_MINUTE) * 100.0).roundToLong() / 100.0
    }

    private companion object {
        private const val BYTES_PER_GIB = 1_073_741_824.0
        private const val SECONDS_PER_MINUTE = 60.0
    }
}
