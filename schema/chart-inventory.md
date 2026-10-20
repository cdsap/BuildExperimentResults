# Chart and metric inventory for experiment-result JSON

Source inspected: `ChartGenerator`, `ChartDataGenerator`, `MeasurementsByReport`, and diagnostics import. This mapping is the v1 observation contract. Presentation details (Chart.js types, colors, DOM ids) stay out of JSON.

| Displayed chart / table | Source | Needs per-observation values | JSON destination |
|---|---|---|---|
| Build Duration Time Series | `BuildWithResourceUsage.buildDuration` (ms) | Yes | `datasets.builds[].durationMs`; metric `build.duration` |
| Configuration Time | `BuildWithResourceUsage.configuration` (ms) | Yes | `datasets.builds[].configurationMs`; metric `build.configuration` |
| Build Process Memory | `total.buildProcessMemory.max` (raw long; HTML axis currently labeled MB) | Yes | `datasets.resourceSeries` unit `bytes`; table metric keeps existing BER `Gb` aggregates |
| Build Child Processes Memory | `total.buildChildProcessesMemory.max` | Yes | `datasets.resourceSeries` unit `bytes` |
| Resource usage table rows | `ResourceUsageMeasurement` mean/P50/P90 | No (aggregates exist) | `metrics` family `resource` |
| Task Duration picker | Common `taskExecution` paths across variants, duration per build | Yes | `datasets.taskSeries`; metrics `task.path.*` |
| Task type table | `TasksTypeMeasurements` | No | `metrics` family `task` |
| Kotlin GC line | Custom values `Kotlin-Process-*-gcTime` (minutes) | Yes | `datasets.processSeries` |
| Gradle GC line | Custom values `Gradle-Process-*-gcTime` | Yes | `datasets.processSeries` |
| Other process table rows | `ProcessMeasurement` | Optional | `metrics` family `process`; process custom values also in `processSeries` |
| Kotlin build report charts | Parsed Performance blobs, `KotlinBuildReportsConstants.ALLOWED_LIST` | Yes (per-build median of matching entries, matching current HTML) | `datasets.kotlinBuildReportSeries` |
| GC collections | Custom values `*-total-collections` when `--gc-report` | Yes | `datasets.gcSeries` |
| CPU Diagnostics | EDR package via `--diagnostics-package`, pooled aggregates | No — do not invent a timeline | `metrics` family `cpu-diagnostics`; `diagnostics.groups` |
| JFR | EDR package, pooled aggregates | No — do not invent a timeline | `metrics` family `jfr`; `diagnostics.groups` |

## Sample semantics

- Build, configuration, task, resource, process, Kotlin, and GC series use `sampleUnit: build` (or `task-invocation` for task metrics).
- CPU/JFR use `sampleUnit: diagnostic-sample` and keep EDR `jobKeys`. They are not paired with Develocity builds by sequence.
- HTML currently labels build duration as seconds while values are milliseconds; JSON keeps milliseconds.

## Public-data allowlist (v1)

Included: variant tags, task paths/types/outcomes, numeric metrics, build ids, build start timestamps, requested tasks, Develocity scan URLs of the form `{url}/s/{id}`, GitHub workflow URLs derived from repository + run id.

Excluded: API keys, LLM keys, environment/user/host fields, cache keys, raw Develocity models, local filesystem paths in warnings.
