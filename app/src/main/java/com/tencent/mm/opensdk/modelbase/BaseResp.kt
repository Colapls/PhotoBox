package com.tencent.mm.opensdk.modelbase

/** Stub for missing WeChat OpenSDK AAR. Local-only placeholder. */
open class BaseResp {
    object ErrCode {
        const val ERR_OK = 0
        const val ERR_USER_CANCEL = -2
        const val ERR_SENT_FAILED = -3
        const val ERR_AUTH_DENIED = -4
        const val ERR_UNSUPPORT = -5
    }
    var errCode: Int = 0
    var errStr: String? = null
}