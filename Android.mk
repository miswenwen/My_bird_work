#
# Copyright (C) 2008 The Android Open Source Project
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
include $(CLEAR_VARS)

LOCAL_EMMA_COVERAGE_FILTER := +com.yunos.calculator.*

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := arity \
         guava

LOCAL_JAVA_LIBRARIES :=  hwdroid yunos_v4
##liuqipeng new added 1212
#LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) \
        $(call all-java-files-under, bird/src) \
#liuqipeng end 1212
LOCAL_PACKAGE_NAME := AliCalculator
LOCAL_CERTIFICATE := platform

LOCAL_EMMA_COVERAGE_FILTER := @$(LOCAL_PATH)/emma_filter_classes,--$(LOCAL_PATH)/emma_filter_method

LOCAL_PROGUARD_ENABLED := custom
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
##liuqipeng new added 1212
bird_res=$(LOCAL_PATH)/bird/res
LOCAL_RESOURCE_DIR :=$(bird_res) $(LOCAL_PATH)/res
#bird: flip sensor, add by peibaosheng @20160714 begin
LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
	--extra-packages com.bird.calculator
#bird: flip sensor, add by peibaosheng @20160714 end
#liuqipeng end 1212
include $(BUILD_PACKAGE)
include $(CLEAR_VARS)
##################################################
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := arity:libs/arity-2.1.2.jar
include $(BUILD_MULTI_PREBUILT)

# Use the folloing include to make our test apk.
# include $(call all-makefiles-under,$(LOCAL_PATH))
