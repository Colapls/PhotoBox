package com.photobox.feature.share

import android.content.Context
import android.graphics.Bitmap
import com.photobox.feature.share.wechat.WeChatConstants
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXImageObject
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ShareResult {
    data object Success : ShareResult
    data class Failure(val message: String) : ShareResult
    data object WeChatNotInstalled : ShareResult
}

@Singleton
class ShareDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wxApiManager: WXApiManager,
    private val resultBus: ShareResultBus,
) {
    /**
     * 分享多张图片（已加载为 Bitmap）。Plan 3 限制：最多 9 张。
     * 实际异步执行 sendReq；结果通过 [ShareResultBus] 流式返回。
     */
    fun shareImages(bitmaps: List<Bitmap>, scene: Int = WeChatConstants.SCENE_SESSION) {
        if (bitmaps.isEmpty()) {
            resultBus.emit(ShareResult.Failure("无图片可分享"))
            return
        }
        if (bitmaps.size > 9) {
            resultBus.emit(ShareResult.Failure("最多分享 9 张"))
            return
        }
        if (!wxApiManager.isWeChatInstalled()) {
            resultBus.emit(ShareResult.WeChatNotInstalled)
            return
        }

        val req = buildReq(bitmaps, scene)
        val sent = wxApiManager.api().sendReq(req)
        if (!sent) {
            resultBus.emit(ShareResult.Failure("启动微信失败"))
        }
        // 成功 / 失败 / 取消 由 WXEntryActivity.onResp 回调，通过 ShareResultBus 投递
    }

    private fun buildReq(bitmaps: List<Bitmap>, scene: Int): SendMessageToWX.Req {
        val first = bitmaps.first()
        val media = WXMediaMessage().apply {
            title = "PhotoBox"
            description = "分享了 ${bitmaps.size} 张照片"
            mediaObject = WXImageObject(first)
            thumbData = compressThumb(first)
        }
        return SendMessageToWX.Req().apply {
            transaction = "photobox_share_${System.currentTimeMillis()}"
            message = media
            this.scene = scene
        }
    }

    private fun compressThumb(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        var bytes = stream.toByteArray()
        // 缩到 ≤ 32KB
        while (bytes.size > 32 * 1024 && bitmap.width > 64) {
            val scaled = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
            val s = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, s)
            bytes = s.toByteArray()
        }
        return bytes
    }
}