package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.network.GEClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

class DevelocityBuildDataProviderTest {
    @Test
    fun dependsOnGeClient() {
        val clientParameter =
            DevelocityBuildDataProvider::class
                .primaryConstructor!!
                .parameters
                .single { it.name == "client" }

        assertEquals(GEClient::class, clientParameter.type.classifier)
    }

    @Test
    fun implementsBuildDataProvider() {
        assertTrue(BuildDataProvider::class.java.isAssignableFrom(DevelocityBuildDataProvider::class.java))
    }
}
