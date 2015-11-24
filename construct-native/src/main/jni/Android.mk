# Copyright 2015 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)
PROJECT_ROOT_FROM_JNI := ../../../../..
PROJECT_ROOT := $(LOCAL_PATH)/$(PROJECT_ROOT_FROM_JNI)
MY_SOURCE_DIR := /home/stetro/Source/tango-examples-c
PCL_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/pcl-android
BOOST_ANDROID_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/boost-android
EIGEN_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/eigen

include $(CLEAR_VARS)
LOCAL_MODULE := plane_fitting_jni_example
LOCAL_SHARED_LIBRARIES := tango_client_api tango_support_api
LOCAL_CFLAGS := -std=c++11
LOCAL_C_INCLUDES := $(MY_SOURCE_DIR)/tango-gl/include \
                    $(MY_SOURCE_DIR)/third-party/glm \
                    $(PCL_INCLUDE)/include/pcl-1.6 \
                    $(BOOST_ANDROID_INCLUDE)/include \
                    $(EIGEN_INCLUDE)
LOCAL_SRC_FILES := jni_interface.cc \
                   plane_fitting.cc \
                   plane_fitting_application.cc \
                   point_cloud.cc \
                   $(MY_SOURCE_DIR)/tango-gl/bounding_box.cpp \
                   $(MY_SOURCE_DIR)/tango-gl/camera.cpp \
                   $(MY_SOURCE_DIR)/tango-gl/conversions.cpp \
                   $(MY_SOURCE_DIR)/tango-gl/cube.cpp \
                   $(MY_SOURCE_DIR)/tango-gl/drawable_object.cpp \
                   $(MY_SOURCE_DIR)/tango-gl/mesh.cpp \
                   $(MY_SOURCE_DIR)/tango-gl/shaders.cpp \
                   $(MY_SOURCE_DIR)/tango-gl/transform.cpp \
                   $(MY_SOURCE_DIR)/tango-gl/util.cpp \
                   $(MY_SOURCE_DIR)/tango-gl/video_overlay.cpp
LOCAL_LDLIBS := -lGLESv2 -llog -L$(SYSROOT)/usr/lib -L$(PCL_INCLUDE)/lib -L$(BOOST_ANDROID_INCLUDE)/lib
include $(BUILD_SHARED_LIBRARY)

$(call import-add-path,$(MY_SOURCE_DIR))
$(call import-module,tango_client_api)
$(call import-module,tango_support_api)
