package com.orionisli.ovl0ad3r.data.engine

import com.orionisli.ovl0ad3r.data.repo.RepoPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 依赖排序契约。
 *
 * 站点索引里的 depends 是平的包名列表, 安装顺序只能由客户端算; 排错会让
 * `pacman -U` 在缺依赖时中止整个事务, 所以核心用例放在离线单测里钉死。
 */
class DependencyResolverTest {

    private fun pkg(name: String, vararg deps: String) = RepoPackage(
        name = name,
        version = "1.0-1",
        description = "",
        filename = "$name-1.0-1-aarch64.pkg.tar.zst",
        sizeBytes = 1L,
        installedSizeBytes = 1L,
        sha256 = "0".repeat(64),
        homepage = "",
        license = "",
        architecture = "aarch64",
        buildDateEpochSec = 0L,
        packager = "0vl0ad3r Packager <packages@0vl0ad3r.org>",
        depends = deps.toList()
    )

    @Test
    fun `依赖排在目标之前`() {
        val catalog = listOf(pkg("a", "b", "c"), pkg("b", "c"), pkg("c"))
        val plan = DependencyResolver.resolve(listOf("a"), catalog, emptySet())
        assertEquals(listOf("c", "b", "a"), plan.ordered.map { it.name })
        assertEquals(emptyList<String>(), plan.missing)
    }

    @Test
    fun `已安装的依赖不重复下载`() {
        val catalog = listOf(pkg("a", "b"), pkg("b", "c"), pkg("c"))
        val plan = DependencyResolver.resolve(listOf("a"), catalog, setOf("b", "c"))
        assertEquals(listOf("a"), plan.ordered.map { it.name })
    }

    @Test
    fun `显式目标即使已安装也保留`() {
        val catalog = listOf(pkg("a"))
        val plan = DependencyResolver.resolve(listOf("a"), catalog, setOf("a"))
        assertEquals(listOf("a"), plan.ordered.map { it.name })
    }

    @Test
    fun `索引里缺失且未安装的依赖上报而不是静默跳过`() {
        val catalog = listOf(pkg("a", "ghost"))
        val plan = DependencyResolver.resolve(listOf("a"), catalog, emptySet())
        assertEquals(listOf("ghost"), plan.missing)
        assertEquals(listOf("a"), plan.ordered.map { it.name })
    }

    @Test
    fun `已安装的缺失依赖算满足不报错`() {
        val catalog = listOf(pkg("a", "ghost"))
        val plan = DependencyResolver.resolve(listOf("a"), catalog, setOf("ghost"))
        assertTrue(plan.missing.isEmpty())
    }

    @Test
    fun `环依赖不会死循环`() {
        val catalog = listOf(pkg("a", "b"), pkg("b", "a"))
        val plan = DependencyResolver.resolve(listOf("a"), catalog, emptySet())
        assertEquals(2, plan.ordered.size)
        assertTrue(plan.ordered.map { it.name }.containsAll(listOf("a", "b")))
    }

    @Test
    fun `多个目标共享依赖只排一次`() {
        val catalog = listOf(pkg("a", "c"), pkg("b", "c"), pkg("c"))
        val plan = DependencyResolver.resolve(listOf("a", "b"), catalog, emptySet())
        assertEquals(listOf("c", "a", "b"), plan.ordered.map { it.name })
    }
}
