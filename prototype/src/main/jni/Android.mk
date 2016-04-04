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
NATIVE_LIBS:=$(LOCAL_PATH)/../../../../native-libraries/
GLM:=$(LOCAL_PATH)/../../../../native-libraries/glm
TANGO_GL:=$(LOCAL_PATH)/../../../../native-libraries/tango_gl
OPENCV:=/home/stetro/Source/opencv/platforms/build_android_arm/
BOOST_ANDROID_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/boost
EIGEN_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/eigen
CHISEL := $(LOCAL_PATH)/../../../../native-libraries/open_chisel


include $(CLEAR_VARS)


OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
include $(OPENCV)/OpenCV.mk

LOCAL_MODULE    := libaugmented_reality_jni_example
LOCAL_SHARED_LIBRARIES += tango_client_api
LOCAL_SHARED_LIBRARIES += tango_support_api


LOCAL_SRC_FILES += $(TANGO_GL)/axis.cc \
                   $(TANGO_GL)/bounding_box.cc \
                   $(TANGO_GL)/camera.cc \
                   $(TANGO_GL)/conversions.cc \
                   $(TANGO_GL)/drawable_object.cc \
                   $(TANGO_GL)/frustum.cc \
                   $(TANGO_GL)/gesture_camera.cc \
                   $(TANGO_GL)/grid.cc \
                   $(TANGO_GL)/goal_marker.cc \
                   $(TANGO_GL)/line.cc \
                   $(TANGO_GL)/cube.cc \
                   $(TANGO_GL)/mesh.cc \
                   $(TANGO_GL)/shaders.cc \
                   $(TANGO_GL)/trace.cc \
                   $(TANGO_GL)/transform.cc \
                   $(TANGO_GL)/util.cc \
                   $(CHISEL)/src/Chunk.cpp \
                   $(CHISEL)/src/ChunkManager.cpp \
                   $(CHISEL)/src/DistVoxel.cpp \
                   $(CHISEL)/src/ColorVoxel.cpp \
                   $(CHISEL)/src/geometry/AABB.cpp \
                   $(CHISEL)/src/geometry/Plane.cpp \
                   $(CHISEL)/src/geometry/Frustum.cpp \
                   $(CHISEL)/src/camera/Intrinsics.cpp \
                   $(CHISEL)/src/camera/PinholeCamera.cpp \
                   $(CHISEL)/src/pointcloud/PointCloud.cpp \
                   $(CHISEL)/src/ProjectionIntegrator.cpp \
                   $(CHISEL)/src/Chisel.cpp \
                   $(CHISEL)/src/mesh/Mesh.cpp \
                   $(CHISEL)/src/marching_cubes/MarchingCubes.cpp \
                   $(CHISEL)/src/io/PLY.cpp \
                   $(CHISEL)/src/geometry/Raycast.cpp \
                   ar_object.cc \
                   augmented_reality_app.cc \
                   jni_interface.cc \
                   pose_data.cc \
                   scene.cc \
                   chisel_mesh.cc \
                   plane_mesh.cc \
                   reconstruction_octree.cc \
                   reconstructor.cc \
                   convex_hull.cc \
                   point_cloud_drawable.cc \
                   yuv_drawable.cc \
                   depth_drawable.cc \
                   tango_event_data.cc

LOCAL_C_INCLUDES += $(TANGO_GL)/include \
                    $(GLM)/ \
                    $(BOOST_ANDROID_INCLUDE)/include \
                    $(EIGEN_INCLUDE) \
                    $(CHISEL)/include

SYSTEM_C_INCLUDES +=

LOCAL_CFLAGS    += -std=c++11 -mfloat-abi=softfp -mfpu=neon -march=armv7 -mthumb -O3

LOCAL_LDLIBS    +=  -lc -lm -llog -landroid -ldl -llog -lGLESv3 -L$(SYSROOT)/usr/lib

include $(BUILD_SHARED_LIBRARY)

$(call import-add-path, $(NATIVE_LIBS))
$(call import-module,tango_client_api)
$(call import-module,tango_support_api)
