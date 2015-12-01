//
// Created by stetro on 01.12.15.
//

#ifndef MASTERPROTOTYPE_CONSTRUCTNATIVE_H
#define MASTERPROTOTYPE_CONSTRUCTNATIVE_H

#include <jni.h>
#include <cstdlib>
#include <android/log.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "Native",__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "Native",__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "Native",__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "Native",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "Native",__VA_ARGS__)

namespace constructnative {

    class Application {
    public:
        Application();

        ~Application();

        jfloatArray reconstruct(JNIEnv *env, jfloatArray vertices);

        void freeArray(JNIEnv *env);

    private:
        jfloatArray array;
    };
}

#endif //MASTERPROTOTYPE_CONSTRUCTNATIVE_H
