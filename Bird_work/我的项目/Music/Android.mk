LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
	src/com/android/music/IMediaPlaybackService.aidl
#add by wangye for music share 20160919 begin
res_dirs=$(LOCAL_PATH)/bird/res
LOCAL_SRC_FILES += $(call all-java-files-under, bird/src)
LOCAL_RESOURCE_DIR :=$(res_dirs) $(LOCAL_PATH)/res
LOCAL_AAPT_FLAGS := --auto-add-overlay
#add by wangye for music share 20160919 end
LOCAL_PACKAGE_NAME := Music
# SPRD: remove
#LOCAL_SDK_VERSION := current

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
