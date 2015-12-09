
LOCAL_PATH := $(call my-dir)
PROJECT_ROOT_FROM_JNI := ../../../../..
PROJECT_ROOT := $(LOCAL_PATH)/$(PROJECT_ROOT_FROM_JNI)
PCL_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/pcl
BOOST_ANDROID_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/boost
FLANN_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/flann
EIGEN_INCLUDE := $(LOCAL_PATH)/../../../../native-libraries/eigen
NDK_TOOLCHAIN_VERSION=4.9

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



# Project and linking

include $(CLEAR_VARS)

LOCAL_MODULE := constructnative
LOCAL_CFLAGS := -std=gnu++11
LOCAL_C_INCLUDES := $(PCL_INCLUDE)/include/pcl-1.6 \
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

LOCAL_SRC_FILES := jni_interface.cc \
                   constructnative.cc


LOCAL_LDLIBS := -lstdc++ -lc -lm -llog -landroid -ldl -lGLESv2 -lEGL \
				-lpcl_common -lpcl_geometry -lpcl_search -lpcl_kdtree -lpcl_octree -lpcl_sample_consensus \
				-lpcl_surface -lpcl_features -lpcl_filters -lpcl_keypoints -lpcl_tracking -lpcl_ml \
				-lpcl_registration -lpcl_segmentation \
                -lflann -lflann_cpp


LOCAL_CFLAGS += -mfloat-abi=softfp -mfpu=neon -march=armv7 -mthumb -O3

include $(BUILD_SHARED_LIBRARY)

