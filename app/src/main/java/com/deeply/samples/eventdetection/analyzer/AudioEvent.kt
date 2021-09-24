package com.deeply.samples.eventdetection.analyzer

import java.util.*

data class AudioEvent(
    val name: String,
    val from: Calendar,
    val to: Calendar
)