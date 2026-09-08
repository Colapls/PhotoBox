package com.photobox.core.data.db.converter

import androidx.room.TypeConverter

/**
 * 将 List<Long> 与逗号分隔字符串互转。YAGNI: 不引 kotlinx.serialization。
 * 空列表编码为 ""，单元素 [42] 编码为 "42"，多元素按 "," 连接。
 */
class LongListConverter {
    @TypeConverter
    fun fromList(list: List<Long>?): String =
        list?.joinToString(separator = ",").orEmpty()

    @TypeConverter
    fun toList(value: String?): List<Long> =
        if (value.isNullOrBlank()) emptyList()
        else value.split(",").mapNotNull { it.trim().toLongOrNull() }
}