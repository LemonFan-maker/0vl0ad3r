package com.orionisli.ovl0ad3r.data

import com.orionisli.ovl0ad3r.ui.components.PageDragState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 底栏与内容的共用位置真源验证。
 *
 * 吸附动画需要真实帧时钟，这里注入一个「立刻取消」的空作用域，
 * 只验证拖拽期的像素换算、橡皮筋阻尼、落点判定与实体化窗口这些纯逻辑。
 */
class PageDragStateTest {

    private val pageCount = 5
    private val lastIndex = pageCount - 1
    private val width = 200f

    private fun state(initialIndex: Int = 0): PageDragState =
        PageDragState(lastIndex, initialIndex).apply { onWidthSettled(width) }

    private fun PageDragState.positionPx(): Float = position.value * width

    @Test
    fun initialPositionFollowsInitialIndex() {
        assertEquals(0f, state(0).positionPx(), TOLERANCE)
        assertEquals(lastIndex * width, state(lastIndex).positionPx(), TOLERANCE)
    }

    @Test
    fun widthSettledKeepsPositionAndWindowOnSelectedIndex() {
        // 位置以标签下标为单位，与像素宽度无关：宽度未实测时窗口就已落在选中页上
        val s = PageDragState(lastIndex, 3)
        assertEquals(3f, s.position.value, TOLERANCE)
        assertEquals(2, s.windowLow)
        assertEquals(4, s.windowHigh)

        s.onWidthSettled(width)
        assertEquals(3f, s.position.value, TOLERANCE)
        assertEquals(3 * width, s.positionPx(), TOLERANCE)
    }

    @Test
    fun windowStaysOnePageEitherSide() {
        val s = state(initialIndex = 2)
        assertEquals(1, s.windowLow)
        assertEquals(3, s.windowHigh)

        s.beginDrag()
        s.dragToPx(0f)
        assertEquals(0, s.windowLow)
        assertEquals(1, s.windowHigh)

        s.dragToPx(lastIndex * width)
        assertEquals(lastIndex - 1, s.windowLow)
        assertEquals(lastIndex, s.windowHigh)
    }

    @Test
    fun dragMovesPositionOneToOne() {
        val s = state(initialIndex = 0)
        s.beginDrag()
        s.dragToPx(150f)
        assertEquals(150f, s.positionPx(), TOLERANCE)
        s.dragToPx(420f)
        assertEquals(420f, s.positionPx(), TOLERANCE)
    }

    @Test
    fun dragBeyondEndsIsDamped() {
        val s = state(initialIndex = 0).apply { bouncePx = 16f }
        s.beginDrag()

        // 左端超出 100px，按 0.35 阻尼后为 -35px，但被橡皮筋余量截断
        s.dragToPx(-100f)
        assertEquals(-16f, s.positionPx(), TOLERANCE)

        // 右端超出 100px，阻尼后末页 + 35px，同样被余量截断
        s.dragToPx(lastIndex * width + 100f)
        assertEquals(lastIndex * width + 16f, s.positionPx(), TOLERANCE)
    }

    @Test
    fun dragWithoutBeginIsIgnored() {
        val s = state(initialIndex = 1)
        s.dragToPx(999f)
        assertEquals(width, s.positionPx(), TOLERANCE)
    }

    @Test
    fun hoverIndexRoundsToNearestTab() {
        val s = state(initialIndex = 0)
        s.beginDrag()

        s.dragToPx(99f)
        assertEquals(0, s.hoverIndex)

        s.dragToPx(101f)
        assertEquals(1, s.hoverIndex)

        // (690 + 100) / 200 = 3.95，向下取整落在第 4 项
        s.dragToPx(690f)
        assertEquals(3, s.hoverIndex)

        s.dragToPx(710f)
        assertEquals(4, s.hoverIndex)
    }

    @Test
    fun hoverIndexStaysInRangeWhileBouncing() {
        val s = state(initialIndex = 0).apply { bouncePx = 16f }
        s.beginDrag()

        s.dragToPx(-16f)
        assertEquals(0, s.hoverIndex)

        s.dragToPx(lastIndex * width + 16f)
        assertEquals(lastIndex, s.hoverIndex)
    }

    @Test
    fun endDragLeavesDraggingState() {
        val s = state(initialIndex = 0)
        s.beginDrag()
        assertTrue(s.isDragging)

        s.dragToPx(390f)
        s.endDrag(2, EmptyScope)
        assertFalse(s.isDragging)
        // 空作用域下动画不推进，位置停留在拖拽终点
        assertEquals(390f, s.positionPx(), TOLERANCE)
    }

    @Test
    fun jumpToAlignsPositionImmediately() {
        val s = state(initialIndex = 0)
        s.jumpTo(4)
        assertEquals(lastIndex * width, s.positionPx(), TOLERANCE)
        assertEquals(3, s.windowLow)
        assertEquals(lastIndex, s.windowHigh)
    }

    private object EmptyScope : CoroutineScope {
        override val coroutineContext = Job().also { it.cancel() }
    }

    private companion object {
        const val TOLERANCE = 1e-4f
    }
}
