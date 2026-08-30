package io.github.cdsap.compare.output.ai

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiTransportDtoTest {
    private val gson = Gson()

    @Test
    fun `OpenAI and Claude transport DTOs live in the output ai package`() {
        assertEquals("io.github.cdsap.compare.output.ai", OpenAiRequest::class.java.packageName)
        assertEquals("io.github.cdsap.compare.output.ai", OpenAiResponse::class.java.packageName)
        assertEquals("io.github.cdsap.compare.output.ai", ClaudeRequest::class.java.packageName)
        assertEquals("io.github.cdsap.compare.output.ai", ClaudeResponse::class.java.packageName)
    }

    @Test
    fun `OpenAI request serializes with provider field names`() {
        val request =
            OpenAiRequest(
                model = "gpt-4-turbo",
                messages =
                    arrayOf(
                        Role("system", "Analyze builds"),
                        Role("user", "csv-payload"),
                    ),
                temperature = 0.3,
            )

        val json = JsonParser.parseString(gson.toJson(request)).asJsonObject

        assertEquals("gpt-4-turbo", json.get("model").asString)
        assertEquals(0.3, json.get("temperature").asDouble, 0.0)
        val messages = json.getAsJsonArray("messages")
        assertEquals(2, messages.size())
        assertEquals("system", messages[0].asJsonObject.get("role").asString)
        assertEquals("Analyze builds", messages[0].asJsonObject.get("content").asString)
        assertEquals("user", messages[1].asJsonObject.get("role").asString)
        assertEquals("csv-payload", messages[1].asJsonObject.get("content").asString)
    }

    @Test
    fun `OpenAI response parses provider payload fields`() {
        val payload =
            """
            {
              "id": "chatcmpl-1",
              "object": "chat.completion",
              "created": 1710000000,
              "model": "gpt-4-turbo",
              "choices": [
                {
                  "index": 0,
                  "message": { "role": "assistant", "content": "analysis text" },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 10,
                "completion_tokens": 20,
                "total_tokens": 30
              }
            }
            """.trimIndent()

        val response = gson.fromJson(payload, OpenAiResponse::class.java)

        assertEquals("chatcmpl-1", response.id)
        assertEquals("chat.completion", response.`object`)
        assertEquals(1710000000L, response.created)
        assertEquals("gpt-4-turbo", response.model)
        assertEquals(1, response.choices.size)
        assertEquals(0, response.choices[0].index)
        assertEquals("assistant", response.choices[0].message.role)
        assertEquals("analysis text", response.choices[0].message.content)
        assertEquals("stop", response.choices[0].finish_reason)
        assertEquals(10, response.usage.prompt_tokens)
        assertEquals(20, response.usage.completion_tokens)
        assertEquals(30, response.usage.total_tokens)
    }

    @Test
    fun `Claude request serializes with provider field names`() {
        val request =
            ClaudeRequest(
                model = "claude-sonnet-4-20250514",
                max_tokens = 4096,
                system = "Analyze builds",
                messages = arrayOf(ClaudeMessage("user", "csv-payload")),
                temperature = 0.3,
            )

        val json = JsonParser.parseString(gson.toJson(request)).asJsonObject

        assertEquals("claude-sonnet-4-20250514", json.get("model").asString)
        assertEquals(4096, json.get("max_tokens").asInt)
        assertEquals("Analyze builds", json.get("system").asString)
        assertEquals(0.3, json.get("temperature").asDouble, 0.0)
        val messages = json.getAsJsonArray("messages")
        assertEquals(1, messages.size())
        assertEquals("user", messages[0].asJsonObject.get("role").asString)
        assertEquals("csv-payload", messages[0].asJsonObject.get("content").asString)
    }

    @Test
    fun `Claude response parses provider payload fields`() {
        val payload =
            """
            {
              "id": "msg_1",
              "type": "message",
              "role": "assistant",
              "content": [
                { "type": "text", "text": "analysis text" }
              ],
              "model": "claude-sonnet-4-20250514",
              "stop_reason": "end_turn",
              "usage": {
                "input_tokens": 11,
                "output_tokens": 22
              }
            }
            """.trimIndent()

        val response = gson.fromJson(payload, ClaudeResponse::class.java)

        assertEquals("msg_1", response.id)
        assertEquals("message", response.type)
        assertEquals("assistant", response.role)
        assertEquals(1, response.content.size)
        assertEquals("text", response.content[0].type)
        assertEquals("analysis text", response.content[0].text)
        assertEquals("claude-sonnet-4-20250514", response.model)
        assertEquals("end_turn", response.stop_reason)
        assertEquals(11, response.usage.input_tokens)
        assertEquals(22, response.usage.output_tokens)
    }

    @Test
    fun `model package does not expose OpenAI or Claude transport DTO class names`() {
        val modelPackage = "io.github.cdsap.compare.model"
        val forbidden =
            listOf(
                "OpenAiRequest",
                "OpenAiResponse",
                "ClaudeRequest",
                "ClaudeResponse",
            )

        forbidden.forEach { className ->
            val found =
                runCatching {
                    Class.forName("$modelPackage.$className")
                }.isSuccess
            assertTrue(!found, "Expected $modelPackage.$className to be absent")
        }
    }
}
