package com.photobox.core.data.mediastore

/**
 * 媒体文件（图片或视频）的统一模型。
 * mediaId: ContentProvider 的 _ID（Long）
 * uri: content://media/external/images/media/<id> 或 videos/media/<id>
 * isVideo: 区分图片/视频（流中暂只展示图片；视频在 Plan 3 加入时处理）
 * dateTakenMs: DATE_TAKEN（毫秒）；0 表示相册里没记录
 */
data class MediaItem(
    val mediaId: Long,
    val uri: String,
    val isVideo: Boolean,
    val dateTakenMs: Long,
    val durationMs: Long = 0L, // 视频时长；图片为 0
    val mimeType: String,
)
