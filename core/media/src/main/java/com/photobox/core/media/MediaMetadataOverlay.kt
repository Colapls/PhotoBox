package com.photobox.core.media

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaMetadataOverlay(
    dateTakenMs: Long,
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier,
) {
    val takenAt = remember(dateTakenMs) { formatTakenAt(dateTakenMs) }
    val location = rememberGeocodedAddress(latitude, longitude)
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f),
        offset = Offset(0f, 1f),
        blurRadius = 4f,
    )

    if (takenAt == null && location == null) return

    Column(modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        if (takenAt != null) {
            Text(
                text = takenAt,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    shadow = textShadow,
                ),
            )
        }
        if (location != null) {
            Text(
                text = location,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow),
                modifier = if (takenAt != null) Modifier.padding(top = 4.dp) else Modifier,
            )
        }
    }
}

private fun formatTakenAt(dateTakenMs: Long): String? {
    if (dateTakenMs <= 0L) return null
    val formatter = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault())
    return formatter.format(Date(dateTakenMs))
}

private fun formatLocation(latitude: Double?, longitude: Double?): String? {
    // 实际显示交由 [rememberGeocodedAddress] 反地理编码（国家·省·市·区）。
    // 这里仅做空判断保留兼容签名（不再使用）。
    if (latitude == null || longitude == null) return null
    if (latitude == 0.0 && longitude == 0.0) return null
    return null
}
