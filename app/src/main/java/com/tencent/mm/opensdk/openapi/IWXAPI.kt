package com.tencent.mm.opensdk.openapi

import com.tencent.mm.opensdk.modelbase.BaseReq

/** Stub for missing WeChat OpenSDK AAR. Local-only placeholder. */
interface IWXAPI {
    val isWXAppInstalled: Boolean
    fun registerApp(appId: String): Boolean
    fun handleIntent(intent: android.content.Intent?, handler: IWXAPIEventHandler?): Boolean
    fun sendReq(req: BaseReq?): Boolean
}