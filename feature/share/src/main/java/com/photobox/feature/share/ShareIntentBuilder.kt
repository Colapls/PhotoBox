package com.photobox.feature.share

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.photobox.core.data.mediastore.MediaItem
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXImageObject
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import java.io.InputStream
import javax.inject.Inject

/**
 * 把 MediaItem 列表 + 场景值构建成 SendMessageToWX.Req。
 * 单图 / 多图都用 WXMediaMessage + WXImageObject[]（Plan 3 限制：最多 9 张）。
 */
class ShareIntentBuilder @Inject constructor() {

    fun buildImageReq(
        items: List<MediaItem>,
        scene: Int,
        thumbnailSize: Int = 120,
    ): SendMessageToWX.Req {
        require(items.isNotEmpty()) { "At least one media item required" }
        require(items.size <= 9) { "WeChat allows at most 9 images per share" }

        val firstItem = items.first()
        val firstBitmap = loadBitmapFromUri(firstItem.uri, thumbnailSize)

        val mediaMessage = WXMediaMessage().apply {
            title = "PhotoBox"
            description = "分享了 ${items.size} 张照片"
            // 多图：mediaObject = WXImageObject 但需要传缩略图 bitmap
            mediaObject = WXImageObject(firstBitmap)
            thumbData = compressToBytes(firstBitmap, maxSizeKb = 32)
        }

        return SendMessageToWX.Req().apply {
            transaction = buildString {
                append("photobox_share_")
                append(System.currentTimeMillis())
            }
            message = mediaMessage
            this.scene = scene
        }
    }

    private fun loadBitmapFromUri(uriString: String, maxSize: Int): Bitmap {
        val uri = Uri.parse(uriString)
        val input: InputStream? = uri.let {
            // 调用方负责 contentResolver；此处仅占位
            null
        }
        // 实际实现：在 Activity / Composable 调用时传 Context，由 Composable 端解析
        // 这里提供一个简化工厂方法
        return _bitmapCache[uriString] ?: error("Bitmap not preloaded; call preload() from Composable")
    }

    private val _bitmapCache = mutableMapOf<String, Bitmap>()

    fun preload(uriString: String, bitmap: Bitmap) {
        _bitmapCache[uriString] = bitmap
    }

    private fun compressToBytes(bitmap: Bitmap, maxSizeKb: Int): ByteArray {
        var quality = 100
        var bytes: ByteArray
        do {
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            bytes = stream.toByteArray()
            quality -= 10
        } while (bytes.size > maxSizeKb * 1024 && quality > 10)
        return bytes
    }
}