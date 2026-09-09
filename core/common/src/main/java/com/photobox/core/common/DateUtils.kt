package com.photobox.core.common

import java.util.Calendar
import java.util.TimeZone

object DateUtils {
    /**
     * 给定时间戳，返回当天 00:00:00.000（设备本地时区）的 epoch ms。
     */
    fun startOfDay(ms: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * 给定时间戳，返回当天 23:59:59.999 的 epoch ms（endOfDay 包含全天的全部时刻）。
     */
    fun endOfDay(ms: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        return startOfDay(ms, timeZone) + 24L * 60 * 60 * 1000 - 1
    }
}
