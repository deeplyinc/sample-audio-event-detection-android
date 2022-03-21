package com.deeply.samples.eventdetection.analyzer

import android.app.Application
import android.content.Context
import android.util.Log
import com.deeply.library.librosa.feature.MelSpectrogram
import com.deeply.samples.eventdetection.MainViewModel
import org.apache.commons.collections4.QueueUtils
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.apache.commons.math3.util.FastMath
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.pow
import kotlin.math.sqrt

class HomeAudioEventDetector(application: Application): AudioEventDetector {
    companion object {
        private const val TAG = "HomeAudioEventDetector"

        private const val MODEL_PARAM_N_FFT = 2048
        private const val MODEL_PARAM_HOP_LENGTH = 1024
    }

    private val app = application
    private val audioBuffer = QueueUtils.synchronizedQueue(CircularFifoQueue<Float>(AudioEventDetector.MODEL_INPUT_SAMPLE_SIZE))
    private val resultsBuffer = CopyOnWriteArrayList<AudioEvent>()

    private var moduleEncoder: Module? = null


    init {
        try {
            // PyTorch 모델 파일(.ptl)을 assets 폴더에 넣은 후 파일 이름을 지정해주시면 됩니다.
            moduleEncoder = LiteModuleLoader.load(
                assetFilePath(app, "elderly_220321_7class.ptl"))
        } catch (e: Exception) {
            Log.e(MainViewModel.TAG, "Failed to load PyTorch model file: ", e)
        }
    }

    override fun accumulate(inputAudioSamples: FloatArray) {
        synchronized(audioBuffer) {
            audioBuffer.addAll(inputAudioSamples.asList())

            if (audioBuffer.size < AudioEventDetector.MODEL_INPUT_SAMPLE_SIZE) return
            if (moduleEncoder == null) return

            // keep current time
            val to = Calendar.getInstance()
            val from = Calendar.getInstance()
            from.add(Calendar.SECOND, -(AudioEventDetector.MODEL_INPUT_SAMPLE_SIZE / AudioEventDetector.SAMPLE_RATE))

            // start inference
            val audioSamples = audioBuffer.toFloatArray()
            val preprocessed = preprocess(audioSamples)
            val modelInput = buildInput(preprocessed)
            val modelOutput = moduleEncoder?.forward(modelInput)

            if (modelOutput?.isTensor == true) {
                val resultTensor = modelOutput.toTensor()
                val resultData = resultTensor.dataAsFloatArray.take(AudioEventType.values().size)

                if (isValidResults(resultData)) {
//                    printResult(resultData) // Uncomment if you need the detail results
                    val audioEvent = buildAudioEvent(resultData, from ,to)
                    Log.d(TAG, "Analysis completed. Result: $audioEvent")
                    accumulateResult(audioEvent)
                }
            }
        }
    }

    override fun getResults(from: Calendar?, to: Calendar?): List<AudioEvent> {
        val results = mutableListOf<AudioEvent>()
        for (event in resultsBuffer) {
            if ((from == null || event.from.after(from)) && (to == null || event.to.before(to))) {
                results.add(event)
            }
        }
        return results.toList()
    }

    override fun clearResults() {
        resultsBuffer.clear()
    }

