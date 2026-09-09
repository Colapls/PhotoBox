package com.photobox.feature.share.wechat

object WeChatConstants {
    /**
     * 占位 AppID：24 字符全 0，用于本地调试。
     * TODO: 替换为真实 AppID（需在微信开放平台注册并通过审核）。
     */
    const val PLACEHOLDER_APP_ID: String = "wx0000000000000000"

    /** 微信朋友圈场景值。 */
    const val SCENE_TIMELINE: Int = 1  // WXScene.TIMELINE

    /** 微信好友场景值。 */
    const val SCENE_SESSION: Int = 0   // WXScene.SESSION

    /** WXEntryActivity 的 action，微信 SDK 通过此 action 回调。 */
    const val ACTION_WX_ENTRY: String = "com.photobox.app.wxapi.WX_ENTRY_ACTION"
}