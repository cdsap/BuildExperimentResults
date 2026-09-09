package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.repository.GradleEnterpriseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

class GradleBuildsProviderTest {
    @Test
    fun dependsOnGradleEnterpriseRepositoryPort() {
        val repositoryParameter =
            GradleBuildsProvider::class
                .primaryConstructor!!
                .parameters
                .single { it.name == "repository" }

        assertEquals(GradleEnterpriseRepository::class, repositoryParameter.type.classifier)
    }
}
