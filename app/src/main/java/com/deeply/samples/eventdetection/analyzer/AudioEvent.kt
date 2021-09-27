package com.deeply.samples.eventdetection.analyzer

import java.util.*

data class AudioEvent(
    /** 감지된 오디오 이벤트의 이름. */
    val name: String,
    /** 오디오 이벤트가 시작된 시간. 대략적인 시간으로, 엄밀하지 않을 수 있습니다. */
    val from: Calendar,
    /** 오디오 이벤트가 끝난 시간. 대략적인 시간으로, 엄밀하지 않을 수 있습니다. */
    val to: Calendar
)