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
TANGO_C_EXAMPLES:=/home/stetro/Source/tango-examples-c
OPENCV:=/home/stetro/Source/opencv/platforms/build_android_arm/

include $(CLEAR_VARS)


OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
include $(OPENCV)/OpenCV.mk

LOCAL_MODULE    := libaugmented_reality_jni_example
LOCAL_SHARED_LIBRARIES += tango_client_api
LOCAL_CFLAGS    += -std=c++11

LOCAL_SRC_FILES += $(TANGO_C_EXAMPLES)/tango-gl/axis.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/bounding_box.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/camera.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/conversions.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/drawable_object.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/frustum.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/gesture_camera.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/grid.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/goal_marker.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/line.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/cube.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/mesh.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/shaders.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/trace.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/transform.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/util.cpp \
                   $(TANGO_C_EXAMPLES)/tango-gl/video_overlay.cpp \
                   augmented_reality_app.cc \
                   jni_interface.cc \
                   pose_data.cc \
                   scene.cc \
                   point_cloud_drawable.cc \
                   yuv_drawable.cc \
                   depth_drawable.cc \
                   tango_event_data.cc

LOCAL_C_INCLUDES += $(TANGO_C_EXAMPLES)/tango-gl/include \
                    $(TANGO_C_EXAMPLES)/third-party/glm/

LOCAL_LDLIBS    += -llog -lGLESv3 -L$(SYSROOT)/usr/lib
include $(BUILD_SHARED_LIBRARY)

$(call import-add-path, $(TANGO_C_EXAMPLES))
$(call import-module,tango_client_api)
