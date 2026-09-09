package com.photobox.feature.share

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 微信回调结果总线：WXEntryActivity → 业务层订阅。
 */
@Singleton
class ShareResultBus @Inject constructor() {
    private val _results = MutableSharedFlow<ShareResult>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val results: SharedFlow<ShareResult> = _results.asSharedFlow()

    fun emit(result: ShareResult) {
        _results.tryEmit(result)
    }
}