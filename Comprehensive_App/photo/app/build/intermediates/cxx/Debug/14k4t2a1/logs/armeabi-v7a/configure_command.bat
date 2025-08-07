@echo off
"D:\\SDK_Android\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HE:\\DeskTop\\Comprehensive_App\\photo\\app\\src\\main\\cpp" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=armeabi-v7a" ^
  "-DCMAKE_ANDROID_ARCH_ABI=armeabi-v7a" ^
  "-DANDROID_NDK=D:\\SDK_Android\\ndk\\25.1.8937393" ^
  "-DCMAKE_ANDROID_NDK=D:\\SDK_Android\\ndk\\25.1.8937393" ^
  "-DCMAKE_TOOLCHAIN_FILE=D:\\SDK_Android\\ndk\\25.1.8937393\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=D:\\SDK_Android\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=E:\\DeskTop\\Comprehensive_App\\photo\\app\\build\\intermediates\\cxx\\Debug\\14k4t2a1\\obj\\armeabi-v7a" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=E:\\DeskTop\\Comprehensive_App\\photo\\app\\build\\intermediates\\cxx\\Debug\\14k4t2a1\\obj\\armeabi-v7a" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BE:\\DeskTop\\Comprehensive_App\\photo\\app\\.cxx\\Debug\\14k4t2a1\\armeabi-v7a" ^
  -GNinja
