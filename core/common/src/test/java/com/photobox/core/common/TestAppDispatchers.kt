package com.photobox.core.common

import kotlinx.coroutines.CoroutineDispatcher

/**
 * 测试用 [AppDispatchers] 实现，允许每个调度器独立注入。
 * 生产代码注入 [DefaultAppDispatchers]，测试代码注入 [TestAppDispatchers]。
 */
data class TestAppDispatchers(
    override val main: CoroutineDispatcher,
    override val io: CoroutineDispatcher,
    override val default: CoroutineDispatcher,
) : AppDispatchers
