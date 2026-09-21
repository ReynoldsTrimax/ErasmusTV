package com.erasmustv.app.core.util

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.util.Log

object DeviceCodecCapability {
    private const val TAG = "DeviceCodecCapability"

    @Volatile
    private var isHevcMain10SupportedCache: Boolean? = null

    /**
     * Checks if the device has a hardware or software decoder capable of HEVC Profile Main 10 (10-bit).
     * Returns false on devices/emulators that only support Main/High 8-bit profile.
     */
    fun isHevcMain10Supported(): Boolean {
        isHevcMain10SupportedCache?.let { return it }

        try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val codecInfos = codecList.codecInfos ?: emptyArray()

            for (info in codecInfos) {
                if (info == null || info.isEncoder) continue
                val types = try {
                    info.supportedTypes
                } catch (_: Throwable) {
                    null
                } ?: continue

                for (type in types) {
                    if (type.equals("video/hevc", ignoreCase = true)) {
                        val capabilities = try {
                            info.getCapabilitiesForType(type)
                        } catch (_: Throwable) {
                            null
                        } ?: continue

                        val profileLevels = capabilities.profileLevels ?: continue
                        for (pl in profileLevels) {
                            if (pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
                                pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 ||
                                pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus
                            ) {
                                logI(TAG, "Device supports HEVC Main 10 via decoder: ${info.name}")
                                isHevcMain10SupportedCache = true
                                return true
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            // In unit tests or environments without native media service, safely default
            logW(TAG, "Notice: Could not query MediaCodecList (${t.message}), defaulting to false")
        }

        logI(TAG, "Device does not support HEVC Main 10 profile")
        isHevcMain10SupportedCache = false
        return false
    }

    private fun logI(tag: String, msg: String) {
        try {
            Log.i(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }

    private fun logW(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (_: Throwable) {
            System.err.println("[$tag] $msg")
        }
    }

    /**
     * Allow overriding for testing or simulation.
     */
    fun setHevcMain10SupportedForTesting(supported: Boolean?) {
        isHevcMain10SupportedCache = supported
    }
}
