package com.deeply.samples.eventdetection.analyzer

import java.util.*

data class AudioEvent(
    /** 감지된 오디오 이벤트의 이름. */
    val name: AudioEventType,
    /** 감지된 오디오 이벤트의 확률값 */
    val confidence: Float,
    /** 오디오 이벤트가 시작된 시간. 대략적인 시간으로, 엄밀하지 않을 수 있습니다. */
    val from: Calendar,
    /** 오디오 이벤트가 끝난 시간. 대략적인 시간으로, 엄밀하지 않을 수 있습니다. */
    val to: Calendar,
    /** 모든 분석 결과를 저장하는 변수 */
    val outputs: Map<String, Float>,
)

enum class AudioEventType {
    COUGH, 
    SNEEZE,
    NOSE_BLOWING,
    SCREAM,
    PANT,
    MOAN,
    OTHERS
}