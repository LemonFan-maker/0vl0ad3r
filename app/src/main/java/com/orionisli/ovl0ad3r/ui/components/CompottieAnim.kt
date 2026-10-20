package com.orionisli.ovl0ad3r.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

@Composable
fun PullToRefreshRing(
    progress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = OvlTheme.colors.primary,
    size: Dp = 32.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refreshRingSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 3.dp.toPx()
        val diameter = size.toPx() - strokeWidth
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(diameter, diameter)

        if (isRefreshing) {
            drawArc(
                color = color.copy(alpha = 0.25f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = spinAngle,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        } else {
            val sweep = (progress.coerceIn(0f, 1f) * 300f)
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun AnimatedSuccessCheckmark(
    modifier: Modifier = Modifier,
    color: Color = OvlTheme.colors.success,
    size: Dp = 64.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        CanvasSuccessCheckmark(color = color, size = size)
    }
}

@Composable
fun CanvasSuccessCheckmark(
    color: Color = OvlTheme.colors.success,
    size: Dp = 64.dp
) {
    var animStarted by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        animStarted = 1f
    }

    val circleProgress by animateFloatAsState(
        targetValue = animStarted,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "circleProgress"
    )

    val checkProgress by animateFloatAsState(
        targetValue = if (circleProgress > 0.6f) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 140f),
        label = "checkProgress"
    )

    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = (size.toPx() * 0.08f).coerceAtLeast(4f)
        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
        val radius = (size.toPx() - strokeWidth) / 2f

        drawCircle(
            color = color.copy(alpha = 0.15f * circleProgress),
            radius = radius,
            center = center
        )

        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * circleProgress,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        if (checkProgress > 0f) {
            val checkPath = Path().apply {
                val startX = center.x - radius * 0.45f
                val startY = center.y
                val midX = center.x - radius * 0.1f
                val midY = center.y + radius * 0.35f
                val endX = center.x + radius * 0.48f
                val endY = center.y - radius * 0.35f

                moveTo(startX, startY)
                lineTo(midX, midY)
                lineTo(endX, endY)
            }

            val measure = PathMeasure()
            measure.setPath(checkPath, false)
            val segmentPath = Path()
            measure.getSegment(0f, measure.length * checkProgress, segmentPath, true)

            drawPath(
                path = segmentPath,
                color = color,
                style = Stroke(
                    width = strokeWidth * 1.1f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun PulseIndicator(
    color: Color = OvlTheme.colors.primary,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
        val radius = (size.toPx() / 2f)
        drawCircle(
            color = color.copy(alpha = alpha * 0.35f),
            radius = radius * scale,
            center = center
        )
        drawCircle(
            color = color,
            radius = radius * 0.7f,
            center = center
        )
    }
}
