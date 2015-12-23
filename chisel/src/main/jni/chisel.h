//
// Created by stetro on 01.12.15.
//

#ifndef MASTERPROTOTYPE_chisel_H
#define MASTERPROTOTYPE_chisel_H

#include <jni.h>
#include <cstdlib>
#include <android/log.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "Native",__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "Native",__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "Native",__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "Native",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "Native",__VA_ARGS__)

namespace chisel {

    class ChiselApplication {
    public:
        ChiselApplication();

        ~ChiselApplication();

        void addPoints(JNIEnv *env, jfloatArray vertices);

        jfloatArray getMesh(JNIEnv *env);

        void clear(JNIEnv *env);

    };


}

#endif //MASTERPROTOTYPE_chisel_H
