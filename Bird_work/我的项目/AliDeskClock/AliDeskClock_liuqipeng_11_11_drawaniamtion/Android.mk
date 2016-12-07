LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(YUNOS_SUPPORT_COLORTEST),yes)
aui_color_test_file := $(LOCAL_PATH)/aui_color_test.sh
aui_color_test_file := $(wildcard $(aui_color_test_file))
ifneq ($(aui_color_test_file),)
$(info aui_color_test_file=$(aui_color_test_file))
$(shell $(SHELL) $(aui_color_test_file) $(LOCAL_PATH))
endif
endif

VENDOR := $(YUNOS_PLATFORM)

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := framework \
        hwdroid yunos_v4

hwdroid_res_dir := ../Aui/HWDroidRes

src_dirs := src
res_dirs := res $(hwdroid_res_dir)/res

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res_ext $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay\
    --extra-packages com.hwdroid.res

LOCAL_STATIC_JAVA_LIBRARIES := \
    hwdroid_v4 \
    yunosui

LOCAL_JAVA_LIBRARIES += aliyun-aml

LOCAL_REQUIRED_MODULES := libvariablespeed

LOCAL_PACKAGE_NAME := DeskClock
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_OVERRIDES_PACKAGES := AlarmClock

ifeq ($(VENDOR), MTK)
$(warning echo mtk)
LOCAL_PROGUARD_ENABLED := disabled
endif
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
