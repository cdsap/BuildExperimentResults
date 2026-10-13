package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.network.GEClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

class GradleBuildDataRepositoryTest {
    @Test
    fun dependsOnGeClient() {
        val clientParameter =
            GradleBuildDataRepository::class
                .primaryConstructor!!
                .parameters
                .single { it.name == "client" }

        assertEquals(GEClient::class, clientParameter.type.classifier)
    }

    @Test
    fun implementsBuildDataRepository() {
        assertTrue(BuildDataRepository::class.java.isAssignableFrom(GradleBuildDataRepository::class.java))
    }
}
