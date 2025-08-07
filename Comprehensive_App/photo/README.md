# 前置摄像头应用 - 集成Snowboy语音识别

这是一个Android应用，专门用于调用手机的前置摄像头进行拍照，支持手动拍照、自动定时拍照和**语音控制拍照**功能。

## 🎯 新增功能

- 🎤 **语音控制拍照** - 说出"手势识别开始"即可自动拍照
- 🔍 **实时语音检测** - 使用Snowboy离线热词检测
- 🎵 **音频处理** - 16kHz音频采样，实时处理
- 🔄 **自动模式切换** - 语音检测失败时自动切换到模拟模式

## 功能特性

- 📸 前置摄像头预览
- 🎯 手动拍照功能
- ⏰ 自动定时拍照（3秒间隔）
- 💾 自动保存照片到相册
- 🔒 权限管理
- 🎨 现代化UI设计
- 📊 实时拍照计数显示

## 技术栈

- **语言**: Kotlin
- **架构**: AndroidX CameraX
- **UI**: Material Design 3
- **最低SDK**: API 21 (Android 5.0)
- **目标SDK**: API 34 (Android 14)

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/frontcamera/
│   │   └── MainActivity.kt          # 主Activity
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml    # 主界面布局
│   │   ├── values/
│   │   │   ├── strings.xml          # 字符串资源
│   │   │   ├── colors.xml           # 颜色资源
│   │   │   └── themes.xml           # 主题样式
│   │   └── xml/
│   │       ├── backup_rules.xml     # 备份规则
│   │       └── data_extraction_rules.xml
│   └── AndroidManifest.xml          # 应用清单
└── build.gradle                     # 应用级构建配置
```

## 权限说明

应用需要以下权限：

- `CAMERA`: 访问摄像头
- `RECORD_AUDIO`: 录音权限（用于语音识别）

## 使用方法

### 手动拍照
1. 打开应用
2. 授予相机权限
3. 应用会自动启动前置摄像头预览
4. 点击拍照按钮进行拍照
5. 照片会自动保存到应用私有目录

### 自动拍照
1. 打开应用并授予权限
2. 打开"自动拍照"开关
3. 应用将每3秒自动拍摄一张照片
4. 屏幕顶部会显示已拍摄的照片数量
5. 点击"停止自动拍照"按钮或关闭开关来停止自动拍照

### 语音控制拍照
1. 打开应用并授予相机和录音权限
2. 点击"开始语音监听"按钮
3. 对着麦克风说出"手势识别开始"
4. 应用会自动拍照并显示"检测到语音命令"
5. 点击"停止语音监听"按钮停止监听

## 构建和运行

### 环境要求

- Android Studio Arctic Fox 或更高版本
- Android SDK API 21+
- Gradle 8.12+

### 构建步骤

1. 克隆项目到本地
2. 在Android Studio中打开项目
3. 等待Gradle同步完成
4. 连接Android设备或启动模拟器
5. 点击运行按钮

### 命令行构建

```bash
# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

## 核心代码说明

### 自动拍照实现

```kotlin
// 开始自动拍照
private fun startAutoCapture() {
    isAutoCaptureEnabled = true
    photoCount = 0
    
    autoCaptureHandler = Handler(Looper.getMainLooper())
    autoCaptureRunnable = object : Runnable {
        override fun run() {
            if (isAutoCaptureEnabled) {
                takePhoto()
                photoCount++
                autoCaptureHandler?.postDelayed(this, AUTO_CAPTURE_INTERVAL)
            }
        }
    }
    
    // 立即开始第一次拍照
    autoCaptureHandler?.post(autoCaptureRunnable!!)
}
```

### 摄像头初始化

```kotlin
private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    
    cameraProviderFuture.addListener({
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        
        val preview = Preview.Builder().build()
        imageCapture = ImageCapture.Builder().build()
        
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT) // 只使用前置摄像头
            .build()
            
        cameraProvider.bindToLifecycle(
            this, cameraSelector, preview, imageCapture
        )
    }, ContextCompat.getMainExecutor(this))
}
```

