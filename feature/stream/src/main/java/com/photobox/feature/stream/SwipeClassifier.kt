package com.photobox.feature.stream

import kotlin.math.abs

/**
 * 流屏幕左滑手势分类器。
 * 把"现在手指在做什么"和"够不够长成一次滑动"抽成纯函数，
 * 让 StreamScreen 的 pointerInput 只负责事件循环，判定交给这里。
 */

/** 锁定的方向：true = 水平（X），false = 垂直（Y），null = 尚未锁定。 */

/**
 * 根据当前手指到 down 的绝对位移判断方向。
 * @param dx 当前手指位置 - down.x（向右为正）
 * @param dy 当前手指位置 - down.y（向下为正）
 * @param slop 平台 touchSlop（像素）
 * @return null = 尚未跨过 slop；true = 水平主导；false = 垂直主导
 */
internal fun classifySwipeAxis(dx: Float, dy: Float, slop: Float): Boolean? =
    if (abs(dx) <= slop && abs(dy) <= slop) {
        null
    } else if (abs(dx) > abs(dy)) {
        true
    } else {
        false
    }

/**
 * 判定本次手势结束时是否为一次「左滑」（负方向且水平 > 垂直 2 倍）。
 * @param thresholdMul 离开 slop 的倍数（默认 4，对应原 80dp @ 20dp slop）
 */
internal fun isLeftSwipe(
    totalDx: Float,
    totalDy: Float,
    slop: Float,
    thresholdMul: Float = 4f,
): Boolean {
    if (totalDx >= 0f) return false
    if (totalDx > -slop * thresholdMul) return false
    return abs(totalDx) > 2 * abs(totalDy)
}