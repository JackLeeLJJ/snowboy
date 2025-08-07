LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := snowboy-detect-android
LOCAL_SRC_FILES := snowboy-detect-swig.cc

# 包含头文件路径
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../include

# 链接静态库
LOCAL_STATIC_LIBRARIES := snowboy-detect

# 编译标志
LOCAL_CFLAGS := -O3 -fPIC
LOCAL_CPPFLAGS := -std=c++11 -O3 -fPIC

# 链接标志
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)

# 包含静态库
include $(CLEAR_VARS)
LOCAL_MODULE := snowboy-detect
LOCAL_SRC_FILES := ../../lib/android/armv7a/libsnowboy-detect.a
include $(PREBUILT_STATIC_LIBRARY) 