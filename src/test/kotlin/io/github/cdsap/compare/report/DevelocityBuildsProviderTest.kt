package io.github.cdsap.compare.report

import io.github.cdsap.geapi.client.repository.GradleEnterpriseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

class DevelocityBuildsProviderTest {
    @Test
    fun dependsOnGradleEnterpriseRepositoryPort() {
        val repositoryParameter =
            DevelocityBuildsProvider::class
                .primaryConstructor!!
                .parameters
                .single { it.name == "repository" }

        assertEquals(GradleEnterpriseRepository::class, repositoryParameter.type.classifier)
    }

    @Test
    fun implementsBuildsProvider() {
        assertTrue(BuildsProvider::class.java.isAssignableFrom(DevelocityBuildsProvider::class.java))
    }
}
