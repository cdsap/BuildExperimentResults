## BuildExperimentResults
It compares build scan information for a sequence of builds executed under two different variants in a common experiment.

### Usage
```
 curl -L https://github.com/cdsap/BuildExperimentResults/releases/download/v0.4.0/build-experiment-results --output build-experiment-results
 chmod 0757 build-experiment-results

./build-experiment-results --experiment-id 154 --variants lint-4-1 --variants lint-2-1 \
    --api-key $GE_API \
    --url $GE_URL
```

* --experiment-id: experiment id tag
* --variants: name of the branches/variants of the experiment
* --requested-task: requested task in the experiment
* --api-key: Develocity Key
* --url: Develocity url

## Output

### Console
```
┌───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                                                                Experiment                                                                                                 │
├────────────────────────────┬──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Experiment id              │ 154                                                                                                                                                                          │
├────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Experiment task            │ lintDemoRelease                                                                                                                                                              │
├────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ lint-4-1                   │ Builds processed: 5                                                                                                                                                          │
├────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ lint-2-1                   │ Builds processed: 5                                                                                                                                                          │
├────────────────────────────┼────────────────────────┬─────────────────────────────────────────────────┬─────────────────────────────────────────────────┬─────────────────────────────────────────────────┤
│                            │                        │                       Mean                      │                       P50                       │                       P90                       │
│          Category          │         Metric         ├────────────────────────┬────────────────────────┼────────────────────────┬────────────────────────┼────────────────────────┬────────────────────────┤
│                            │                        │ lint-4-1               │ lint-2-1               │ lint-4-1               │ lint-2-1               │ lint-4-1               │ lint-2-1               │
│                            │                        │                        │                        │                        │                        │                        │                        │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ Build                      │ Build time             │              513368 ms │              503779 ms │              473524 ms │              497573 ms │              599878 ms │              570158 ms │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ Gradle process state       │ Gradle-Process-capacit │                  1.93  │                  1.32  │                  1.91  │                  1.33  │                  2.02  │                  1.39  │
│                            │ y                      │                        │                        │                        │                        │                        │                        │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ Gradle process state       │ Gradle-Process-gcTime  │                  0.19  │                  0.22  │                  0.18  │                  0.22  │                  0.21  │                  0.26  │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ Gradle process state       │ Gradle-Process-max     │                   4.0  │                   2.0  │                   4.0  │                   2.0  │                   4.0  │                   2.0  │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ Gradle process state       │ Gradle-Process-uptime  │                  8.49  │                  8.32  │                  7.82  │                  8.23  │                  9.93  │                  9.43  │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ Gradle process state       │ Gradle-Process-usage   │                  1.25  │                  0.77  │                  1.31  │                   0.7  │                  1.63  │                  0.93  │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ Kotlin process state       │ Kotlin-Process-capacit │                  1.31  │                  0.77  │                  1.26  │                  0.78  │                  2.19  │                  0.85  │
│                            │ y                      │                        │                        │                        │                        │                        │                        │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ Kotlin process state       │ Kotlin-Process-gcTime  │                  0.05  │                  0.05  │                  0.05  │                  0.05  │                  0.06  │                  0.08  │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ Kotlin process state       │ Kotlin-Process-max     │                   4.0  │                   2.0  │                   4.0  │                   2.0  │                   4.0  │                   2.0  │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ Kotlin process state       │ Kotlin-Process-uptime  │                  4.87  │                  4.78  │                  4.58  │                  4.61  │                  6.15  │                   5.6  │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ Kotlin process state       │ Kotlin-Process-usage   │                  0.69  │                  0.47  │                  0.71  │                  0.53  │                  0.85  │                  0.59  │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│                            │ org.jetbrains.kotlin.g │                        │                        │                        │                        │                        │                        │
│ Task Type                  │ radle.tasks.KotlinComp │                5481 ms │                5540 ms │                3539 ms │                3692 ms │               12574 ms │               12708 ms │
│                            │ ile                    │                        │                        │                        │                        │                        │                        │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│                            │ com.android.build.grad │                        │                        │                        │                        │                        │                        │
│ Task Type                  │ le.tasks.MergeSourceSe │                 132 ms │                 139 ms │                 126 ms │                 126 ms │                 254 ms │                 301 ms │
│                            │ tFolders               │                        │                        │                        │                        │                        │                        │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│                            │ com.android.build.grad │                        │                        │                        │                        │                        │                        │
│ Task Type                  │ le.tasks.MergeResource │                 286 ms │                 286 ms │                  30 ms │                  31 ms │                1052 ms │                1001 ms │
│                            │ s                      │                        │                        │                        │                        │                        │                        │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│                            │ com.android.build.grad │                        │                        │                        │                        │                        │                        │
│ Task Type                  │ le.tasks.ExtractDeepLi │                   5 ms │                   7 ms │                   5 ms │                   7 ms │                  10 ms │                  16 ms │
│                            │ nksTask                │                        │                        │                        │                        │                        │                        │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│                            │ com.android.build.grad │                        │                        │                        │                        │                        │                        │
│ Task Type                  │ le.internal.res.ParseL │                 219 ms │                 211 ms │                  16 ms │                  17 ms │                 681 ms │                 616 ms │
│                            │ ibraryResourcesTask    │                        │                        │                        │                        │                        │                        │
├────────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
...
```

