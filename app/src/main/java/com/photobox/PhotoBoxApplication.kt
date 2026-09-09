package com.photobox

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.photobox.feature.applock.AppLockLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PhotoBoxApplication : Application() {

    @Inject lateinit var appLockLifecycleObserver: AppLockLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLockLifecycleObserver)
    }
}
