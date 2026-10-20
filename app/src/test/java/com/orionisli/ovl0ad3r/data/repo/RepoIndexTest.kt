package com.orionisli.ovl0ad3r.data.repo

import com.orionisli.ovl0ad3r.data.engine.DependencyResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 索引解析契约测试。
 *
 * fixture 是从 v0.1.0 Release (RepoEndpoints.GITHUB_RELEASES) 原样抓下来的真实
 * 产物, 不是手写的理想样例 —— 上游生成脚本改字段时, 这里应当先炸。
 */
class RepoIndexTest {

    private fun fixture(): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("repo/index.json")) {
            "缺少测试夹具 repo/index.json"
        }.bufferedReader().use { it.readText() }

    @Test
    fun `解析线上真实索引`() {
        val index = RepoIndex.parse(fixture())
        assertEquals(RepoIndex.SUPPORTED_SCHEMA, index.schema)
        assertEquals("0vl0ad3r.db.tar.zst", index.dbFilename)
        assertEquals(18, index.packages.size)
        assertEquals(index.packages.size, index.packages.map { it.name }.distinct().size)
    }

    @Test
    fun `包字段按上游键名落地`() {
        val aria2 = RepoIndex.parse(fixture()).packages.first { it.name == "aria2" }
        assertEquals("1.37.0-1", aria2.version)
        assertEquals("aria2-1.37.0-1-aarch64.pkg.tar.zst", aria2.filename)
        assertEquals(714450L, aria2.sizeBytes)
        assertEquals("aarch64", aria2.architecture)
        // sha256 是后续校验包体唯一的锚点, 必须是完整 64 位十六进制。
        assertTrue(aria2.sha256.matches(Regex("^[0-9a-f]{64}$")))
        // 依赖是包名列表, 不是 pacman 那种带版本约束的串。
        assertTrue(aria2.depends.containsAll(listOf("base-layout", "ca-certificates", "zlib")))
        assertEquals("https://aria2.github.io/", aria2.homepage)
    }

    @Test
    fun `packager 只取显示名不带邮箱`() {
        val pkg = RepoIndex.parse(fixture()).packages.first()
        // 上游原样给的是 `名字 <邮箱>`; publisher 负责把邮箱剥掉再上界面。
        assertTrue(pkg.packager.contains("<"))
        assertFalse(pkg.publisher.contains("<"))
        assertEquals(pkg.packager.substringBefore('<').trim(), pkg.publisher)
    }

    @Test
    fun `转成界面模型时安装态由调用方决定`() {
        val pkg = RepoIndex.parse(fixture()).packages.first { it.name == "nano" }
        val item = pkg.toPackageItem(isInstalled = true, hasUpdate = false)
        assertEquals("nano-9.2-1", item.id)
        // pacman 目标必须是不带版本号的包名。
        assertEquals("nano", item.packageName)
        assertEquals(pkg.description, item.description)
        assertEquals(pkg.sizeBytes, item.sizeBytes)
        // 下载与详情页要用的字段必须一路透传, 不能在中途丢。
        assertEquals(pkg.filename, item.filename)
        assertEquals(pkg.sha256, item.sha256)
        assertEquals(pkg.homepage, item.homepage)
        assertEquals(pkg.license, item.license)
        assertEquals(pkg.depends, item.dependencies)
        assertTrue(item.isInstalled)
        assertFalse(item.hasUpdate)
    }

    @Test
    fun `拒绝不认识的主版本`() {
        val body = fixture().replace(Regex("\"schema\"\\s*:\\s*1"), "\"schema\": 2")
        try {
            RepoIndex.parse(body)
            fail("schema 不匹配时应当抛错, 而不是按旧字段猜着解析")
        } catch (e: RepoParseException) {
            assertTrue(e.message!!.contains("schema"))
        }
    }

    @Test
    fun `拒绝count与实际条目不符的半截索引`() {
        val body = fixture().replace(Regex("\"count\"\\s*:\\s*18"), "\"count\": 99")
        try {
            RepoIndex.parse(body)
            fail("count 对不上说明上游生成脚本有 bug, 不能展示")
        } catch (e: RepoParseException) {
            assertTrue(e.message!!.contains("count"))
        }
    }

    @Test
    fun `非JSON响应给出可读错误而不是原始异常`() {
        try {
            RepoIndex.parse("<html>502 Bad Gateway</html>")
            fail("应当抛 RepoParseException")
        } catch (e: RepoParseException) {
            assertTrue(e.message!!.startsWith("索引不是合法 JSON"))
        }
    }

    @Test
    fun `丢弃无法寻址的条目而不是整表失败`() {
        val body = """
            {"schema":1,"db":"0vl0ad3r.db.tar.zst","count":1,"packages":[
              {"name":"","version":"1-1","filename":"x.pkg.tar.zst"},
              {"name":"ok","version":"1-1","filename":"","sha256":"ab"},
              {"name":"good","version":"2-1","filename":"good-2-1-aarch64.pkg.tar.zst","size":10}
            ]}
        """.trimIndent()
        val index = RepoIndex.parse(body)
        assertEquals(listOf("good"), index.packages.map { it.name })
    }

    @Test
    fun `索引地址在源基址后拼接`() {
        assertEquals(
            "https://r3p0.orionisli.com/index.json",
            RepoRepository.indexUrlOf(RepoEndpoints.PRIMARY)
        )
        // 用户在添加源对话框里多半会带上尾斜杠, 拼出 // 会被某些 CDN 拒绝。
        assertEquals(
            "https://r3p0.orionisli.com/index.json",
            RepoRepository.indexUrlOf("https://r3p0.orionisli.com/")
        )
    }

    @Test
    fun `真实索引的依赖闭包完整且顺序正确`() {
        val index = RepoIndex.parse(fixture())
        val plan = DependencyResolver.resolve(
            targets = index.packages.map { it.name },
            catalog = index.packages,
            installed = emptySet()
        )
        // 安装时任何缺口都会让 pacman -U 中止整个事务; 线上索引必须自洽。
        assertTrue(plan.missing.isEmpty())
        assertEquals(index.packages.size, plan.ordered.size)

        val order = plan.ordered.map { it.name }
        assertTrue(order.indexOf("base-layout") < order.indexOf("nano"))
        assertTrue(order.indexOf("openssl") < order.indexOf("curl"))
        assertTrue(order.indexOf("libcurl") < order.indexOf("curl"))
    }
}
