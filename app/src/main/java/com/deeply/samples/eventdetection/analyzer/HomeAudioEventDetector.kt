package com.deeply.samples.eventdetection.analyzer

import android.app.Application
import android.content.Context
import android.util.Log
import com.deeply.library.librosa.HammingWindowFunction
import com.deeply.library.librosa.StandardScaler
import com.deeply.library.librosa.feature.MelSpectrogram
import com.deeply.samples.eventdetection.MainViewModel
import org.apache.commons.math3.util.FastMath
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class HomeAudioEventDetector(application: Application): AudioEventDetector {
    companion object {
        private const val TAG = "HomeAudioEventDetector"
    }

    private val app = application
    private var moduleEncoder: Module? = null
    private val resultsBuffer = mutableListOf<AudioEvent>()

    init {
        try {
            // PyTorch 모델 파일(.ptl)을 assets 폴더에 넣은 후 파일 이름을 지정해주시면 됩니다.
            moduleEncoder = LiteModuleLoader.load(
                assetFilePath(app, "NonverbalClassifier_20211112.ptl"))
        } catch (e: Exception) {
            Log.e(MainViewModel.TAG, "Failed to load PyTorch model file: ", e)
        }
    }

    override fun accumulate(audioSamples: FloatArray) {
        if (moduleEncoder != null) {
            // keep current time
            val to = Calendar.getInstance()
            val from = Calendar.getInstance()
            from.add(Calendar.SECOND, -3)

            // start inference
            val preprocessed = preprocess(audioSamples)
            val modelInput = buildInput(preprocessed)
            val modelOutput = moduleEncoder?.forward(modelInput)

            if (modelOutput?.isTensor == true) {
                val resultTensor = modelOutput.toTensor()
                val resultData = resultTensor.dataAsFloatArray.take(AudioEventType.values().size)

                if (isValidResults(resultData)) {
                    // printResult(resultData) // Uncomment if you need the detail results
                    val audioEvent = buildAudioEvent(getLargestEvent(resultData), from, to)
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
    private fun preprocess(audioSamples: FloatArray): Array<Array<FloatArray>> {
        val logOffset = 1e-10
        val input = DoubleArray(audioSamples.size)
        for (i in audioSamples.indices) {
            input[i] = audioSamples[i].toDouble()
        }

        // Hamming window
        val windowFunction = HammingWindowFunction(audioSamples.size)
        windowFunction.applyFunction(input)

        // For backward compatibility
        var dimAddInput = Array(1) {
            DoubleArray(
                input.size
            )
        } // [numOfSample][numOfFeature]
        for (i in dimAddInput.indices) {
            for (j in dimAddInput[0].indices) {
                dimAddInput[i][j] = input[j]
            }
        }
        val scaler = StandardScaler()
        dimAddInput = scaler.scale(dimAddInput)

        // For backward compatibility
        for (i in dimAddInput.indices) {
            for (j in dimAddInput[0].indices) {
                input[j] = dimAddInput[i][j]
            }
        }

        val melResult: Array<FloatArray> = MelSpectrogram.createMelSpectrogram(
            input, 16000, null, 2048, 1024,
            null, null, null, null, null
        )

        // log mel-spectrogram
        val logMelResult = Array(melResult.size) {
            FloatArray(
                melResult[0].size
            )
        }
        for (i in melResult.indices) {
            for (j in melResult[0].indices) {
                logMelResult[i][j] = FastMath.log(melResult[i][j] + logOffset).toFloat()
            }
        }

        if (logMelResult.isEmpty()) throw Exception()

        val channelAdded = arrayOf(Array(logMelResult.size) { FloatArray(logMelResult[0].size) })
        for (i in melResult.indices) {
            for (j in melResult[0].indices) {
                channelAdded[0][i][j] = logMelResult[i][j]
            }
        }

        // build proper shape of input
        if (channelAdded.isNotEmpty() && channelAdded[0].isNotEmpty()) {
            Log.d(TAG, "Preprocessing complete. Shape: ${channelAdded.size} * ${channelAdded[0].size} * ${channelAdded[0][0].size}")
        }
        return channelAdded
    }

    /**
     * Build input for PyTorch forward()
     */
    private fun buildInput(preprocessedInputs: Array<Array<FloatArray>>): IValue {
        val buffer = Tensor.allocateFloatBuffer(2 * 1 * 64 * 47)
        for (preprocessedInput in preprocessedInputs) {
            for (xDim in preprocessedInput) {
                for (yDim in xDim) {
                    buffer.put(yDim)
                }
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
    private fun getLargestEvent(resultData: List<Float>): AudioEventType {
        var largestIndex = 0
        for (i in resultData.indices) {
            val probability = resultData[i]

            if (probability > resultData[largestIndex]) {
                largestIndex = i
            }
        }
        return AudioEventType.values()[largestIndex]
    }

    /**
     * Build AudioEvent
     */
    private fun buildAudioEvent(audioEventType: AudioEventType, from: Calendar, to: Calendar): AudioEvent {
        return AudioEvent(audioEventType, from, to)
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
}