package io.github.cdsap.compare.report.measurements;

import io.github.cdsap.geapi.client.model.BuildWithResourceUsage;
import io.github.cdsap.geapi.client.model.PerformanceMetrics;

final class ResourceUsageTotals {
    private ResourceUsageTotals() {}

    static PerformanceMetrics orNull(BuildWithResourceUsage build) {
        return build.getTotal();
    }
}
