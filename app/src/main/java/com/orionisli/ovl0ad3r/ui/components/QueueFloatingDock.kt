package com.orionisli.ovl0ad3r.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.data.model.QueueState
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

@Composable
fun QueueFloatingDock(
    queueState: QueueState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = queueState.count > 0
    val haptic = LocalHapticFeedback.current

    val springSpec = spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = 0.82f,
        stiffness = 140f
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = springSpec,
            initialOffsetY = { it * 2 }
        ) + fadeIn(spring(dampingRatio = 0.82f, stiffness = 140f)),
        exit = slideOutVertically(
            animationSpec = springSpec,
            targetOffsetY = { it * 2 }
        ) + fadeOut(spring(dampingRatio = 0.82f, stiffness = 140f)),
        modifier = modifier
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val dockScale by animateFloatAsState(
            targetValue = if (isPressed) 0.96f else 1f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 180f),
            label = "dockScale"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val dockBrush = Brush.horizontalGradient(
                colors = listOf(
                    OvlTheme.colors.queueDockBackground,
                    OvlTheme.colors.surfaceContainer
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(dockScale)
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = OvlTheme.colors.primary.copy(alpha = 0.25f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(dockBrush)
                    .border(
                        width = 1.dp,
                        color = OvlTheme.colors.queueDockBorder,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        com.orionisli.ovl0ad3r.HapticUtil.click()
                        onClick()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(OvlTheme.colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_queue),
                            contentDescription = null,
                            tint = OvlTheme.colors.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = stringResource(id = R.string.queue_task_count, queueState.count),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = OvlTheme.colors.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )

                    if (queueState.totalDownloadBytes > 0) {
                        Text(
                            text = " (${formatByteSize(queueState.totalDownloadBytes)})",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = OvlTheme.colors.textSecondary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (queueState.isExecuting) {
                        PulseIndicator(
                            color = OvlTheme.colors.primary,
                            size = 8.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.queue_executing),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = OvlTheme.colors.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(OvlTheme.colors.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.action_confirm),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = OvlTheme.colors.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
