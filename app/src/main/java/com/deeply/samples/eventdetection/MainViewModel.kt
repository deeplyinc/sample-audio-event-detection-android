package com.deeply.samples.eventdetection

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deeply.samples.eventdetection.analyzer.AudioEvent
import com.deeply.samples.eventdetection.analyzer.AudioEventDetector
import com.deeply.samples.eventdetection.analyzer.HomeAudioEventDetector
import com.deeply.samples.eventdetection.analyzer.SampleAudioEventDetector
import com.deeply.samples.eventdetection.recorder.DeeplyRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * 녹음 모듈. AudioRecord 를 Kotlin Flow 로 사용할 수 있도록 감싼 wrapper 로, 이 모듈을 사용하지 않아도 상관없습니다.
     * AudioEventDetector 가 FloatArray 형태의 raw audio 를 인풋으로 받는 형태이기 때문에 이것만 맞춰주시면 됩니다.
     */
    private val recorder = DeeplyRecorder(bufferSize = 48000)
    /**
     * 녹음된 raw audio 데이터 기반으로 오디오 이벤트를 분석, 감지하는 모듈. accumulate() 함수를 이용해 raw audio 를
     * 인풋으로 받고, 충분한 오디오 데이터가 모일 경우 분석을 진행하여 그 결과를 모듈 내부에서 관리합니다. getResults() 함수를
     * 이용해 분석 결과를 List<AudioEvent> 형태로 얻을 수 있습니다.
     */
//    private val detector: AudioEventDetector = SampleAudioEventDetector(application)
    private val detector: AudioEventDetector = HomeAudioEventDetector(application)

    fun startAnalyzing() {
        if (!recorder.isRecording()) {
            viewModelScope.launch(Dispatchers.Default) {
                if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    // 녹음 시작, raw audio (audioSamples) 획득
                    recorder.start().collect { audioSamples ->
                        Log.d(TAG, "analyze() - audioSamples.size: ${audioSamples.size}")
                        // raw audio 데이터를 detector 에 축적
                        detector.accumulate(audioSamples.map { it.toFloat() }.toFloatArray())

                        // 분석에 필요한 충분한 raw audio 데이터가 축적될 경우 detector 모듈이 자동으로 분석.
                        // 분석 결과는 detector 모듈 내부에 따로 저장하고 있다가 getResults() 함수가 호출되면 리턴
                    }
                }
            }
        }
    }

    fun stopAnalyzing() {
        if (recorder.isRecording()) {
            recorder.stop()
        }
    }

    fun isAnalyzing() = recorder.isRecording()

    fun getResult(from: Calendar?, to: Calendar?): List<AudioEvent> {
        // 주어진 시간대(from - to)의 분석 결과를 얻는 함수.
        val result = detector.getResults(from, to)
        Log.d(TAG, "getResult() - ${result.size} results: $result")
        return result
    }

    companion object {
        const val TAG = "MainViewModel"
    }
}