package io.github.cdsap.compare.output.ai

import com.sun.net.httpserver.HttpServer
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress

class AiHttpClientTest {
    @Test
    fun `shared client preserves the analysis timeout and Gson JSON transport`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val payload = exchange.requestBody.use { it.readBytes() }
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        server.start()
        val client = AiHttpClient.create()
        try {
            var requestTimeout: Long? = null
            client.sendPipeline.intercept(HttpSendPipeline.Before) {
                requestTimeout = context.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis
            }
            val csvContent = "variant,time\n\"baseline\",123\n"
            val response =
                runBlocking {
                    client.post("http://127.0.0.1:${server.address.port}/") {
                        contentType(ContentType.Application.Json)
                        setBody(Role("user", csvContent))
                    }.body<Role>()
                }

            assertEquals(120_000L, requestTimeout)
            assertEquals("user", response.role)
            assertEquals(csvContent, response.content)
        } finally {
            client.close()
            server.stop(0)
        }
    }
}
