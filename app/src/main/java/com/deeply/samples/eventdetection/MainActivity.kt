package com.deeply.samples.eventdetection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.deeply.samples.eventdetection.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    private val requestRecordPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Audio recording permission granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        initListeners()
    }

    private fun initListeners() {
        binding.start.setOnClickListener {
            toggleAnalyzing()
        }
        binding.result.setOnClickListener {
            val beforeTenMinute = Calendar.getInstance()
            beforeTenMinute.add(Calendar.MINUTE, -10)
            val now = Calendar.getInstance()
            viewModel.getResult(from = beforeTenMinute, to = now)
        }
    }

    private fun toggleAnalyzing() {
        if (viewModel.isAnalyzing()) {
            viewModel.stopAnalyzing() // 오디오 녹음 및 분석 종료
            binding.start.text = "Start"
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                requestRecordPermission.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                viewModel.startAnalyzing() // 오디오 녹음 및 분석 시작
                binding.start.text = "Stop"
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}