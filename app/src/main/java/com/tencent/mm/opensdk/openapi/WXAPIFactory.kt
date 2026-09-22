package com.tencent.mm.opensdk.openapi

import android.content.Context

/** Stub for missing WeChat OpenSDK AAR. Local-only placeholder. */
object WXAPIFactory {
    fun createWXAPI(context: Context, appId: String, register: Boolean = false): IWXAPI = StubWXAPI()
    private class StubWXAPI : IWXAPI {
        override val isWXAppInstalled: Boolean get() = false
        override fun registerApp(appId: String) = true
        override fun handleIntent(intent: android.content.Intent?, handler: IWXAPIEventHandler?) = false
        override fun sendReq(req: com.tencent.mm.opensdk.modelbase.BaseReq?) = false
    }
}