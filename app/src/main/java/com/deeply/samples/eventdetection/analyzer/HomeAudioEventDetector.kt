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
import kotlin.random.Random

class HomeAudioEventDetector(application: Application): AudioEventDetector {
    companion object {
        private const val TAG = "HomeAudioEventDetector"
    }

    private val app = application
    private var moduleEncoder: Module? = null

    init {
        try {
            moduleEncoder = LiteModuleLoader.load(assetFilePath(app, "20211022-200000_vanilla_pytorch.ptl"))
        } catch (e: Exception) {
            Log.e(MainViewModel.TAG, "Failed to load PyTorch model file: ", e)
        }
    }

    override fun accumulate(audioSamples: FloatArray) {
        if (moduleEncoder != null) {
            val preprocessed = preprocess(audioSamples)
            for (preprocessedItem in preprocessed) {

            }
            val modelInput = buildInput(preprocessed)
            val modelOutput = moduleEncoder?.forward(modelInput)
            // logging
            if (modelOutput?.isTensor == true) {
                val resultTensor = modelOutput.toTensor()
                val resultData = resultTensor.dataAsFloatArray
                Log.d(TAG, "accumulate: ${resultData.size}")

                if (resultData.size < AudioEventType.values().size) {
                    Log.e(TAG, "accumulate: ")
                    return
                }
                for (i in resultData.indices) {
                    if (i < AudioEventType.values().size) {
                        Log.d(TAG, "accumulate result: ${resultData[i]}")
                    }
                }
            }
        }
    }

    override fun getResults(from: Calendar?, to: Calendar?): List<AudioEvent> {
        val results = mutableListOf<AudioEvent>()
        val nResults = Random.nextInt(5)
        for (i in 0..nResults) {
            val aSecondAgo = Calendar.getInstance()
            aSecondAgo.add(Calendar.SECOND, -1)
            val now = Calendar.getInstance()
            results.add(AudioEvent("sample audio event", aSecondAgo, now))
        }
        return results.toList()
    }

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

        // transpose
        if (logMelResult.isEmpty()) throw Exception()

        val transposeLogMelResultWithChannelAdd = arrayOf(Array(logMelResult[0].size) { FloatArray(logMelResult.size) })
        for (i in melResult.indices) {
            for (j in melResult[0].indices) {
                transposeLogMelResultWithChannelAdd[0][j][i] = logMelResult[i][j]
            }
        }

        // build proper shape of input
        if (transposeLogMelResultWithChannelAdd.isNotEmpty() && transposeLogMelResultWithChannelAdd[0].isNotEmpty()) {
            Log.d(TAG, "Preprocessing complete. Shape: ${transposeLogMelResultWithChannelAdd.size} * ${transposeLogMelResultWithChannelAdd[0].size} * ${transposeLogMelResultWithChannelAdd[0][0].size}")
        }
        return transposeLogMelResultWithChannelAdd
    }

    private fun buildInput(preprocessedInputs: Array<Array<FloatArray>>): IValue {
        val buffer = Tensor.allocateFloatBuffer(2 * 1 * 47 * 64)
        for (preprocessedInput in preprocessedInputs) {
            for (xDim in preprocessedInput) {
                for (yDim in xDim) {
                    buffer.put(yDim)
                }
            }
        }
        val tensor = Tensor.fromBlob(buffer, longArrayOf(2, 1, 47, 64))
        return IValue.from(tensor)
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