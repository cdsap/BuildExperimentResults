package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.network.GEClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

class DevelocityBuildDataSourceTest {
    @Test
    fun dependsOnGeClient() {
        val clientParameter =
            DevelocityBuildDataSource::class
                .primaryConstructor!!
                .parameters
                .single { it.name == "client" }

        assertEquals(GEClient::class, clientParameter.type.classifier)
    }

    @Test
    fun implementsBuildDataSource() {
        assertTrue(BuildDataSource::class.java.isAssignableFrom(DevelocityBuildDataSource::class.java))
    }
}
