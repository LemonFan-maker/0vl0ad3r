package com.orionisli.ovl0ad3r.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 索引离线快照契约。
 *
 * 它决定用户重启后还能不能看到上次同步的包目录; 解析失败必须安静地退回
 * 空态, 不能把启动流程炸掉。
 */
class RepoIndexCacheTest {

    private fun tempDir(): File =
        Files.createTempDirectory("0vl0ad3r-index-cache").toFile()

    private fun fixture(): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("repo/index.json")) {
            "缺少测试夹具 repo/index.json"
        }.bufferedReader().use { it.readText() }

    @Test
    fun `写入后重启可恢复包目录与源地址`() {
        val dir = tempDir()
        RepoIndexCache.write(dir, fixture(), "https://r3p0.orionisli.com")

        val cached = RepoIndexCache.read(dir)
        checkNotNull(cached)
        assertEquals(18, cached.index.packages.size)
        assertEquals("https://r3p0.orionisli.com", cached.sourceUrl)
        assertTrue(cached.index.packages.any { it.name == "nano" })
    }

    @Test
    fun `没有源地址文件时仍恢复包目录`() {
        val dir = tempDir()
        File(dir, RepoIndexCache.INDEX_FILE).writeText(fixture())

        val cached = RepoIndexCache.read(dir)
        checkNotNull(cached)
        assertEquals(18, cached.index.packages.size)
        assertNull(cached.sourceUrl)
    }

    @Test
    fun `损坏的缓存返回空而不是抛异常`() {
        val dir = tempDir()
        File(dir, RepoIndexCache.INDEX_FILE).writeText("{not json")
        assertNull(RepoIndexCache.read(dir))
    }

    @Test
    fun `缺失缓存返回空`() {
        assertNull(RepoIndexCache.read(tempDir()))
    }

    @Test
    fun `非https源地址不参与下载恢复`() {
        val dir = tempDir()
        RepoIndexCache.write(dir, fixture(), "http://insecure.example")

        val cached = RepoIndexCache.read(dir)
        checkNotNull(cached)
        assertNull(cached.sourceUrl)
    }
}