    /**
     * Preprocess the given audioSamples
     */
    private fun preprocess(audioSamples: FloatArray): Array<FloatArray> {
        // 마이크에서 오디오 샘플 인풋이 들어올 때는 short 자료형으로 들어오기 때문에 scaling 적용을 해주어야 함
        // 반면 테스트 등을 목적으로 .wav 등의 파일 형태로 인풋이 들어올 때는 이미 scaling 적용이 된 형태이기 때문에 따로 scale 해줄 필요가 없음
        val scaledInput = scaleToFloatRange(audioSamples)
        val input = scaledInput.map { it.toDouble() }.toDoubleArray()

        // standardization
        val avg = input.average()
        val variance = input.map {
            it.minus(avg).pow(2.0)
        }.average()
        val std = sqrt(variance)
        val standardizedInput = input.map {
            (it - avg) / (std)
        }

        val melResult: Array<FloatArray> = MelSpectrogram.createMelSpectrogram(
            standardizedInput.toDoubleArray(),
            AudioEventDetector.SAMPLE_RATE,
            null,
            MODEL_PARAM_N_FFT,
            MODEL_PARAM_HOP_LENGTH,
            null,
            null,
            null,
            null,
            null
        )

        // log mel-spectrogram
        val logOffset = 1e-10
        val multiplier = 10.0F
        val logMelResult = Array(melResult.size) {
            FloatArray(
                melResult[0].size
            )
        }
        for (i in melResult.indices) {
            for (j in melResult[0].indices) {
                logMelResult[i][j] = multiplier * FastMath.log10(melResult[i][j] + logOffset).toFloat()
            }
        }

        if (logMelResult.isEmpty()) throw Exception()

        // Log.d(TAG, "Preprocessing complete. Shape: ${logMelResult.size} * ${logMelResult[0].size}")

        return logMelResult
    }

    /**
     * Build input for PyTorch forward()
     */
    private fun buildInput(preprocessedInputs: Array<FloatArray>): IValue {
        val buffer = Tensor.allocateFloatBuffer(2 * 1 * 64 * 47)
        for (i in preprocessedInputs.indices) {
            for (j in preprocessedInputs[i].indices) {
                buffer.put(preprocessedInputs[i][j])
            }
        }
        val tensor = Tensor.fromBlob(buffer, longArrayOf(2, 1, 64, 47))
        return IValue.from(tensor)
    }

    /**
     * Store the inference results to the buffer in AudioEvent form
     */
    private fun accumulateResult(audioEvent: AudioEvent) {
        resultsBuffer.add(audioEvent)
    }

    /**
     * Get the event with the largest probability
     */
    private fun getLargestEvent(resultData: List<Float>): Pair<AudioEventType, Float> {
        var largestIndex = 0
        var largestConfidence = resultData[largestIndex]
        for (i in resultData.indices) {
            val probability = resultData[i]

            if (probability > largestConfidence) {
                largestIndex = i
                largestConfidence = probability
            }
        }
        return Pair(AudioEventType.values()[largestIndex], largestConfidence)
    }

    /**
     * Build AudioEvent
     */
    private fun buildAudioEvent(resultData: List<Float>, from: Calendar, to: Calendar): AudioEvent {
        val totalResultMap = AudioEventType.values()
            .map { it.name }
            .zip(resultData)
            .toMap()
        val (largestEvent, confidence) = getLargestEvent(resultData)
        return AudioEvent(largestEvent, confidence, from, to, totalResultMap)
    }

    /**
     * Check the inference results are valid or not
     */
    private fun isValidResults(resultData: List<Float>): Boolean {
        if (resultData.size < AudioEventType.values().size) {
            Log.e(TAG, "Model outputs are less than predefined output types in AudioEventType.")
            return false
        }
        return true
    }

    /**
     * Print the inference results to Logcat
     */
    private fun printResult(resultData: List<Float>) {
        Log.d(TAG, "Inference results")
        for (i in resultData.indices) {
            if (i < AudioEventType.values().size) {
                Log.d(TAG, "- ${AudioEventType.values()[i].name}: ${resultData[i]}")
            }
        }
    }

    private fun assetFilePath(context: Context, assetName: String): String? {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        try {
            context.assets.open(assetName).use { `is` ->
                FileOutputStream(file).use { os ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (`is`.read(buffer).also { read = it } != -1) {
                        os.write(buffer, 0, read)
                    }
                    os.flush()
                }
                return file.absolutePath
            }
        } catch (e: IOException) {
            Log.e(MainViewModel.TAG, assetName + ": " + e.localizedMessage)
        }
        return null
    }

    private fun scaleToFloatRange(data: FloatArray): List<Float> {
        return data.map {
            it / 32768.0F
        }
    }
}