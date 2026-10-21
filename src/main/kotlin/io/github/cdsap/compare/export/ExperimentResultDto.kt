package io.github.cdsap.compare.export

const val EXPERIMENT_RESULT_SCHEMA_VERSION = "1.0.0"
const val PRODUCER_NAME = "BuildExperimentResults"
const val PRODUCER_VERSION = "1.0-SNAPSHOT"

data class ExperimentResultDto(
    val schemaVersion: String,
    val generatedAt: String,
    val producer: ProducerDto,
    val report: ReportIdentityDto,
    val experiment: ExperimentIdentityDto,
    val execution: ExecutionDto,
    val variants: List<VariantDto>,
    val metrics: List<MetricDto>,
    val datasets: DatasetsDto,
    val diagnostics: DiagnosticsDto,
    val warnings: List<WarningDto>,
    val completeness: CompletenessDto,
)

data class ProducerDto(
    val name: String,
    val version: String,
    val revision: String?,
)

data class ReportIdentityDto(
    val id: String,
    val kind: String,
    val title: String?,
    val status: String,
)

data class ExperimentIdentityDto(
    val id: String,
    val repository: String?,
    val requestedTasks: List<String>,
    val mode: String?,
    val isProfiler: Boolean,
)

data class ExecutionDto(
    val source: String,
    val runId: String?,
    val runAttempt: Int?,
    val workflowName: String?,
    val startedAt: String?,
    val links: Map<String, String>,
)

data class VariantDto(
    val id: String,
    val label: String,
    val sourceTag: String,
    val role: String,
    val buildCount: Int,
)

data class MetricDto(
    val id: String,
    val family: String,
    val category: String,
    val name: String,
    val unit: String,
    val valueType: String,
    val direction: String?,
    val scope: Map<String, String>?,
    val aggregation: AggregationDto,
    val values: Map<String, StatisticDto>,
)

data class AggregationDto(
    val sampleUnit: String,
    val method: String,
)

data class StatisticDto(
    val mean: Double?,
    val p50: Double?,
    val p90: Double?,
    val count: Int?,
)

data class DatasetsDto(
    val builds: Map<String, List<BuildObservationDto>>,
    val taskSeries: Map<String, TaskSeriesDto>,
    val resourceSeries: Map<String, NamedSeriesDto>,
    val processSeries: Map<String, NamedSeriesDto>,
    val gcSeries: Map<String, NamedSeriesDto>,
    val kotlinBuildReportSeries: Map<String, NamedSeriesDto>,
)

data class BuildObservationDto(
    val observationId: String,
    val sequence: Int,
    val durationMs: Double,
    val configurationMs: Double,
    val startedAt: String?,
    val scanUrl: String?,
)

data class TaskSeriesDto(
    val path: String,
    val taskType: String?,
    val observations: Map<String, List<TaskObservationDto>>,
)

data class TaskObservationDto(
    val observationId: String,
    val sequence: Int,
    val durationMs: Double,
    val outcome: String?,
)

data class NamedSeriesDto(
    val name: String,
    val unit: String,
    val observations: Map<String, List<NumericObservationDto>>,
)

data class NumericObservationDto(
    val observationId: String,
    val sequence: Int,
    val value: Double?,
)

data class DiagnosticsDto(
    val status: String,
    val source: DiagnosticsSourceDto?,
    val groups: List<DiagnosticsGroupDto>,
)

data class DiagnosticsSourceDto(
    val tool: String,
    val schemaVersion: String?,
    val experimentRunId: String?,
    val sourceRepo: String?,
)

data class DiagnosticsGroupDto(
    val id: String,
    val family: String,
    val sampleUnit: String,
    val metricIds: List<String>,
    val jobKeysByVariant: Map<String, List<String>>,
)

data class WarningDto(
    val code: String,
    val severity: String,
    val source: String,
    val message: String,
    val variantId: String? = null,
    val metricId: String? = null,
)

data class CompletenessDto(
    val requestedVariantIds: List<String>,
    val observedVariantIds: List<String>,
    val missingMetricIds: List<String>,
    val diagnostics: String,
)
