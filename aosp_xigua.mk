#
# Copyright (C) 2021-2024 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from xigua device
$(call inherit-product, device/oneplus/xigua/device.mk)

# Inherit some common PixelOS stuff.
$(call inherit-product, vendor/aosp/config/common_full_phone.mk)

PRODUCT_NAME := aosp_xigua
PRODUCT_DEVICE := xigua
PRODUCT_MANUFACTURER := OnePlus
PRODUCT_BRAND := OnePlus
PRODUCT_MODEL := PJA110

PRODUCT_SYSTEM_NAME := $(PRODUCT_MODEL)
PRODUCT_SYSTEM_DEVICE := OP5943L1

TARGET_BOOT_ANIMATION_RES := 1440
PRODUCT_GMS_CLIENTID_BASE := android-oneplus

PRODUCT_BUILD_PROP_OVERRIDES += \
    PRIVATE_BUILD_DESC="PJA110-user 14 UKQ1.231108.001 T.18a85be-38125-9b836 release-keys" \
    TARGET_DEVICE=$(PRODUCT_SYSTEM_DEVICE) \
    TARGET_PRODUCT=$(PRODUCT_SYSTEM_NAME)

BUILD_FINGERPRINT := OnePlus/PJA110/OP5943L1:14/UKQ1.231108.001/T.18a85be-38125-9b836:user/release-keys
