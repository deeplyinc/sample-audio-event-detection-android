package com.deeply.samples.eventdetection.analyzer

import java.util.*

interface AudioEventDetector {
    fun accumulate(audioSamples: FloatArray)
    fun getResults(from: Calendar?, to: Calendar?): List<AudioEvent>
}