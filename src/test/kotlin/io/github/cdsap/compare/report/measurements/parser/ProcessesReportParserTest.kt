package io.github.cdsap.compare.report.measurements.parser

import com.google.gson.Gson
import io.github.cdsap.geapi.client.model.BuildWithResourceUsage
import io.github.cdsap.geapi.client.model.CustomValue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader

class ProcessesReportParserTest {
    @Test
    fun testProcessesAreCorrectedParsedWithTagGradle() {
        val parser = ProcessesReportParser()
        val customValues = Gson().fromJson(jsonCustomValues, Array<CustomValue>::class.java)
        val result = parser.parse(customValues, "Gradle")

        assertTrue(result.size == 6)
        assertTrue(result.containsKey("Gradle-Process-capacity"))
        assertTrue(result["Gradle-Process-max"] == "4.0 GB")
    }

    @Test
    fun testProcessesAreCorrectedParsedWithTagKotlin() {
        val parser = ProcessesReportParser()
        val customValues = Gson().fromJson(jsonCustomValues, Array<CustomValue>::class.java)
        val result = parser.parse(customValues, "Kotlin")

        assertTrue(result.size == 6)
        assertTrue(result.containsKey("Kotlin-Process-uptime"))
        assertTrue(result["Kotlin-Process-gcType"] == "-XX:+UseG1GC")
    }

    @Test
    fun testProcessesAreCorrectedParsedWithoutKnownTag() {
        val parser = ProcessesReportParser()
        val customValues = Gson().fromJson(jsonCustomValues, Array<CustomValue>::class.java)
        val result = parser.parse(customValues, "Scala")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testParseProcessesFromBuildsReturnsAList() {
        val variantABuilds: List<BuildWithResourceUsage> = Gson().fromJson(BufferedReader(InputStreamReader(javaClass.classLoader.getResourceAsStream("varianta.json"))).readText(), Array<BuildWithResourceUsage>::class.java).toList()

        val parser = ProcessesReportParser()
        val result = parser.parseByVariant(variantABuilds, "Kotlin")
        assertTrue(result.isNotEmpty())
        assertTrue(result.containsKey("Kotlin-Process-uptime"))
        assertTrue(result["Kotlin-Process-uptime"]?.size == 5)
        assertTrue(result["Kotlin-Process-uptime"]?.contains("6.15 minutes")!!)
        assertTrue(result["Kotlin-Process-uptime"]?.contains("4.58 minutes")!!)
        assertTrue(result["Kotlin-Process-uptime"]?.contains("4.42 minutes")!!)
    }
}

val jsonCustomValues = """
[
{
    "name": "AnotherEelement",
    "value": "develop"
},
{
    "name": "Gradle-Process-1878-capacity",
    "value": "1.89 GB"
},
{
    "name": "Gradle-Process-1878-gcTime",
    "value": "0.18 minutes"
},
{
    "name": "Gradle-Process-1878-gcType",
    "value": "-XX:+UseParallelGC"
},
{
    "name": "Gradle-Process-1878-max",
    "value": "4.0 GB"
},
{
    "name": "Gradle-Process-1878-uptime",
    "value": "7.67 minutes"
},
{
    "name": "Gradle-Process-1878-usage",
    "value": "0.83 GB"
},
{
    "name": "Kotlin-Process-2000-capacity",
    "value": "0.25 GB"
},
{
    "name": "Kotlin-Process-2000-gcTime",
    "value": "0.0 minutes"
},
{
    "name": "Kotlin-Process-2000-gcType",
    "value": "-XX:+UseG1GC"
},
{
    "name": "Kotlin-Process-2000-max",
    "value": "4.0 GB"
},
{
    "name": "Kotlin-Process-2000-uptime",
    "value": "5.94 minutes"
},
{
    "name": "Kotlin-Process-2000-usage",
    "value": "0.16 GB"
},
{
    "name": "Kotlin-Process-2214-capacity",
    "value": "1.26 GB"
},
{
    "name": "Kotlin-Process-2214-gcTime",
    "value": "0.04 minutes"
},
{
    "name": "Kotlin-Process-2214-gcType",
    "value": "-XX:+UseG1GC"
},
{
    "name": "Kotlin-Process-2214-max",
    "value": "4.0 GB"
},
{
    "name": "Kotlin-Process-2214-uptime",
    "value": "4.42 minutes"
},
{
    "name": "Kotlin-Process-2214-usage",
    "value": "0.46 GB"
}]
"""
