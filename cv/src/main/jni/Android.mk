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
C_EXAMPLES:=/home/stetro/Source/tango-examples-c
OPENCV:=/home/stetro/Source/opencv/platforms/build_android_arm/



include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
include $(OPENCV)/OpenCV.mk

LOCAL_MODULE    := libvideo_overlay_jni_example
LOCAL_SHARED_LIBRARIES += tango_client_api
LOCAL_CFLAGS    += -std=c++11

LOCAL_SRC_FILES += jni_interface.cc \
                   yuv_drawable.cc \
                   video_overlay_app.cc \
                   $(C_EXAMPLES)/tango-gl/axis.cpp \
                   $(C_EXAMPLES)/tango-gl/bounding_box.cpp \
                   $(C_EXAMPLES)/tango-gl/camera.cpp \
                   $(C_EXAMPLES)/tango-gl/conversions.cpp \
                   $(C_EXAMPLES)/tango-gl/drawable_object.cpp \
                   $(C_EXAMPLES)/tango-gl/frustum.cpp \
                   $(C_EXAMPLES)/tango-gl/gesture_camera.cpp \
                   $(C_EXAMPLES)/tango-gl/grid.cpp \
                   $(C_EXAMPLES)/tango-gl/goal_marker.cpp \
                   $(C_EXAMPLES)/tango-gl/line.cpp \
                   $(C_EXAMPLES)/tango-gl/mesh.cpp \
                   $(C_EXAMPLES)/tango-gl/shaders.cpp \
                   $(C_EXAMPLES)/tango-gl/trace.cpp \
                   $(C_EXAMPLES)/tango-gl/transform.cpp \
                   $(C_EXAMPLES)/tango-gl/util.cpp \
                   $(C_EXAMPLES)/tango-gl/video_overlay.cpp

LOCAL_C_INCLUDES += $(C_EXAMPLES)/tango-gl/include \
                    $(C_EXAMPLES)/third-party/glm

LOCAL_LDLIBS    += -llog -lm -lGLESv2 -L$(SYSROOT)/usr/lib



include $(BUILD_SHARED_LIBRARY)

$(call import-add-path, $(C_EXAMPLES))
$(call import-module,tango_client_api)


