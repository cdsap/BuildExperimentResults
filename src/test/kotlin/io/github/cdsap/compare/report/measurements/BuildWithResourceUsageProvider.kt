package io.github.cdsap.compare.report.measurements

import io.github.cdsap.geapi.client.model.Metric
import io.github.cdsap.geapi.client.model.PerformanceMetrics

class BuildWithResourceUsageProvider {
    fun get(): PerformanceMetrics {
        val metric =
            Metric(
                average = 50L,
                median = 51L,
                max = 100L,
                p25 = 25L,
                p75 = 75L,
                p95 = 95L
            )

        return PerformanceMetrics(
            buildProcessCpu = metric,
            allProcessesCpu = metric,
            allProcessesMemory = metric,
            buildChildProcessesCpu = metric,
            buildChildProcessesMemory = metric,
            buildProcessMemory = metric,
            diskReadThroughput = metric,
            diskWriteThroughput = metric,
            networkUploadThroughput = metric,
            networkDownloadThroughput = metric
        )
    }
}
