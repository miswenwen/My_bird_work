# Author:Wang Lei

ifeq ($(BIRD_ACCESS_CONTROL), yes)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

#add by meifangting 20150922 begin
ifeq ($(strip $(BLACK_UI_STYLE)),yes)
blackui_style := blackui/res
LOCAL_MANIFEST_FILE := blackui/AndroidManifest.xml
endif
res_dirs := $(blackui_style) res
#add by meifangting 20150922 end

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs)) 
LOCAL_AAPT_FLAGS := \
    --auto-add-overlay 
    
LOCAL_PACKAGE_NAME := AccessControl
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
#liuqipeng log off
#LOCAL_STATIC_JAVA_LIBRARIES := fingerprints.framework

LOCAL_PROGUARD_ENABLED:= disabled 

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

endif


