/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include "chisel.h"

static chisel::ChiselApplication chiselApplication;


#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_de_stetro_master_chisel_JNIInterface_clear(JNIEnv* env, jobject /*obj*/) {
    chiselApplication.clear(env);
}

JNIEXPORT void JNICALL
Java_de_stetro_master_chisel_JNIInterface_update(JNIEnv* env, jobject /*obj*/) {
chiselApplication.update(env);
}

JNIEXPORT jfloatArray JNICALL
Java_de_stetro_master_chisel_JNIInterface_getMesh(
        JNIEnv* env, jobject /*obj*/) {
    return chiselApplication.getMesh(env);
}

JNIEXPORT void JNICALL
Java_de_stetro_master_chisel_JNIInterface_addPoints(
        JNIEnv* env, jobject /*obj*/, jfloatArray vertices, jfloatArray transformation) {
chiselApplication.addPoints(env, vertices, transformation);
}

#ifdef __cplusplus
}
#endif