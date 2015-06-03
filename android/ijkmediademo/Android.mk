

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

drawee_dir := ../../../NTDrawee
res_dirs := res $(drawee_dir)/res

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/,$(res_dirs))

LOCAL_AAPT_FLAGS := \
	--auto-add-overlay \
	--extra-packages com.facebook.drawee
LOCAL_AAPT_FLAGS += --extra-packages com.facebook.drawee

LOCAL_PACKAGE_NAME := NTVideos

LOCAL_STATIC_JAVA_LIBRARIES += \
    drawee-pipeline-release-jar \
    drawee-release-jar \
    drawee-volley-release-jar \
    fbcore-release-jar \
    greendao-1.3.7-jar \
    imagepipeline-okhttp-release-jar \
    imagepipeline-release-jar

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += guava
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags


include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

include $(BUILD_MULTI_PREBUILT)



include $(call all-makefiles-under,$(LOCAL_PATH))
