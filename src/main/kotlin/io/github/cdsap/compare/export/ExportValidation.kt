package io.github.cdsap.compare.export

class ExportValidation {
    fun validate(result: ExperimentResultDto) {
        val errors = mutableListOf<String>()
        val major = result.schemaVersion.substringBefore('.').toIntOrNull()
        if (major != 1) {
            errors += "unsupported schemaVersion '${result.schemaVersion}'"
        }
        if (result.generatedAt.isBlank()) errors += "generatedAt is required"
        if (result.producer.name.isBlank()) errors += "producer.name is required"
        if (result.report.id.isBlank() || !ID.matches(result.report.id)) {
            errors += "report.id '${result.report.id}' is not a sanitized slug"
        }
        if (result.variants.isEmpty()) errors += "variants must not be empty"

        val variantIds = result.variants.map { it.id }
        if (variantIds.size != variantIds.distinct().size) errors += "variant ids must be unique"
        variantIds.filterNot { ID.matches(it) }.forEach { errors += "variant id '$it' is not a sanitized slug" }

        val metricIds = result.metrics.map { it.id }
        if (metricIds.size != metricIds.distinct().size) errors += "metric ids must be unique"
        result.metrics.forEach { metric ->
            metric.values.keys.filterNot { it in variantIds }.forEach { key ->
                errors += "metric '${metric.id}' references unknown variant '$key'"
            }
            metric.values.values.forEach { statistic ->
                listOf(statistic.mean, statistic.p50, statistic.p90).forEach { value ->
                    if (value != null && !value.isFinite()) {
                        errors += "metric '${metric.id}' contains a non-finite statistic"
                    }
                }
            }
        }

        collectStrings(result).forEach { value ->
            if (USERINFO.containsMatchIn(value)) {
                errors += "exported JSON must not contain credential userinfo in URLs"
            }
            if (SECRET_HINT.containsMatchIn(value)) {
                errors += "exported JSON must not contain secrets"
            }
        }

        if (errors.isNotEmpty()) {
            throw IllegalArgumentException("Experiment result JSON failed validation: ${errors.joinToString("; ")}")
        }
    }

    private fun collectStrings(result: ExperimentResultDto): List<String> {
        val values = mutableListOf<String>()
        values += result.report.id
        values += listOfNotNull(result.report.title, result.experiment.repository)
        values += result.execution.links.values
        result.warnings.forEach { values += it.message }
        result.datasets.builds.values.flatten().forEach { observation ->
            values += listOfNotNull(observation.scanUrl, observation.startedAt)
        }
        return values
    }

    private companion object {
        private val ID = Regex("^[A-Za-z0-9._-]+$")
        private val USERINFO = Regex("https?://[^/\\s]+@")
        private val SECRET_HINT = Regex("(?i)(api[_-]?key\\s*[:=]|secret\\s*[:=]|password\\s*[:=]|bearer\\s+\\S+)")
    }
}
