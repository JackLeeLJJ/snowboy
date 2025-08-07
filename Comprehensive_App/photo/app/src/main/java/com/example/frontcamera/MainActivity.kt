package com.example.frontcamera

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.lang.UnsatisfiedLinkError
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.frontcamera.databinding.ActivityMainBinding
import ai.kitt.snowboy.SnowboyDetect
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    
    // Snowboy相关变量
    private var snowboyDetect: SnowboyDetect? = null
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    
    // 自动拍照相关变量
    private var isAutoCaptureEnabled = false
    private var autoCaptureHandler: Handler? = null
    private var autoCaptureRunnable: Runnable? = null
    private var photoCount = 0
    
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val AUTO_CAPTURE_INTERVAL = 3000L // 3秒间隔
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "=== onCreate 开始 ===")
        
        // 确保JNI库已加载
        try {
            System.loadLibrary("snowboy-detect-android")
            Log.d(TAG, "JNI库加载成功")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "JNI库加载失败: ${e.message}")
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置按钮点击事件
        binding.captureButton.setOnClickListener { 
            if (isAutoCaptureEnabled) {
                stopAutoCapture()
            } else {
                takePhoto()
            }
        }
        
        // 设置自动拍照开关
        binding.autoCaptureSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startAutoCapture()
            } else {
                stopAutoCapture()
            }
        }

        // 添加语音监听按钮
        binding.voiceListeningButton.setOnClickListener {
            if (!isListening) {
                startVoiceListening()
            } else {
                stopVoiceListening()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // 延迟检查权限，确保UI完全加载
        binding.root.post {
            Log.d(TAG, "=== 开始检查权限 ===")
            checkPermissions()
        }
    }

    private fun checkPermissions() {
        Log.d(TAG, "检查权限状态...")
        
        REQUIRED_PERMISSIONS.forEach { permission ->
            val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "权限 $permission: ${if (isGranted) "已授予" else "未授予"}")
        }

        if (allPermissionsGranted()) {
            Log.d(TAG, "所有权限已授予，启动相机")
            startCamera()
            initializeSnowboy()
        } else {
            Log.d(TAG, "权限不足，请求权限")
            // 显示权限提示
            binding.permissionText.visibility = android.view.View.VISIBLE
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun initializeSnowboy() {
        // 暂时强制使用模拟模式，避免库兼容性问题
        Log.d(TAG, "强制使用模拟模式")
        initializeSimulationMode()
        
        // 原始代码（暂时注释）
        /*
        try {
            // 尝试使用真实的Snowboy
            initializeRealSnowboy()
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "JNI库加载失败: ${e.message}")
            Toast.makeText(this, "语音检测库加载失败，请重新安装应用", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "真实Snowboy初始化失败，切换到模拟模式: ${e.message}")
            e.printStackTrace()
            // 如果真实模式失败，回退到模拟模式
            initializeSimulationMode()
        }
        */
    }
    
    private fun initializeRealSnowboy() {
        // 检查资源文件是否存在
        val resourceFile = getAssetFilePath("common.res")
        val modelFile = getAssetFilePath("gesture_start.pmdl")
        
        Log.d(TAG, "尝试初始化真实Snowboy")
        Log.d(TAG, "资源文件路径: $resourceFile")
        Log.d(TAG, "模型文件路径: $modelFile")
        
        // 初始化真实的Snowboy检测器
        snowboyDetect = SnowboyDetect(resourceFile, modelFile).apply {
            SetSensitivity("0.3") // 降低灵敏度，更容易触发
            SetAudioGain(1.0f)    // 音频增益
        }
        
        Log.d(TAG, "真实Snowboy初始化成功")
        Toast.makeText(this, "语音检测已初始化（真实模式）- 说出'手势识别开始'即可触发", Toast.LENGTH_LONG).show()
    }
    
    private fun initializeSimulationMode() {
        // 初始化Snowboy检测器 - 使用模拟模式
        snowboyDetect = SnowboyDetect("", "").apply {
            SetSensitivity("0.3") // 降低灵敏度，更容易触发
            SetAudioGain(1.0f)    // 音频增益
        }
        Log.d(TAG, "Snowboy初始化成功（模拟模式）")
        Toast.makeText(this, "语音检测已初始化（模拟模式）- 说话即可触发", Toast.LENGTH_LONG).show()
    }
    
    private fun getAssetFilePath(assetName: String): String {
        // 将资源文件复制到内部存储并返回路径
        val inputStream = assets.open(assetName)
        val outputFile = File(filesDir, assetName)
        
        if (!outputFile.exists()) {
            outputFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        
        return outputFile.absolutePath
    }

    private fun startVoiceListening() {
        if (!allPermissionsGranted()) {
            Toast.makeText(this, "需要录音和相机权限", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (snowboyDetect == null) {
            Toast.makeText(this, "语音检测未初始化", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 检查相机是否正在运行
        if (camera == null) {
            Toast.makeText(this, "相机未启动，请先启动相机", Toast.LENGTH_SHORT).show()
            return
        }
        
        isListening = true
        binding.voiceListeningButton.text = "停止语音监听"
        binding.statusText.text = "正在监听语音命令..."
        binding.statusText.visibility = android.view.View.VISIBLE
        
        Toast.makeText(this, "开始监听语音命令", Toast.LENGTH_SHORT).show()
        
        // 启动音频录制
        startAudioRecording()
    }
    
    private fun stopVoiceListening() {
        isListening = false
        binding.voiceListeningButton.text = "开始语音监听"
        binding.statusText.visibility = android.view.View.GONE
        
        // 停止音频录制
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        snowboyDetect?.Reset()
        Toast.makeText(this, "停止语音监听", Toast.LENGTH_SHORT).show()
    }
    
    private fun startAudioRecording() {
        val sampleRate = 16000 // Snowboy要求16kHz
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "音频参数无效")
            Toast.makeText(this, "音频参数无效", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord初始化失败")
                Toast.makeText(this, "音频录制初始化失败", Toast.LENGTH_SHORT).show()
                return
            }
            
            audioRecord?.startRecording()
            Log.d(TAG, "音频录制开始")
            
            // 启动检测线程
            Thread {
                val buffer = ShortArray(bufferSize / 2)
                while (isListening) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        val result = snowboyDetect?.RunDetection(buffer, readSize) ?: -2
                        if (result > 0) {
                            // 检测到热词
                            Log.d(TAG, "检测到语音命令: $result")
                            runOnUiThread {
                                onVoiceCommandDetected()
                            }
                            break
                        }
                    }
                }
            }.start()
            
        } catch (e: Exception) {
            Log.e(TAG, "启动音频录制失败: ${e.message}")
            Toast.makeText(this, "音频录制启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun onVoiceCommandDetected() {
        binding.statusText.text = "检测到语音命令，启动摄像头..."
        Toast.makeText(this, "检测到语音命令！", Toast.LENGTH_SHORT).show()
        
        // 停止语音监听
        stopVoiceListening()
        
        // 启动摄像头并拍照
        startCameraAndTakePhoto()
    }
    
    private fun startCameraAndTakePhoto() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // 创建预览用例
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            
            // 创建拍照用例
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            
            // 选择前置摄像头
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            try {
                // 解绑之前的用例
                cameraProvider.unbindAll()
                
                // 绑定用例到相机
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                
                // 延迟拍照，确保相机已启动
                Handler(Looper.getMainLooper()).postDelayed({
                    takePhoto()
                }, 1000)
                
            } catch (e: Exception) {
                Log.e(TAG, "相机启动失败: ${e.message}")
                Toast.makeText(this, "相机启动失败", Toast.LENGTH_SHORT).show()
            }
            
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        Log.d(TAG, "尝试拍照...")
        val imageCapture = imageCapture ?: return
        
        // 检查相机状态
        if (camera == null) {
            Log.e(TAG, "相机未初始化，无法拍照")
            Toast.makeText(this, "相机未初始化，请重新启动应用", Toast.LENGTH_SHORT).show()
            return
        }

        // 创建照片文件到应用私有目录
        val photoFile = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "FrontCamera_${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "照片已保存到: ${photoFile.absolutePath}"
                    Log.d(TAG, "照片保存成功: $savedUri")
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    val msg = getString(R.string.failed_to_save_photo)
                    Log.e(TAG, "拍照失败: ${exception.message}")
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun startCamera() {
        Log.d(TAG, "开始启动相机...")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                // 解绑之前的用例
                cameraProvider.unbindAll()

                // 绑定用例到相机
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                // 隐藏权限提示
                binding.permissionText.visibility = android.view.View.GONE
                Log.d(TAG, "相机启动成功")

            } catch (exc: Exception) {
                Log.e(TAG, "相机启动失败: ${exc.message}")
                exc.printStackTrace()
                Toast.makeText(this, "前置摄像头启动失败: ${exc.message}", Toast.LENGTH_SHORT).show()
                
                // 尝试重新启动相机
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "尝试重新启动相机...")
                    startCamera()
                }, 2000)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted(): Boolean {
        val result = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        Log.d(TAG, "权限检查结果: $result")
        return result
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "权限请求结果: requestCode=$requestCode")
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            permissions.forEachIndexed { index, permission ->
                val isGranted = grantResults[index] == PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "权限 $permission: ${if (isGranted) "已授予" else "被拒绝"}")
            }
            
            if (allPermissionsGranted()) {
                Log.d(TAG, "所有权限已授予，启动相机")
                startCamera()
                initializeSnowboy()
            } else {
                Log.d(TAG, "权限被拒绝，显示提示")
                binding.permissionText.visibility = android.view.View.VISIBLE
                Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause 被调用")
        // 暂停时停止语音监听，避免与其他应用冲突
        if (isListening) {
            Log.d(TAG, "应用暂停，停止语音监听")
            stopVoiceListening()
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume 被调用")
        // 每次回到前台时检查权限
        if (allPermissionsGranted() && camera == null) {
            Log.d(TAG, "onResume: 权限已授予但相机未启动，重新启动相机")
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy 被调用")
        // 停止自动拍照
        if (isAutoCaptureEnabled) {
            stopAutoCapture()
        }
        // 停止语音监听
        stopVoiceListening()
        // 释放Snowboy资源
        snowboyDetect?.delete()
        cameraExecutor.shutdown()
    }

    // 开始自动拍照
    private fun startAutoCapture() {
        if (!allPermissionsGranted() || imageCapture == null) {
            Log.d(TAG, "权限不足或相机未准备好，无法开始自动拍照")
            binding.autoCaptureSwitch.isChecked = false
            return
        }
        
        isAutoCaptureEnabled = true
        photoCount = 0
        // FloatingActionButton没有text属性，使用contentDescription
        binding.captureButton.contentDescription = getString(R.string.stop_auto_capture)
        binding.statusText.text = getString(R.string.auto_capture_started)
        binding.statusText.visibility = android.view.View.VISIBLE
        
        Log.d(TAG, "开始自动拍照，间隔: ${AUTO_CAPTURE_INTERVAL}ms")
        
        autoCaptureHandler = Handler(Looper.getMainLooper())
        autoCaptureRunnable = object : Runnable {
            override fun run() {
                if (isAutoCaptureEnabled) {
                    takePhoto()
                    photoCount++
                    binding.statusText.text = getString(R.string.auto_capture_count, photoCount)
                    autoCaptureHandler?.postDelayed(this, AUTO_CAPTURE_INTERVAL)
                }
            }
        }
        
        // 立即开始第一次拍照
        autoCaptureHandler?.post(autoCaptureRunnable!!)
    }
    
    // 停止自动拍照
    private fun stopAutoCapture() {
        isAutoCaptureEnabled = false
        // FloatingActionButton没有text属性，使用contentDescription
        binding.captureButton.contentDescription = getString(R.string.take_photo)
        binding.statusText.text = getString(R.string.auto_capture_stopped, photoCount)
        
        Log.d(TAG, "停止自动拍照，共拍摄: $photoCount 张")
        
        // 延迟隐藏状态文本
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isAutoCaptureEnabled) {
                binding.statusText.visibility = android.view.View.GONE
            }
        }, 2000)
        
        autoCaptureRunnable?.let { autoCaptureHandler?.removeCallbacks(it) }
        autoCaptureHandler = null
        autoCaptureRunnable = null
    }
} 