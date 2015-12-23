
LOCAL_PATH := $(call my-dir)
PROJECT_ROOT_FROM_JNI := ../../../../..
PROJECT_ROOT := $(LOCAL_PATH)/$(PROJECT_ROOT_FROM_JNI)

EIGEN_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/Eigen
NDK_TOOLCHAIN_VERSION=4.9

# Project and linking

include $(CLEAR_VARS)

LOCAL_MODULE := chisel
LOCAL_CFLAGS := -std=gnu++11
LOCAL_C_INCLUDES := $(EIGEN_INCLUDE)

LOCAL_SRC_FILES := jni_interface.cc \
                   chisel.cc


LOCAL_LDLIBS := -lstdc++ -lc -lm -llog -landroid -ldl -lGLESv2 -lEGL

LOCAL_CFLAGS += -mfloat-abi=softfp -mfpu=neon -march=armv7 -mthumb -O3

include $(BUILD_SHARED_LIBRARY)

