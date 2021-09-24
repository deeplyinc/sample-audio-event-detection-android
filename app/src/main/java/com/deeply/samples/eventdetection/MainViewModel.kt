package com.deeply.samples.eventdetection

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deeply.samples.eventdetection.recorder.DeeplyRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val recorder = DeeplyRecorder(bufferSize = 16000)

    fun analyze() {
        viewModelScope.launch(Dispatchers.Default) {
            if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                recorder.start().collect { audioSamples ->
                    Log.d(TAG, "analyze() - audioSamples.size: ${audioSamples.size}")
                }
            }
        }
    }

    companion object {
        const val TAG = "MainViewModel"
    }
}