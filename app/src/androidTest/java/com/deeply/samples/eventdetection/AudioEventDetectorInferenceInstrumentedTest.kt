package com.deeply.samples.eventdetection

import android.app.Application
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deeply.samples.eventdetection.analyzer.AudioEventDetector
import com.deeply.samples.eventdetection.analyzer.AudioEventType
import com.deeply.samples.eventdetection.analyzer.HomeAudioEventDetector
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.InputStreamReader

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AudioEventDetectorInferenceInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val detector: AudioEventDetector = HomeAudioEventDetector(context.applicationContext as Application)

    @Test
    fun testCough() {
        val audioSamples = readAudioSampleFile("test_cough_0_audio.txt")
        detector.accumulate(audioSamples.toFloatArray())
        val result = detector.getResults(null, null)
        detector.clearResults()

        assertEquals(result.first().name, AudioEventType.COUGH)
    }

    @Test
    fun testSneeze() {
        val audioSamples = readAudioSampleFile("test_sneeze_0_audio.txt")
        detector.accumulate(audioSamples.toFloatArray())
        val result = detector.getResults(null, null)
        detector.clearResults()

        assertEquals(result.first().name, AudioEventType.SNEEZE)
    }

    @Test
    fun testNoseBlowing() {
        val audioSamples = readAudioSampleFile("test_noseblowing_0_audio.txt")
        detector.accumulate(audioSamples.toFloatArray())
        val result = detector.getResults(null, null)
        detector.clearResults()

        assertEquals(result.first().name, AudioEventType.NOSE_BLOWING)
    }

    @Test
    fun testScream() {
        val audioSamples = readAudioSampleFile("test_scream_0_audio.txt")
        detector.accumulate(audioSamples.toFloatArray())
        val result = detector.getResults(null, null)
        detector.clearResults()

        assertEquals(result.first().name, AudioEventType.SCREAM)
    }

    @Test
    fun testPant() {
        val audioSamples = readAudioSampleFile("test_pant_0_audio.txt")
        detector.accumulate(audioSamples.toFloatArray())
        val result = detector.getResults(null, null)
        detector.clearResults()

        assertEquals(result.first().name, AudioEventType.PANT)
    }

    @Test
    fun testMoan() {
        val audioSamples = readAudioSampleFile("test_moan_0_audio.txt")
        detector.accumulate(audioSamples.toFloatArray())
        val result = detector.getResults(null, null)
        detector.clearResults()

        assertEquals(result.first().name, AudioEventType.MOAN)
    }

    @Test
    fun testOthers() {
        val audioSamples = readAudioSampleFile("test_others_0_audio.txt")
        detector.accumulate(audioSamples.toFloatArray())
        val result = detector.getResults(null, null)
        detector.clearResults()

        assertEquals(result.first().name, AudioEventType.OTHERS)
    }

    private fun readAudioSampleFile(assetFileName: String): List<Float> {
        val audioSampleFile = context.assets.open(assetFileName)

        val audioSamples = mutableListOf<Float>()
        val isr = InputStreamReader(audioSampleFile)
        isr.forEachLine {
            audioSamples.add(it.toFloat())
            if (audioSamples.size % 16000 == 0) {
                detector.accumulate(audioSamples.toFloatArray())
                audioSamples.clear()
            }
        }
        audioSampleFile.close()
        isr.close()

        return audioSamples
    }
}