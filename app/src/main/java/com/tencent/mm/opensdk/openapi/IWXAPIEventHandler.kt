package com.tencent.mm.opensdk.openapi

import android.content.Intent
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp

/** Stub for missing WeChat OpenSDK AAR. Local-only placeholder. */
interface IWXAPIEventHandler {
    fun onReq(req: BaseReq?)
    fun onResp(resp: BaseResp?)
}