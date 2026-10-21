package io.github.cdsap.compare.export

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class SchemaContractTest {
    @Test
    fun exampleContainsEveryRequiredRootFieldFromSchema() {
        val schema = JsonParser.parseString(File("schema/experiment-result.v1.json").readText()).asJsonObject
        val example = JsonParser.parseString(File("schema/examples/experiment-result.example.json").readText()).asJsonObject
        val required = schema.getAsJsonArray("required").map { it.asString }

        required.forEach { field ->
            assertTrue(example.has(field), "example is missing required field $field")
        }
        assertEquals("1.0.0", example.get("schemaVersion").asString)
        assertTrue(
            example.getAsJsonArray("metrics")[0].asJsonObject.getAsJsonObject("values")
                .getAsJsonObject("kgp-2.4.20").get("mean").asJsonPrimitive.isNumber,
        )
        ExportValidation().validate(
            com.google.gson.Gson().fromJson(example, ExperimentResultDto::class.java),
        )
    }
}
