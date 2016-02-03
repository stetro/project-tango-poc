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

PCL_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/pcl
BOOST_ANDROID_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/boost
FLANN_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/flann
EIGEN_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/eigen


# PCL libraries

PCL_STATIC_LIB_DIR := $(PCL_INCLUDE)/lib
BOOST_STATIC_LIB_DIR := $(BOOST_ANDROID_INCLUDE)/lib
FLANN_STATIC_LIB_DIR := $(FLANN_INCLUDE)/lib

PCL_STATIC_LIBRARIES :=     pcl_common pcl_geometry pcl_kdtree pcl_octree pcl_sample_consensus pcl_surface \
							pcl_features pcl_keypoints pcl_search pcl_tracking pcl_filters pcl_ml \
							pcl_registration pcl_segmentation
BOOST_STATIC_LIBRARIES :=   boost_date_time boost_iostreams boost_regex boost_system \
						    boost_filesystem boost_program_options boost_signals boost_thread
FLANN_STATIC_LIBRARIES :=   flann_s flann_cpp_s


define build_pcl_static
	include $(CLEAR_VARS)
	LOCAL_MODULE:=$1
	LOCAL_SRC_FILES:=$(PCL_STATIC_LIB_DIR)/lib$1.a
	include $(PREBUILT_STATIC_LIBRARY)
endef

define build_boost_static
	include $(CLEAR_VARS)
	LOCAL_MODULE:=$1
	LOCAL_SRC_FILES:=$(BOOST_STATIC_LIB_DIR)/lib$1.a
	include $(PREBUILT_STATIC_LIBRARY)
endef

define build_flann_static
	include $(CLEAR_VARS)
	LOCAL_MODULE:=$1
	LOCAL_SRC_FILES:=$(FLANN_STATIC_LIB_DIR)/lib$1.a
	include $(PREBUILT_STATIC_LIBRARY)
endef

$(foreach module,$(PCL_STATIC_LIBRARIES),$(eval $(call build_pcl_static,$(module))))
$(foreach module,$(BOOST_STATIC_LIBRARIES),$(eval $(call build_boost_static,$(module))))
$(foreach module,$(FLANN_STATIC_LIBRARIES),$(eval $(call build_flann_static,$(module))))

# Project build

include $(CLEAR_VARS)


OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
include $(OPENCV)/OpenCV.mk

LOCAL_MODULE    := libaugmented_reality_jni_example
LOCAL_SHARED_LIBRARIES += tango_client_api
LOCAL_CFLAGS    += -std=c++11 -mfloat-abi=softfp -mfpu=neon -march=armv7 -mthumb -O3

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
                   augmented_reality_app.cc \
                   jni_interface.cc \
                   pose_data.cc \
                   scene.cc \
                   chisel_mesh.cc \
                   point_cloud_drawable.cc \
                   yuv_drawable.cc \
                   depth_drawable.cc \
                   tango_event_data.cc

LOCAL_C_INCLUDES += $(TANGO_C_EXAMPLES)/tango-gl/include \
                    $(TANGO_C_EXAMPLES)/third-party/glm/ \
                    $(PCL_INCLUDE)/include/pcl-1.6 \
                    $(BOOST_ANDROID_INCLUDE)/include \
                    $(EIGEN_INCLUDE) \
                    $(FLANN_INCLUDE)/include

LOCAL_LDFLAGS += -L$(PCL_INCLUDE)/lib  \
                 -L$(BOOST_ANDROID_INCLUDE)/lib \
                 -L$(FLANN_INCLUDE)/lib

LOCAL_SHARED_LIBRARIES   += pcl_common pcl_geometry pcl_search pcl_kdtree pcl_octree pcl_sample_consensus \
                            pcl_surface pcl_features pcl_filters pcl_keypoints pcl_tracking pcl_ml \
                            pcl_registration pcl_segmentation

LOCAL_SHARED_LIBRARIES   += boost_date_time boost_iostreams boost_regex boost_system \
                            boost_filesystem boost_program_options boost_signals boost_thread

LOCAL_SHARED_LIBRARIES   += flann flann_cpp

LOCAL_LDLIBS    +=  -llog -lGLESv3 -L$(SYSROOT)/usr/lib \
                    -lpcl_common -lpcl_geometry -lpcl_search -lpcl_kdtree -lpcl_octree -lpcl_sample_consensus \
    				-lpcl_surface -lpcl_features -lpcl_filters -lpcl_keypoints -lpcl_tracking -lpcl_ml \
    				-lpcl_registration -lpcl_segmentation \
                    -lflann -lflann_cpp

include $(BUILD_SHARED_LIBRARY)

$(call import-add-path, $(TANGO_C_EXAMPLES))
$(call import-module,tango_client_api)
