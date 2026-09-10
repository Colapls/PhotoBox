package com.photobox.core.common.di

import android.content.Context
import coil3.ImageLoader
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageLoaderModuleTest {
    @Test fun `provideImageLoader configures memory and disk cache`() {
        // mockk Context — cacheDir mock 为临时路径，避免真实文件系统操作。
        val context: Context = mockk(relaxed = true)
        val tempCacheDir = File(System.getProperty("java.io.tmpdir") ?: ".", "test-cache")
        every { context.cacheDir } returns tempCacheDir

        val loader: ImageLoader = ImageLoaderModule.provideImageLoader(context)

        // 内存缓存已配置（maxSizeBytes 由 Runtime 计算，但非零即可）。
        assertNotNull(loader.memoryCache)
        assertTrue(loader.memoryCache!!.maxSizeBytes > 0L)

        // 磁盘缓存目录 + 256MB 上限。
        assertNotNull(loader.diskCache)
        assertEquals(256L * 1024 * 1024, loader.diskCache!!.maxSizeBytes)
        assertEquals(File(tempCacheDir, "coil_disk_cache"), loader.diskCache!!.directory)
    }
}
