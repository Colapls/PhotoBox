package com.photobox.feature.share

import android.content.Context
import com.photobox.feature.share.wechat.WeChatConstants
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 微信 API 单例封装。注册 AppID + 检测安装 + 创建 IWXAPI。
 * 真实的 AppID 通过 context.packageManager.getApplicationInfo(...).metaData 读取，
 * 由于 manifest metaData 在 app 模块声明，本类只接受外部传入的 appId。
 */
@Singleton
class WXApiManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var api: IWXAPI? = null

    /**
     * 初始化 IWXAPI。appId 通常从 BuildConfig.WECHAT_APP_ID 传入。
     */
    @Synchronized
    fun init(appId: String = WeChatConstants.PLACEHOLDER_APP_ID): IWXAPI {
        api?.let { return it }
        val instance = WXAPIFactory.createWXAPI(context, appId, true)
        instance.registerApp(appId)
        api = instance
        return instance
    }

    /**
     * 检测是否安装微信（未安装时 ShareDispatcher 应 Toast 提示而不崩溃）。
     */
    fun isWeChatInstalled(): Boolean = init().isWXAppInstalled

    /**
     * 获取当前 API（必须先调 init）。
     */
    fun api(): IWXAPI = init()
}