package com.photobox.wechat

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import com.photobox.feature.share.ShareResult
import com.photobox.feature.share.ShareResultBus
import com.photobox.feature.share.wechat.WeChatConstants
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WXEntryActivity : Activity(), IWXAPIEventHandler {

    @Inject lateinit var resultBus: ShareResultBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appId = readAppIdFromManifest() ?: WeChatConstants.PLACEHOLDER_APP_ID
        val api = WXAPIFactory.createWXAPI(this, appId)
        api.handleIntent(intent, this)
    }

    private fun readAppIdFromManifest(): String? = try {
        val appInfo = packageManager.getApplicationInfo(
            packageName,
            PackageManager.GET_META_DATA,
        )
        appInfo.metaData?.getString("WX_APP_ID")
    } catch (e: Exception) {
        null
    }

    override fun onResp(resp: BaseResp?) {
        val result = when (resp?.errCode) {
            BaseResp.ErrCode.ERR_OK -> ShareResult.Success
            BaseResp.ErrCode.ERR_USER_CANCEL -> ShareResult.Failure("用户取消")
            BaseResp.ErrCode.ERR_SENT_FAILED -> ShareResult.Failure("发送失败")
            else -> ShareResult.Failure(resp?.errStr ?: "未知错误")
        }
        resultBus.emit(result)
        finish()
    }

    override fun onReq(req: BaseReq?) {
        finish()
    }
}