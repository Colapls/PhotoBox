package com.photobox.core.media

import android.location.Address
import android.location.Geocoder
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 进程内 geocode 缓存。key 用 lat/lon × 1000 量化到 ~110m 网格，
 * 同一地点多张照片共用一次反地理编码结果，避免重复调用 Geocoder（它走网络，有配额）。
 * 500 条上限 → ~500KB 字符串缓存。
 */
private val addressCache = LruCache<Pair<Int, Int>, String?>(500)

/**
 * 异步逆地理编码：返回「中国 · 浙江省 · 杭州市 · 西湖区」形式的字符串。
 * 网络不可用 / Geocoder 不支持 / 越界时返回 null（调用方按 null 处理，不显示位置行）。
 */
@Composable
fun rememberGeocodedAddress(
    latitude: Double?,
    longitude: Double?,
    locale: Locale = Locale.SIMPLIFIED_CHINESE,
): String? {
    if (latitude == null || longitude == null) return null
    if (latitude == 0.0 && longitude == 0.0) return null
    val context = LocalContext.current
    val latKey = (latitude * 1000).toInt()
    val lonKey = (longitude * 1000).toInt()
    return produceState<String?>(initialValue = null, key1 = latKey, key2 = lonKey) {
        value = withContext(Dispatchers.IO) {
            addressCache.get(latKey to lonKey) ?: runCatching {
                Geocoder(context, locale)
                    .getFromLocation(latitude, longitude, 1)
                    ?.firstOrNull()
                    ?.let(::formatAddress)
            }.getOrNull().also { addressCache.put(latKey to lonKey, it) }
        }
    }.value
}

private fun formatAddress(address: Address): String {
    // 中 OEM ROM 对 subAdminArea / subLocality 的填充约定不同（district 可能在任一字段），
    // 先 subAdminArea，缺失则退到 subLocality。
    val district = address.subAdminArea ?: address.subLocality
    return listOfNotNull(
        address.countryName,
        address.adminArea,
        address.locality,
        district,
    )
        .filter { it.isNotBlank() }
        .joinToString(" · ")
}