package io.github.cdsap.compare.report

import io.github.cdsap.compare.export.CanonicalJsonExporter

/**
 * Compatibility name for callers that used the report-layer export port.
 * New report orchestration depends on [CanonicalJsonExporter] directly.
 */
@Deprecated("Use CanonicalJsonExporter")
interface ExperimentResultExporter : CanonicalJsonExporter
