package com.photobox.core.common

object AppLockConstants {
    /**
     * 应用锁解锁后 30 秒内回到 App 不需要重新生物识别（spec 4.5）。
     */
    const val UNLOCK_TTL_MS: Long = 30_000L
}
