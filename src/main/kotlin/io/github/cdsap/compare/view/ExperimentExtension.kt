package io.github.cdsap.compare.view

fun String.removeExperimentId(experimentId: String?) = if (experimentId == null) this else this.replace("${experimentId}_", "")
