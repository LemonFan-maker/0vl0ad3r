package com.orionisli.ovl0ad3r.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ByteTally 的不变量: 回调序列单调不减, 末值等于各源字节之和。
 *
 * 这条不变量只在**多个源同时成功**时才会被破坏, 而线上 Release 兜底源在首次打
 * tag 之前必然 404, 所以真站点冒烟永远只覆盖到单源成功那一路。真实链路又强制
 * HTTPS, 不适合在单测里起 TLS 服务, 故直接驱动 syncAll 实际使用的这个汇总器。
 */
class ByteTallyTest {

    private fun runTally(sources: List<List<Long>>): List<Long> {
        val emitted = mutableListOf<Long>()
        val tally = ByteTally { synchronized(emitted) { emitted += it } }
        // 每个源一个回调, 按给定序列上报"该源自己的累计值"。
        val perSource = sources.map { tally.forSource() }
        // 交错推进, 模拟并发下两源的进度交替到达。
        val depth = sources.maxOf { it.size }
        for (i in 0 until depth) {
            perSource.forEachIndexed { idx, cb ->
                sources[idx].getOrNull(i)?.let(cb)
            }
        }
        return emitted
    }

    @Test
    fun `双源交错的末值是两源之和而不是最后结束那个源的量`() {
        // 源 A 共 10000 字节, 源 B 共 300 字节, B 后结束。
        val emitted = runTally(
            listOf(
                listOf(4000, 8000, 10000),
                listOf(150, 300)
            )
        )
        assertEquals(10300L, emitted.last())
    }

    @Test
    fun `回调序列单调不减`() {
        val emitted = runTally(
            listOf(
                listOf(4000, 8000, 10000),
                listOf(150, 300)
            )
        )
        val regressions = emitted.zipWithNext().count { (prev, next) -> next < prev }
        assertEquals("字节数出现回退: $emitted", 0, regressions)
        assertTrue(emitted.isNotEmpty())
    }

    @Test
    fun `单源成功时末值就是该源字节数`() {
        assertEquals(10886L, runTally(listOf(listOf(4096, 10886))).last())
    }

    @Test
    fun `同一累计值重复上报不重复计数`() {
        // 两个源都停在同一累计值上: 总量应各自计一次, 不因为重复回调而膨胀。
        val emitted = runTally(
            listOf(
                listOf(500, 500, 500),
                listOf(500)
            )
        )
        assertEquals(1000L, emitted.last())
    }

    @Test
    fun `失败源的已落字节仍计入总量`() {
        // 源 B 读到 200 字节后连接断开, 不再上报; 那 200 字节确实下载过。
        val emitted = runTally(
            listOf(
                listOf(900),
                listOf(200)
            )
        )
        assertEquals(1100L, emitted.last())
    }
}
