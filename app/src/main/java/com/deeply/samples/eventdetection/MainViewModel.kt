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
import com.deeply.samples.eventdetection.analyzer.SampleAudioEventDetector
import com.deeply.samples.eventdetection.recorder.DeeplyRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val recorder = DeeplyRecorder(bufferSize = 16000)
    private val detector: AudioEventDetector = SampleAudioEventDetector(application)

    fun startAnalyzing() {
        if (!recorder.isRecording()) {
            viewModelScope.launch(Dispatchers.Default) {
                if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    recorder.start().collect { audioSamples ->
                        Log.d(TAG, "analyze() - audioSamples.size: ${audioSamples.size}")
                        detector.accumulate(audioSamples.map { it.toFloat() }.toFloatArray())
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
        val result = detector.getResults(from, to)
        Log.d(TAG, "getResult() - ${result.size} results: $result")
        return result
    }

    companion object {
        const val TAG = "MainViewModel"
    }
}