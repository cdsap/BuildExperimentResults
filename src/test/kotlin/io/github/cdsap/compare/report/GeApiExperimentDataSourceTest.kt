package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.network.GEClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

class GeApiExperimentDataSourceTest {
    @Test
    fun dependsOnGeClient() {
        val clientParameter =
            GeApiExperimentDataSource::class
                .primaryConstructor!!
                .parameters
                .single { it.name == "client" }

        assertEquals(GEClient::class, clientParameter.type.classifier)
    }

    @Test
    fun implementsExperimentDataSource() {
        assertTrue(ExperimentDataSource::class.java.isAssignableFrom(GeApiExperimentDataSource::class.java))
    }
}
