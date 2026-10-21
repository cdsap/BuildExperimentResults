package io.github.cdsap.compare.export

internal fun finiteNumber(value: Any?): Double? {
    val number =
        when (value) {
            null -> return null
            is Number -> value.toDouble()
            is String -> parseNumericString(value)
            else -> return null
        } ?: return null
    return number.takeIf { it.isFinite() }
}

private fun parseNumericString(value: String): Double? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val token = trimmed.replace(",", "").split(Regex("\\s+")).firstOrNull().orEmpty()
    return token.toDoubleOrNull()
}
