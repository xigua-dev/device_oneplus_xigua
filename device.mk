#
# Copyright (C) 2021-2023 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# AAPT
PRODUCT_AAPT_CONFIG := normal
PRODUCT_AAPT_PREF_CONFIG := xxxhdpi

# Audio
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/audio/card-defs.xml:$(TARGET_COPY_OUT_VENDOR)/etc/card-defs.xml \
    $(LOCAL_PATH)/audio/mixer_paths.xml:$(TARGET_COPY_OUT_ODM)/etc/mixer_paths.xml \
    $(LOCAL_PATH)/audio/resourcemanager.xml:$(TARGET_COPY_OUT_ODM)/etc/resourcemanager.xml \
    $(LOCAL_PATH)/audio/usecaseKvManager.xml:$(TARGET_COPY_OUT_ODM)/etc/usecaseKvManager.xml \
    $(LOCAL_PATH)/audio/usecaseKvManager.xml:$(TARGET_COPY_OUT_VENDOR)/etc/usecaseKvManager.xml

# Boot animation
TARGET_SCREEN_HEIGHT := 2772
TARGET_SCREEN_WIDTH := 1240

# Fingerprint
PRODUCT_COPY_FILES += \
    prebuilts/vndk/v33/arm64/arch-arm64-armv8-a/shared/vndk-core/android.hardware.keymaster-V3-ndk.so:$(TARGET_COPY_OUT_ODM)/lib64/android.hardware.keymaster-V3-ndk.so

# Display
PRODUCT_SYSTEM_PROPERTIES += \
    persist.sys.brightness.low.gamma=true

# Overlays
DEVICE_PACKAGE_OVERLAYS += \
    $(LOCAL_PATH)/overlay-lineage

PRODUCT_PACKAGES += \
    OPlusFrameworksResTarget \
    OPlusSettingsProviderResTarget \
    OPlusSettingsResTarget \
    OPlusSystemUIResTarget

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)

# Inherit from the common OEM chipset makefile.
$(call inherit-product, device/oneplus/sm8550-common/common.mk)

# Inherit from the proprietary files makefile.
$(call inherit-product, vendor/oneplus/xigua/xigua-vendor.mk)

ifneq ($(INCLUDE_GAPPS),)
$(warning "Inheriting gapps from vendor/gapps")
$(call inherit-product, vendor/gapps/arm64/arm64-vendor.mk)
endif