### CSV
```
type,metric,mean_lint-4-1-different-process,mean_lint-2-1-different-process,mean_unit,p50_lint-4-1-different-process,p50_lint-2-1-different-process,p50_unit,p90_lint-4-1-different-process,p50_lint-2-1-different-process,p90_unit
Build,Build time,513368,503779,ms,473524,497573,ms,599878,570158,ms
Gradle process state,Gradle-Process-capacity,1.93,1.32,,1.91,1.33,,2.02,1.39,
Gradle process state,Gradle-Process-gcTime,0.19,0.22,,0.18,0.22,,0.21,0.26,
Gradle process state,Gradle-Process-max,4.0,2.0,,4.0,2.0,,4.0,2.0,
Gradle process state,Gradle-Process-uptime,8.49,8.32,,7.82,8.23,,9.93,9.43,
Gradle process state,Gradle-Process-usage,1.25,0.77,,1.31,0.7,,1.63,0.93,
Kotlin process state,Kotlin-Process-capacity,1.31,0.77,,1.26,0.78,,2.19,0.85,
Kotlin process state,Kotlin-Process-gcTime,0.05,0.05,,0.05,0.05,,0.06,0.08,
Kotlin process state,Kotlin-Process-max,4.0,2.0,,4.0,2.0,,4.0,2.0,
Kotlin process state,Kotlin-Process-uptime,4.87,4.78,,4.58,4.61,,6.15,5.6,
Kotlin process state,Kotlin-Process-usage,0.69,0.47,,0.71,0.53,,0.85,0.59,
...
```

### Html Table
Used in GitHub Action summaries
```
<table>
<tr><td colspan=8>Experiment</td></tr>
<tr><td>Task experiment</td><td colspan=7>lintDemoRelease</td></tr>
<tr><td>lint-4-1-different-process</td><td colspan=7>5 builds processed</td></tr><tr><td>lint-2-1-different-process</td><td colspan=7>5 builds processed</td></tr><tr><td rowspan=2>Category</td><td rowspan=2>Metric</td><td colspan=2>Mean</td><td colspan=2>P50</td><td colspan=2>P90</td></tr><tr><td>lint-4-1-different-process</td><td>lint-2-1-different-process</td><td>lint-4-1-different-process</td><td>lint-2-1-different-process</td><td>lint-4-1-different-process</td><td>lint-2-1-different-process</td>
</tr>
<tr><td>Build</td><td>Build time</td><td>513368 ms</td><td>503779 ms</td><td>473524 ms</td><td>497573 ms</td><td>599878 ms</td><td>570158 ms</td></tr>
<tr><td>Task Type</td><td>org.jetbrains.kotlin.gradle.tasks.KotlinCompile</td><td>5481 ms</td><td>5540 ms</td><td>3539 ms</td><td>3692 ms</td><td>12574 ms</td><td>12708 ms</td></tr>
...
</table>
```

### Metrics
The CLI generates the following metrics for the experiment
* Build time
* Duration by task type
* Duration by task path
* Processes information (Requires Info Kotlin process and/or Info Gradle process)
* Kotlin Build Reports (Requires Kotlin Build Reports)


### Complete list of parameters

| Name                  | Description                                                  | Default | Required | Example                                    |
|-----------------------|--------------------------------------------------------------|---------|----------|--------------------------------------------|
| api-key               | String token                                                 |         | Yes      | --api-key=$GE_KEY                          |
| url                   | Gradle Enterprise instance                                   |         | Yes      | --url=https://ge.acme.dev                  |
| requested-task        | Requested task in the build                                  |         | No       | --requested-task=assemble                  |
| variants              | name of the branches/variants of the experiment              |         | Yes      | --variants=main --variants=gradle_8_4      |
| experiment-id         | experiment-id (represented as tag)                           | empty   | Yes      | --experiment-id=experiment_gradle_8_4      |
| max-builds            | Max builds to be processed                                   | 500     | No       | --max-builds=600                           |
| profile               | Starting build to apply the reverse search                   | false   | No       | --profile                                  |
| build-report          | Generates build report metrics                               | true    | No       | Exclude it with --no-build-report          |
| task-path-report      | Generates task path report metrics                           | true    | No       | Exclude it with --no-task-path-report      |
| task-type-report      | Generates task type report metrics                           | true    | No       | Exclude it with --no-task-type-report      |
| process-report        | Generates process report metrics                             | false   | No       | Exclude it with --no-process-report        |
| kotlin-build-report   | Generates Kotlin build report metrics                        | true    | No       | Exclude it with --no-kotlin-build-report   |
| resource-usage-report | Generates Resource usage report metrics (requires DV 2024.2) | true    | No       | Exclude it with --no-resource-usage-report |
| warmups-to-discard    | If is a Gradle Profiler experiment, the warmups to discard   |         | No       | --exclusive-tags                           |
`
### Examples
* https://github.com/cdsap/Pagan/actions/runs/5672476887
* Using Gradle Profiler https://github.com/cdsap/Pagan/actions/runs/6382400113

### Libraries used
* picnic
* geapi-data
* clickt
* kotlin-statistics


