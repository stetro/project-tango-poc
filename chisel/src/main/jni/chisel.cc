//
// Created by stetro on 01.12.15.
//
#include "chisel.h"


namespace chisel {

    void ChiselApplication::addPoints(JNIEnv *env, jfloatArray vertices) {

    }

    jfloatArray ChiselApplication::getMesh(JNIEnv * env) {
        jfloatArray array = env->NewFloatArray(0);
        return array;
    }

    void ChiselApplication::clear(JNIEnv * env) {

    }

    ChiselApplication::ChiselApplication(){

    }
    ChiselApplication::~ChiselApplication(){

    }


}