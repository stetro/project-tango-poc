
LOCAL_PATH := $(call my-dir)
PROJECT_ROOT_FROM_JNI := ../../../../..
PROJECT_ROOT := $(LOCAL_PATH)/$(PROJECT_ROOT_FROM_JNI)

EIGEN_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/Eigen
CHISEL := $(LOCAL_PATH)/../../../../native-libraries/open_chisel
BOOST:= /Users/stetro/Source/Boost-for-Android-Prebuilt/boost_1_53_0


# Building Boost




# Project and linking

include $(CLEAR_VARS)

LOCAL_MODULE := chisel

LOCAL_C_INCLUDES := $(EIGEN_INCLUDE) \
                    $(CHISEL)/include

LOCAL_SRC_FILES := jni_interface.cc \
                   chisel.cc \
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
                   $(CHISEL)/src/geometry/Raycast.cpp


LOCAL_CFLAGS += -I$(BOOST)/include
LOCAL_LDLIBS := -lstdc++ -lc -lm -llog -landroid -ldl -lGLESv2 -lEGL


LOCAL_CFLAGS += -mfloat-abi=softfp -mfpu=neon -march=armv7 -mthumb -O3

include $(BUILD_SHARED_LIBRARY)

