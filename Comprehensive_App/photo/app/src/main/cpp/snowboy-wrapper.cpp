#include <jni.h>
#include <android/log.h>
#include <map>
#include <string>
#include <cmath>
#include <algorithm>

// 尝试包含真实Snowboy头文件
#ifdef SNOWBOY_REAL
#include "snowboy-detect.h"
#endif

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "SnowboyWrapper", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "SnowboyWrapper", __VA_ARGS__)

// 全局变量管理检测器实例
#ifdef SNOWBOY_REAL
std::map<jlong, snowboy::SnowboyDetect*> detector_map;
#else
std::map<jlong, bool> detector_map; // 使用bool表示是否为模拟模式
#endif
jlong next_detector_id = 1;

// 模拟检测函数
int mock_detection(const short* data, int array_length) {
    // 简单的能量检测
    double energy = 0.0;
    for (int i = 0; i < array_length; i++) {
        energy += (data[i] * data[i]);
    }
    energy /= array_length;
    
    // 如果能量超过阈值，返回检测到
    if (energy > 1000000) { // 调整阈值
        LOGI("Mock detection triggered with energy: %f", energy);
        return 1;
    }
    return 0;
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_ai_kitt_snowboy_snowboyJNI_new_1SnowboyDetect(JNIEnv *env, jclass clazz, jstring resource_filename, jstring model_str) {
    (void)clazz; // 避免未使用参数警告

    const char* resource_cstr = env->GetStringUTFChars(resource_filename, 0);
    const char* model_cstr = env->GetStringUTFChars(model_str, 0);

#ifdef SNOWBOY_REAL
    try {
        // 尝试创建真实的Snowboy检测器
        snowboy::SnowboyDetect* detector = new snowboy::SnowboyDetect(resource_cstr, model_cstr);
        jlong detector_id = next_detector_id++;
        detector_map[detector_id] = detector;

        LOGI("Created real SnowboyDetect instance with ID: %lld", detector_id);

        env->ReleaseStringUTFChars(resource_filename, resource_cstr);
        env->ReleaseStringUTFChars(model_str, model_cstr);

        return detector_id;
    } catch (const std::exception& e) {
        LOGE("Failed to create real SnowboyDetect: %s", e.what());
        
        // 如果创建失败，返回模拟的ID
        jlong mock_id = -next_detector_id++;
        LOGI("Using mock SnowboyDetect with ID: %lld", mock_id);

        env->ReleaseStringUTFChars(resource_filename, resource_cstr);
        env->ReleaseStringUTFChars(model_str, model_cstr);

        return mock_id;
    }
#else
    // 暂时只使用模拟模式，避免链接器错误
    jlong detector_id = next_detector_id++;
    detector_map[detector_id] = true; // true表示模拟模式

    LOGI("Created mock SnowboyDetect instance with ID: %lld", detector_id);

    env->ReleaseStringUTFChars(resource_filename, resource_cstr);
    env->ReleaseStringUTFChars(model_str, model_cstr);

    return detector_id;
#endif
}

JNIEXPORT void JNICALL
Java_ai_kitt_snowboy_snowboyJNI_delete_1SnowboyDetect(JNIEnv *env, jclass clazz, jlong detector_ptr) {
    (void)env;
    (void)clazz;
    
#ifdef SNOWBOY_REAL
    if (detector_ptr > 0) {
        // 删除真实的检测器
        auto it = detector_map.find(detector_ptr);
        if (it != detector_map.end()) {
            delete it->second;
            detector_map.erase(it);
            LOGI("Deleted real SnowboyDetect instance with ID: %lld", detector_ptr);
        }
    } else {
        // 模拟检测器，无需删除
        LOGI("Deleted mock SnowboyDetect instance with ID: %lld", detector_ptr);
    }
#else
    if (detector_map.find(detector_ptr) != detector_map.end()) {
        detector_map.erase(detector_ptr);
        LOGI("Deleted SnowboyDetect instance with ID: %lld", detector_ptr);
    }
#endif
}

JNIEXPORT void JNICALL
Java_ai_kitt_snowboy_snowboyJNI_SnowboyDetect_1Reset(JNIEnv *env, jclass clazz, jlong detector_ptr) {
    (void)env;
    (void)clazz;
    
#ifdef SNOWBOY_REAL
    if (detector_ptr > 0) {
        // 调用真实的Reset
        auto it = detector_map.find(detector_ptr);
        if (it != detector_map.end()) {
            it->second->Reset();
            LOGI("Real SnowboyDetect Reset called for ID: %lld", detector_ptr);
            return;
        }
    }
#endif
    
    // 模拟Reset
    LOGI("Mock SnowboyDetect Reset called for ID: %lld", detector_ptr);
}

JNIEXPORT jint JNICALL
Java_ai_kitt_snowboy_snowboyJNI_SnowboyDetect_1RunDetection_1_1SWIG_15(JNIEnv *env, jclass clazz, jlong detector_ptr, jobject detector_obj, jshortArray data, jint array_length) {
    (void)env;
    (void)clazz;
    (void)detector_obj;
    
    // 获取音频数据
    jshort* audio_data = env->GetShortArrayElements(data, 0);
    
#ifdef SNOWBOY_REAL
    if (detector_ptr > 0) {
        // 调用真实的RunDetection
        auto it = detector_map.find(detector_ptr);
        if (it != detector_map.end()) {
            int result = it->second->RunDetection(audio_data, array_length);
            env->ReleaseShortArrayElements(data, audio_data, 0);
            
            if (result > 0) {
                LOGI("Real SnowboyDetect triggered: %d", result);
            }
            return result;
        }
    }
#endif
    
    // 使用模拟检测
    int result = mock_detection(audio_data, array_length);
    env->ReleaseShortArrayElements(data, audio_data, 0);
    
    return result;
}

JNIEXPORT void JNICALL
Java_ai_kitt_snowboy_snowboyJNI_SnowboyDetect_1SetSensitivity(JNIEnv *env, jclass clazz, jlong detector_ptr, jstring sensitivity) {
    (void)env;
    (void)clazz;
    
#ifdef SNOWBOY_REAL
    if (detector_ptr > 0) {
        // 调用真实的SetSensitivity
        auto it = detector_map.find(detector_ptr);
        if (it != detector_map.end()) {
            const char* sensitivity_cstr = env->GetStringUTFChars(sensitivity, 0);
            it->second->SetSensitivity(sensitivity_cstr);
            env->ReleaseStringUTFChars(sensitivity, sensitivity_cstr);
            LOGI("Real SnowboyDetect SetSensitivity: %s", sensitivity_cstr);
            return;
        }
    }
#endif
    
    // 模拟SetSensitivity
    LOGI("Mock SnowboyDetect SetSensitivity called for ID: %lld", detector_ptr);
}

JNIEXPORT void JNICALL
Java_ai_kitt_snowboy_snowboyJNI_SnowboyDetect_1SetAudioGain(JNIEnv *env, jclass clazz, jlong detector_ptr, jfloat audio_gain) {
    (void)env;
    (void)clazz;
    
#ifdef SNOWBOY_REAL
    if (detector_ptr > 0) {
        // 调用真实的SetAudioGain
        auto it = detector_map.find(detector_ptr);
        if (it != detector_map.end()) {
            it->second->SetAudioGain(audio_gain);
            LOGI("Real SnowboyDetect SetAudioGain: %f", audio_gain);
            return;
        }
    }
#endif
    
    // 模拟SetAudioGain
    LOGI("Mock SnowboyDetect SetAudioGain: %f for ID: %lld", audio_gain, detector_ptr);
}

} // extern "C" 