package com.orionisli.ovl0ad3r.data.engine

import com.orionisli.ovl0ad3r.data.repo.RepoPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `pacman -Q` 输出解析契约。
 *
 * 生产侧只有一份通用解析器, 不按包名分支; 下面的测试只是给同一个解析器喂
 * 不同的 `pacman -Q` 样本, 覆盖升级标记、未收录包与告警行。
 */
class InstalledPackagesReaderTest {

    private companion object {
        const val NANO = "nano"
        const val NANO_VERSION = "9.2-1"
        const val NANO_LINE = "$NANO $NANO_VERSION"

        /** pacman 把 stderr 合并进 stdout 时可能混入的非包行。 */
        val PACMAN_DIAGNOSTICS = listOf(
            "warning: database is up to date",
            "error: failed to init transaction",
            "警告：数据库文件 core 不存在",
            "错误：无法初始化事务"
        )
    }

    private fun parseNames(output: String) =
        InstalledPackagesReader.parse(output, emptyList()).map { it.name }

    private fun withDiagnostics(vararg packageLines: String) =
        (PACMAN_DIAGNOSTICS + packageLines).joinToString("\n")

    private fun repo(name: String, version: String, vararg deps: String) = RepoPackage(
        name = name,
        version = version,
        description = "$name package",
        filename = "$name-$version-aarch64.pkg.tar.zst",
        sizeBytes = 1024L,
        installedSizeBytes = 2048L,
        sha256 = "a".repeat(64),
        homepage = "https://example.com/$name",
        license = "GPL-3.0",
        architecture = "aarch64",
        buildDateEpochSec = 0L,
        packager = "0vl0ad3r Packager <packages@0vl0ad3r.org>",
        depends = deps.toList()
    )

    @Test
    fun `本地版本与索引一致时无更新`() {
        val items = InstalledPackagesReader.parse("$NANO_LINE\n", listOf(repo(NANO, NANO_VERSION)))
        val nano = items.single()
        assertEquals("$NANO-$NANO_VERSION", nano.id)
        assertEquals(NANO, nano.packageName)
        assertEquals(NANO_VERSION, nano.version)
        assertTrue(nano.isInstalled)
        assertFalse(nano.hasUpdate)
    }

    @Test
    fun `本地落后于索引时标记可升级且显示本地版本`() {
        val items = InstalledPackagesReader.parse("$NANO_LINE\n", listOf(repo(NANO, "10.0-1")))
        val nano = items.single()
        assertEquals("$NANO-$NANO_VERSION", nano.id)
        assertEquals(NANO_VERSION, nano.version)
        assertTrue(nano.hasUpdate)
    }

    @Test
    fun `系统底座包即使索引版本不同也不提示升级`() {
        val items = InstalledPackagesReader.parse(
            "libalpm 7.1.0-1\n",
            listOf(repo("libalpm", "7.2.0-1"))
        )
        val libalpm = items.single()
        assertTrue(libalpm.isSystemPackage)
        assertFalse(libalpm.hasUpdate)
    }

    @Test
    fun `索引未收录的系统包仍标记为系统包`() {
        val items = InstalledPackagesReader.parse("base-layout 1.0.0-2\n", emptyList())
        assertTrue(items.single().isSystemPackage)
    }

    @Test
    fun `索引未收录的包如实显示名称与版本`() {
        val items = InstalledPackagesReader.parse("base-layout 1.0.0-1\n", emptyList())
        val layout = items.single()
        assertEquals("base-layout", layout.packageName)
        assertEquals("1.0.0-1", layout.version)
        assertTrue(layout.isInstalled)
        assertEquals("", layout.description)
    }

    @Test
    fun `索引里的描述与依赖会补齐到已安装模型`() {
        val items = InstalledPackagesReader.parse(
            "$NANO_LINE\n",
            listOf(repo(NANO, NANO_VERSION, "base-layout", "glibc-bionic"))
        )
        val nano = items.single()
        assertEquals("$NANO package", nano.description)
        assertEquals(listOf("base-layout", "glibc-bionic"), nano.dependencies)
    }

    @Test
    fun `告警与错误行不会被当成包名`() {
        assertEquals(listOf(NANO), parseNames(withDiagnostics(NANO_LINE)))
    }

    @Test
    fun `输出按包名排序`() {
        val out = "zlib 1.3-1\naria2 1.37.0-1\n$NANO_LINE\n"
        assertEquals(listOf("aria2", NANO, "zlib"), parseNames(out))
    }

    @Test
    fun `本地库目录名可还原带连字符的包名`() {
        val out = """
            ALPM_DB_VERSION
            $NANO-$NANO_VERSION
            libc++-21.0.0-1
            ca-certificates-2024.09.24-1
            base-layout-1.0.0-1
        """.trimIndent()
        val items = InstalledPackagesReader.parseLocalDbEntries(
            out,
            listOf(repo(NANO, NANO_VERSION), repo("base-layout", "1.0.0-1"))
        )
        assertEquals(
            listOf("base-layout", "ca-certificates", "libc++", NANO),
            items.map { it.packageName }
        )
        assertEquals("2024.09.24-1", items.first { it.packageName == "ca-certificates" }.version)
        assertEquals("21.0.0-1", items.first { it.packageName == "libc++" }.version)
        assertTrue(items.first { it.packageName == NANO }.isInstalled)
    }

    @Test
    fun `本地库兜底也能用索引补齐描述`() {
        val items = InstalledPackagesReader.parseLocalDbEntries(
            "$NANO-$NANO_VERSION\n",
            listOf(repo(NANO, NANO_VERSION, "base-layout"))
        )
        val nano = items.single()
        assertEquals("$NANO package", nano.description)
        assertEquals(listOf("base-layout"), nano.dependencies)
        assertFalse(nano.hasUpdate)
    }
}
