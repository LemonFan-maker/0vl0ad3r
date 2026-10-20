package com.orionisli.ovl0ad3r.ui.components

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

@Stable
class PageDragState(
    private val lastIndex: Int,
    initialIndex: Int
) {

    private val positionUnits = mutableFloatStateOf(initialIndex.toFloat().coerceIn(0f, lastIndex.toFloat()))

    val position: State<Float> get() = positionUnits

    private var settleJob: Job? = null
    private var dragging by mutableStateOf(false)

    private var selectedIndex: Int = initialIndex.coerceIn(0, lastIndex)

    var windowLow by mutableIntStateOf(0)
        private set

    var windowHigh by mutableIntStateOf(0)
        private set

    var tabWidthPx by mutableFloatStateOf(0f)

    var bouncePx: Float = 0f

    val isDragging: Boolean get() = dragging

    val settleSpec: SpringSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 140f)

    val hoverIndex: Int
        get() = (positionUnits.floatValue + 0.5f).toInt().coerceIn(0, lastIndex)

    init {
        writePosition(positionUnits.floatValue)
    }

    private fun writePosition(units: Float) {
        positionUnits.floatValue = units
        val lo = (floor(units).toInt() - 1).coerceIn(0, lastIndex)
        val hi = (ceil(units).toInt() + 1).coerceIn(0, lastIndex)
        if (lo != windowLow) windowLow = lo
        if (hi != windowHigh) windowHigh = hi
    }

    fun onWidthSettled(width: Float) {
        tabWidthPx = width
        if (!dragging && settleJob?.isActive != true) {
            writePosition(selectedIndex.toFloat())
        }
    }

    fun beginDrag() {
        if (dragging) return
        settleJob?.cancel()
        settleJob = null
        dragging = true
    }

    fun dragToPx(rawPx: Float) {
        if (!dragging) return
        val w = tabWidthPx
        if (w <= 0f) return
        val maxPx = lastIndex * w
        val rubber = when {
            rawPx < 0f -> rawPx * 0.35f
            rawPx > maxPx -> maxPx + (rawPx - maxPx) * 0.35f
            else -> rawPx
        }
        val clamped = rubber.coerceIn(-bouncePx, maxPx + bouncePx)
        writePosition(clamped / w)
    }

    fun endDrag(index: Int, scope: CoroutineScope) {
        if (dragging) dragging = false
        settleTo(index, scope)
    }

    fun animateTo(index: Int, scope: CoroutineScope) {
        if (dragging) {
            endDrag(index, scope)
            return
        }
        settleTo(index, scope)
    }

    private fun settleTo(index: Int, scope: CoroutineScope) {
        val target = index.coerceIn(0, lastIndex)
        selectedIndex = target
        val from = positionUnits.floatValue
        val to = target.toFloat()
        settleJob?.cancel()
        if (abs(from - to) < 0.001f) {
            writePosition(to)
            settleJob = null
            return
        }
        settleJob = scope.launch {
            animate(initialValue = from, targetValue = to, animationSpec = settleSpec) { value, _ ->
                writePosition(value)
            }
        }
    }

    fun jumpTo(index: Int) {
        settleJob?.cancel()
        settleJob = null
        dragging = false
        selectedIndex = index.coerceIn(0, lastIndex)
        writePosition(selectedIndex.toFloat())
    }

    val isSettling: Boolean get() = settleJob?.isActive == true
}
