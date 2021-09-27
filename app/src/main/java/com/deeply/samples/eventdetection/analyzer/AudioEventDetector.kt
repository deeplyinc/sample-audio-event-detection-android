package com.deeply.samples.eventdetection.analyzer

import java.util.*

/**
 * Raw 오디오 데이터를 분석해 오디오 이벤트를 감지하는 모듈입니다.
 *
 * 오디오 이벤트 분석을 위해서는 모델의 종류와 특성에 따라 1초, 3초, 5초, 10초 등 다양한 길이의 오디오 데이터가 필요할 수
 * 있고, 경우에 따라서는 추론 결과를 모아서 다시 후처리를 해야할 수도 있습니다. 이런 다양한 경우에 대응하기 위해 accumulate()
 * 메소드를 이용해 실시간으로 발생하는 오디오 데이터를 우선 축적하고, 내부적으로 분석할 수 있는 조건을 만족하면 자동으로 분석을 진행하여
 * 그 결과를 모듈 내에 저장해두는 방식으로 작동합니다. 이 결과는 추후에 원하는 타이밍에 getResults() 메소드를 이용해 사용할 수
 * 있습니다.
 */
interface AudioEventDetector {
    /**
     * raw audio 데이터를 축적합니다.
     */
    fun accumulate(audioSamples: FloatArray)

    /**
     * 저장된 분석 결과를 리턴합니다.
     * from, to 값에 따라 리턴하는 결과물을 제한할 수 있습니다.
     *
     * - from, to 가 모두 null 인 경우: 전체 분석 결과
     * - from 만 null 인 경우: to 이전의 모든 결과
     * - to 만 null 인 경우: from 이후의 모든 결과
     */
    fun getResults(from: Calendar?, to: Calendar?): List<AudioEvent>
}