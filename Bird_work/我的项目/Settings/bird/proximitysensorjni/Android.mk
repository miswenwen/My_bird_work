#ifeq ($(strip $(BIRD_PROXIMITY_CALIBRATION)), yes)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES += liblog libnativehelper
LOCAL_PRELINK_MODULE := false

LOCAL_C_INCLUDES := $(JNI_H_INCLUDE) 
LOCAL_SRC_FILES := \
	libproximity_jni.cpp

#LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libproximityjni
include $(BUILD_SHARED_LIBRARY)

#endif

