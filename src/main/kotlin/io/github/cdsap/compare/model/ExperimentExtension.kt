package io.github.cdsap.compare.model

fun String.removeExperimentId(experimentId: String?) = if (experimentId == null) this else this.replace("${experimentId}_", "")
