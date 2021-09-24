package com.deeply.samples.eventdetection.recorder

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class DeeplyRecorder(
    private val audioSource: Int = MediaRecorder.AudioSource.MIC,
    private val sampleRate: Int = 16000,
    private val channel: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    bufferSize: Int? = null
) {
    private var buffer: ShortArray = ShortArray(bufferSize ?: AudioRecord.getMinBufferSize(
            sampleRate,
            channel,
            audioFormat
        )
    )

    private var audioRecorder: AudioRecord? = null
    private var run = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(): Flow<ShortArray> = flow {
        run = true
        audioRecorder = AudioRecord(audioSource, sampleRate, channel, audioFormat, buffer.size)
        audioRecorder?.let {
            it.startRecording()
            while (run) {
                val result = it.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                emit(buffer)
            }
            audioRecorder?.stop()
            audioRecorder?.release()
        }
    }.flowOn(Dispatchers.IO)

    fun stop() {
        run = false
    }

    fun isRecording() = audioRecorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    fun getMinBufferSize(): Int = AudioRecord.getMinBufferSize(sampleRate, channel, audioFormat)
}