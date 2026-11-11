package io.github.cdsap.compare.output

import io.github.cdsap.compare.model.removeExperimentId as removeModelExperimentId

fun String.removeExperimentId(experimentId: String?) = removeModelExperimentId(experimentId)
