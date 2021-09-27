package com.deeply.samples.eventdetection.analyzer

import android.app.Application
import android.content.Context
import android.util.Log
import com.deeply.samples.eventdetection.MainViewModel
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.random.Random

/**
 * 오디오 분석 모듈의 샘플 구현.
 * 이 버전은 샘플 버전으로, 작동 방식을 확인하기 위한 목적으로 작성되었기 때문에 실제로 오디오 분석을 진행하지는 않습니다.
 * 모델 파일이 현재는 포함되어 있지 않기 때문에 로딩하는 과정에서 exception이 발생하나, 샘플 모듈의 작동에는 영향을 주지 않습니다.
 */
class SampleAudioEventDetector(application: Application): AudioEventDetector {
    private val app = application
    private var moduleEncoder: Module? = null

    init {
        try {
            moduleEncoder = LiteModuleLoader.load(assetFilePath(app, "model.ptl"))
        } catch (e: Exception) {
            Log.e(MainViewModel.TAG, "Failed to load PyTorch model file: ", e)
        }
    }

    override fun accumulate(audioSamples: FloatArray) {
        if (moduleEncoder != null) {
            TODO("not yet implemented")
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

    private fun buildInput(preprocessed: Array<FloatArray>): IValue {
        val buffer = Tensor.allocateFloatBuffer(64 * 157)
        val tensor = Tensor.fromBlob(buffer, longArrayOf(64, 157))
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