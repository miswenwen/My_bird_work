LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(strip $(MTK_CLEARMOTION_SUPPORT)),no)
# if not support clearmotion, load a small video for clearmotion
LOCAL_ASSET_DIR := $(LOCAL_PATH)/assets_no_clearmotion
else
LOCAL_ASSET_DIR := $(LOCAL_PATH)/assets_clearmotion
endif

#BIRD_A200_CUSTOM add by qinzhifeng 20160413 begin
a200-icons :=
ifeq ($(strip $(BIRD_A200_CUSTOM)),yes)
a200-icons :=$(LOCAL_PATH)/a200-colorful-icons/res
endif
#BIRD_A200_CUSTOM add by qinzhifeng 20160413 end

res_dirs := $(a200-icons)

LOCAL_JAVA_LIBRARIES := bouncycastle conscrypt telephony-common ims-common \
                        mediatek-framework

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 android-support-v13 jsr305 \
                               com.mediatek.lbs.em2.utils \
                               com.mediatek.settings.ext
                               
LOCAL_MODULE_TAGS := optional

sensor_dir := ../FlipSensor

#update by liuzhiling 20160707
elder_res=$(LOCAL_PATH)/elder/res/
ifneq ($(strip $(BIRD_TWO_TAB_SETTINGS)),yes)
elder_res :=
endif
#liuqipeng
liu_res=$(LOCAL_PATH)/res_liu
#liuqipeng
LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) \
        $(call all-java-files-under, bird/src) \
        $(call all-java-files-under, $(sensor_dir)/src) \
        src/com/android/settings/EventLogTags.logtags
#fp_res/res add by lichengfeng for fingerprint res 20160623
LOCAL_RESOURCE_DIR :=$(liu_res) $(elder_res) $(res_dirs) $(LOCAL_PATH)/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/bird/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/fp_res/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res_ext
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/$(sensor_dir)/res

#bird: flip sensor, add by peibaosheng @20160714 begin
LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.bird.flipsensor
#bird: flip sensor, add by peibaosheng @20160714 end

LOCAL_PACKAGE_NAME := Settings
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
endif

include frameworks/opt/setupwizard/navigationbar/common.mk
include frameworks/opt/setupwizard/library/common.mk
include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
