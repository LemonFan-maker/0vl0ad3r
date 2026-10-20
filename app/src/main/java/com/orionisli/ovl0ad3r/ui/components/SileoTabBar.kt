package com.orionisli.ovl0ad3r.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.HapticUtil
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt

enum class SileoTab(val titleResId: Int, val iconResId: Int) {
    FEATURED(R.string.tab_featured, R.drawable.ic_tab_featured),
    SOURCES(R.string.tab_sources, R.drawable.ic_tab_sources),
    SEARCH(R.string.tab_search, R.drawable.ic_tab_search),
    INSTALLED(R.string.tab_installed, R.drawable.ic_tab_installed),
    SETTINGS(R.string.tab_settings, R.drawable.ic_settings)
}

private val CardShape = RoundedCornerShape(29.dp)
private val IndicatorShape = RoundedCornerShape(25.dp)

@Composable
fun SileoTabBar(
    selectedTab: SileoTab,
    onTabSelected: (SileoTab) -> Unit,
    dragState: PageDragState,
    dragScope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val tabs = SileoTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    val colors = OvlTheme.colors
    val isDark = colors.isDark

    val cardBg = colors.surface
    val cardBorder = colors.outlineVariant
    val indicatorBg = colors.primaryContainer
    val indicatorStroke = colors.primary.copy(alpha = if (isDark) 0.25f else 0.12f)
    val glassBg = if (isDark) colors.surface.copy(alpha = 0.88f) else colors.surface.copy(alpha = 0.92f)
    val glassStroke =
        if (isDark) colors.primary.copy(alpha = 0.71f) else colors.primary.copy(alpha = 0.55f)

    var liquidDragging by remember { mutableStateOf(false) }
    var hover by remember { mutableIntStateOf(selectedIndex) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(
                    elevation = 3.5.dp,
                    shape = CardShape,
                    spotColor = colors.primary.copy(alpha = 0.18f)
                )
                .clip(CardShape)
                .background(cardBg)
                .border(width = 1.dp, color = cardBorder, shape = CardShape)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            val tabWidthPx = constraints.maxWidth.toFloat() / tabs.size
            val tabWidthDp = with(LocalDensity.current) { tabWidthPx.toDp() }
            val bouncePx = with(LocalDensity.current) { 16.dp.toPx() }

            SideEffect {
                dragState.onWidthSettled(tabWidthPx)
                dragState.bouncePx = bouncePx
            }

            val stretch by animateFloatAsState(
                targetValue = if (liquidDragging) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
                label = "indicatorStretch"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        IntOffset((dragState.position.value * tabWidthPx).roundToInt(), 0)
                    }
                    .width(tabWidthDp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = 1f + 0.02f * stretch
                        scaleY = 1f + 0.04f * stretch
                    }
                    .then(
                        if (liquidDragging) {
                            Modifier.shadow(
                                elevation = 4.dp,
                                shape = IndicatorShape,
                                spotColor = colors.primary.copy(alpha = 0.35f),
                                ambientColor = Color.Transparent,
                                clip = false
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clip(IndicatorShape)
                    .background(
                        if (liquidDragging) {
                            Brush.verticalGradient(
                                listOf(glassBg, glassBg.copy(alpha = 0.72f))
                            )
                        } else {
                            Brush.verticalGradient(listOf(indicatorBg, indicatorBg))
                        }
                    )
                    .border(
                        width = if (liquidDragging) 1.5.dp else 0.8.dp,
                        color = if (liquidDragging) glassStroke else indicatorStroke,
                        shape = IndicatorShape
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(tabWidthPx, tabs.size) {
                        if (tabWidthPx <= 0f) return@pointerInput
                        detectTapGestures { offset ->
                            val clicked = (offset.x / tabWidthPx)
                                .toInt()
                                .coerceIn(0, tabs.size - 1)
                            HapticUtil.tick()
                            if (clicked != currentSelectedIndex) {
                                dragState.animateTo(clicked, dragScope)
                                currentOnTabSelected(tabs[clicked])
                            }
                        }
                    }
                    .pointerInput(tabWidthPx, tabs.size) {
                        if (tabWidthPx <= 0f) return@pointerInput
                        var anchorPx = 0f
                        var travelled = 0f
                        var lastHover = 0
                        detectDragGestures(
                            onDragStart = {
                                anchorPx = dragState.position.value * tabWidthPx
                                travelled = 0f
                                lastHover = dragState.hoverIndex
                                HapticUtil.heavyClick()
                                dragState.beginDrag()
                                liquidDragging = true
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                travelled += dragAmount.x
                                dragState.dragToPx(anchorPx + travelled)
                                if (dragState.hoverIndex != lastHover) {
                                    lastHover = dragState.hoverIndex
                                    hover = lastHover
                                    HapticUtil.detentTick()
                                }
                            },
                            onDragEnd = {
                                liquidDragging = false
                                HapticUtil.click()
                                val target = dragState.hoverIndex.coerceIn(0, tabs.size - 1)
                                dragState.endDrag(target, dragScope)
                                if (target != currentSelectedIndex) {
                                    currentOnTabSelected(tabs[target])
                                }
                            },
                            onDragCancel = {
                                liquidDragging = false
                                dragState.endDrag(currentSelectedIndex, dragScope)
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeIndex = if (liquidDragging) hover else selectedIndex

                tabs.forEachIndexed { index, tab ->
                    val isActive = index == activeIndex
                    val contentColor =
                        if (isActive) colors.primary else colors.textSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = tab.iconResId),
                                contentDescription = stringResource(id = tab.titleResId),
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(id = tab.titleResId),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
