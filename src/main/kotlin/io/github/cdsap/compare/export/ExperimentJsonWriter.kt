package io.github.cdsap.compare.export

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class ExperimentJsonWriter(
    private val validation: ExportValidation = ExportValidation(),
    private val gson: Gson =
        GsonBuilder()
            .serializeNulls()
            .create(),
) {
    fun write(
        result: ExperimentResultDto,
        path: Path,
    ) {
        validation.validate(result)
        val parent = path.toAbsolutePath().parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        val json = gson.toJson(result)
        val temporary = path.resolveSibling("${path.fileName}.tmp")
        Files.write(temporary, json.toByteArray(StandardCharsets.UTF_8))
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
