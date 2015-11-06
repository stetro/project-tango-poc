#
# Copyright 2014 Google Inc. All Rights Reserved.
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
#
LOCAL_PATH := $(call my-dir)
PROJECT_ROOT_FROM_JNI:= ../../../../..
PROJECT_ROOT:= $(call my-dir)/../../../../..

include $(CLEAR_VARS)
LOCAL_MODULE    := libaugmented_reality_jni_example
LOCAL_SHARED_LIBRARIES := tango_client_api
LOCAL_CFLAGS    := -std=c++11
MY_LOCAL_LIB:= /home/stetro/Source/tango-examples-c

LOCAL_SRC_FILES := augmented_reality_app.cc \
                   jni_interface.cc \
                   pose_data.cc \
                   scene.cc \
                   tango_event_data.cc \
                   $(MY_LOCAL_LIB)/tango-gl/axis.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/bounding_box.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/camera.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/conversions.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/drawable_object.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/frustum.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/gesture_camera.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/grid.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/goal_marker.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/line.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/mesh.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/shaders.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/trace.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/transform.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/util.cpp \
                   $(MY_LOCAL_LIB)/tango-gl/video_overlay.cpp

LOCAL_C_INCLUDES := $(MY_LOCAL_LIB)/tango-gl/include \
                    $(MY_LOCAL_LIB)/third-party/glm/

LOCAL_LDLIBS    := -llog -lGLESv2 -L$(SYSROOT)/usr/lib
include $(BUILD_SHARED_LIBRARY)

$(call import-add-path, $(MY_LOCAL_LIB))
$(call import-module,tango_client_api)
