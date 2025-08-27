package io.github.cdsap.compare.output

class PromptAnalysis {
    companion object Object {
        val prompt = """
    Analyze this Gradle build performance comparison data while dynamically adapting to available metrics and highlighting significant trends.
    If certain metrics are missing (e.g., Kotlin process state, Gradle process state, total GC collections, or Kotlin Build Reports), **omit those sections entirely** from the report without leaving placeholders.
    Prioritize key differences first. The analysis should be structured as follows:

    - **1. Build Time Comparison (if available)**
      - Compare overall build times between variants (mean, P50, P90).
      - Compare configuration times between variants (mean, P50, P90).
      - Express times > 1000ms in seconds.
      - Calculate percentage differences between variants.
      - If only one variant is present, provide absolute values instead of comparisons.

    - **2. Task Type Differences (if task execution data is available)**
      - Identify the top 3 most time-consuming tasks for each variant.
      - Compare their execution times in the other variant (if applicable).
      - Show mean, P50, and P90 values for each task.
      - Highlight tasks with significant timing variations (>10% difference).

    - **3. Statistical Patterns (if variance is significant)**
      - Identify tasks with notable timing variations (>10% between variants).
      - Compare P50 and P90 values to highlight trends.
      - Indicate which variant performs better for specific task types.

    - **4. Process State Analysis (if data is available)**
      - **Kotlin Process State:** Compare memory and CPU usage across variants.
      - **Gradle Process State:** Identify differences in resource consumption.
      - Highlight trends and significant deviations.

    - **5. CPU & Memory Usage Analysis (if available)**
      - Compare CPU and memory usage between variants for:
        - All processes (overall system usage).
        - The build process (main Gradle process).
        - Build child processes.
      - Highlight significant differences (≥10%) between variants.
      - Express memory values in GB and CPU values as percentages (%).

    - **6. Garbage Collection Analysis (if total collections data is available, otherwise omit)**
      - Compare total GC collections between variants.
      - Identify potential bottlenecks in memory management.

    - **7. Kotlin Build Reports Analysis (if available, otherwise omit)**
      - **Overview of Kotlin build performance trends.**
      - **Compiler Execution Stages Comparison:**
        - Time spent before task action.
        - Compiler code analysis, IR translation, generation, and lowering.
        - IR translation line number vs. IR generation line number.
      - **Incremental Compilation Insights:**
        - Incremental compilation time vs. full compilation time.
        - Daemon connection and startup overhead.
      - **Classpath and Cache Insights:**
        - Classpath snapshot size and shrink efficiency.
        - Number of cache hits/misses when loading classpath entries.
      - **Compilation Performance Metrics:**
        - Code generation lines per second.
        - Analysis lines per second.
      - **Comparison Across Variants:**
        - Highlight major improvements or regressions.
        - Focus on areas with ≥10% variation between variants.

    - **8. Summary and Formatting Requirements**
      - **Markdown formatting** for the detailed report.
      - Tasks and task types in **quotes** for better readability.
      - **Short introductory summary (3-5 sentences) before the detailed report:**
        - Highlights the most significant findings.
        - **Express build time differences in seconds** in addition to percentage differences.
        - **Formatted for display on an overview page (not in markdown).**
        - The summary always starts with `## Summary`.
        - The detailed report starts with `## Detailed Report`.
      - Ensure observations are factual, numerical, and focus on differences.
      - **If a section has no data, omit it completely from the final report instead of including a placeholder.**
        """.trimIndent()
    }
}
