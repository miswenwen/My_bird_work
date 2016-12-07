
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

#LOCAL_SRC_FILES := $(call all-java-files-under, src) \
#       src/com/yunos/contactservice/IContactService.aidl

ifeq ($(YUNOS_SUPPORT_COLORTEST),yes)
aui_color_test_file := $(LOCAL_PATH)/aui_color_test.sh
aui_color_test_file := $(wildcard $(aui_color_test_file))
ifneq ($(aui_color_test_file),)
$(info aui_color_test_file=$(aui_color_test_file))
$(shell $(SHELL) $(aui_color_test_file) $(LOCAL_PATH))
endif
endif

hwdroid_dir := ../Aui/HWDroidRes

$(warning "Build Contacts, PLATFORM_SDK_VERSION:"$(PLATFORM_SDK_VERSION))
ifeq (true,$(call if-sdk-version-greater-than,22))
    PLATFORM_DEPENDENT_DIR := platforms/src_23
else
    PLATFORM_DEPENDENT_DIR := platforms/src_22
endif

$(warning "Build Contacts, VENDOR:"$(YUNOS_PLATFORM))
ifeq ($(YUNOS_PLATFORM), MTK)
EXTRA_VENDOR_SOURCE := vendors/src_mtk
EXTRA_VENDOR_JAVA_LIBS := ims-common mediatek-framework
else ifeq ($(YUNOS_PLATFORM), QUALCOMM)
EXTRA_VENDOR_SOURCE := vendors/src_qcom
EXTRA_VENDOR_JAVA_LIBS :=
else ifeq ($(YUNOS_PLATFORM), SPREADTRUM)
EXTRA_VENDOR_SOURCE := vendors/src_sprd
EXTRA_VENDOR_JAVA_LIBS := telephony-common
endif

src_dirs := src $(PLATFORM_DEPENDENT_DIR) $(EXTRA_VENDOR_SOURCE)

res_dirs := res $(hwdroid_dir)/res bird/res

#bird/src add by lichengfeng for doov ringdone 20160601
LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs)) \
				$(call all-java-files-under, bird/src)

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.hwdroid.res


LOCAL_STATIC_JAVA_LIBRARIES := \
    guava \
    com.android.vcard \
    alicontactsinterface fastjson \
    hwdroid_v4 image-loader \
    yunosui

LOCAL_PACKAGE_NAME := Contacts
LOCAL_CERTIFICATE := platform
LOCAL_JAVA_LIBRARIES := telephony-common hwdroid yunos_v4 $(EXTRA_VENDOR_JAVA_LIBS)

LOCAL_PRIVILEGED_MODULE := true
LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags
LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
        alicontactsinterface:libs/alicontactsinterface.jar \
        fastjson:libs/fastjson-1.1.34.android.jar \
        image-loader:libs/universal-image-loader-1.8.6-with-sources.jar

include $(BUILD_MULTI_PREBUILT)

# Use the folloing include to make our test apk.
#include $(call all-makefiles-under,$(LOCAL_PATH))
