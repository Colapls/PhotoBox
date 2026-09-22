package com.photobox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PhotoBoxApplication : Application() {
    // 应用锁相关 LifecycleObserver 已移除。
}