### 拍照功能

```kotlin
private fun takePhoto() {
    val photoFile = File(
        getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "FrontCamera_${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())}.jpg"
    )
    
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                // 照片保存成功
            }
            
            override fun onError(exception: ImageCaptureException) {
                // 照片保存失败
            }
        }
    )
}
```

## 常见问题解决

### Gradle下载问题

如果遇到Gradle下载失败的问题，请尝试以下解决方案：

1. **清理Gradle缓存**：
   ```powershell
   Remove-Item -Path "$env:USERPROFILE\.gradle\wrapper\dists" -Recurse -Force
   ```

2. **修改Gradle版本**：
   在 `gradle/wrapper/gradle-wrapper.properties` 中修改为：
   ```properties
   distributionUrl=https\://services.gradle.org/distributions/gradle-8.12-bin.zip
   ```

3. **使用代理或VPN**：
   如果网络问题，可以配置代理或使用VPN

### 权限问题

如果应用无法获取相机权限：

1. 在设备设置中手动授予权限
2. 确保设备有前置摄像头
3. 重启应用

### 自动拍照问题

如果自动拍照功能异常：

1. 确保相机权限已授予
2. 检查应用是否在前台运行
3. 某些设备可能需要保持屏幕常亮

## 注意事项

1. 确保设备有前置摄像头
2. 首次运行需要授予权限
3. 某些设备可能需要手动在设置中启用相机权限
4. 照片保存在应用私有目录中
5. 应用只支持前置摄像头，不支持切换
6. 自动拍照间隔为3秒，可在代码中修改
7. 应用退出时会自动停止自动拍照功能

## Snowboy语音识别集成

### 技术架构

本项目集成了Snowboy离线热词检测库，实现了语音控制拍照功能：

1. **SWIG包装器生成** - 使用SWIG 4.3.1生成Java包装器
2. **原生库编译** - 使用Android NDK编译C++代码
3. **实时音频处理** - 16kHz采样率，16位PCM格式
4. **热词检测** - 检测"手势识别开始"语音命令

### 文件结构

```
app/src/main/
├── java/ai/kitt/snowboy/          # Snowboy Java包装器
│   ├── SnowboyDetect.java
│   ├── snowboyJNI.java
│   └── ...
├── cpp/                           # 原生代码
│   ├── snowboy-wrapper.cpp        # JNI包装器
│   ├── include/snowboy-detect.h   # Snowboy头文件
│   └── CMakeLists.txt            # CMake构建配置
├── jniLibs/                       # 预编译库
│   └── arm64-v8a/libsnowboy-detect.a
└── assets/                        # 模型文件
    ├── common.res                 # Snowboy资源文件
    └── gesture_start.pmdl         # 热词模型
```

### 构建说明

1. **SWIG生成**：
   ```bash
   swig -IE:\DeskTop\Comprehensive_App\snowboy -c++ -java -package ai.kitt.snowboy -outdir java\ai\kitt\snowboy\ -o snowboy-detect-swig.cc snowboy-detect-swig.i
   ```

2. **CMake编译**：
   ```cmake
   add_library(snowboy-detect-android SHARED snowboy-wrapper.cpp)
   target_link_libraries(snowboy-detect-android ${snowboy_static_lib} log)
   ```

3. **Gradle配置**：
   ```gradle
   externalNativeBuild {
       cmake {
           path "src/main/cpp/CMakeLists.txt"
       }
   }
   ```

### 使用说明

- **真实模式**：当模型文件存在时，使用真实的Snowboy检测
- **模拟模式**：当模型文件缺失或检测失败时，自动切换到基于音频能量的简单检测
- **灵敏度调节**：可通过`setSensitivity()`方法调整检测灵敏度

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request！ 