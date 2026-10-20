package com.orionisli.ovl0ad3r.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.data.model.PackageItem
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

enum class PackageButtonState {
    GET,
    QUEUED,
    MANAGE,
    SYSTEM
}

@Composable
fun PackageCard(
    packageItem: PackageItem,
    buttonState: PackageButtonState,
    onClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isRowPressed by interactionSource.collectIsPressedAsState()
    val rowBackground by animateColorAsState(
        targetValue = if (isRowPressed) {
            OvlTheme.colors.surfaceVariant.copy(alpha = 0.35f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f),
        label = "rowBackground"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    com.orionisli.ovl0ad3r.HapticUtil.tick()
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OvlTheme.colors.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = OvlTheme.colors.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_package),
                    contentDescription = null,
                    tint = OvlTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = packageItem.name.ifEmpty { packageItem.id },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = OvlTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                val subtitleText = buildString {
                    append(packageItem.version)
                    if (packageItem.section.isNotEmpty()) {
                        append(" · ")
                        append(packageItem.section)
                    } else if (packageItem.category.isNotEmpty()) {
                        append(" · ")
                        append(packageItem.category)
                    }
                    if (packageItem.sizeBytes > 0) {
                        append(" · ")
                        append(formatByteSize(packageItem.sizeBytes))
                    }
                }

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = OvlTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            SileoPillActionButton(
                state = buttonState,
                onClick = onActionClick
            )
        }

        if (showDivider) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(OvlTheme.colors.outlineVariant.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
fun SileoPillActionButton(
    state: PackageButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (state) {
        PackageButtonState.GET -> OvlTheme.colors.primaryContainer
        PackageButtonState.QUEUED -> OvlTheme.colors.surfaceVariant
        PackageButtonState.MANAGE -> OvlTheme.colors.surfaceContainer
        PackageButtonState.SYSTEM -> OvlTheme.colors.surfaceVariant
    }

    val contentColor = when (state) {
        PackageButtonState.GET -> OvlTheme.colors.primary
        PackageButtonState.QUEUED -> OvlTheme.colors.textSecondary
        PackageButtonState.MANAGE -> OvlTheme.colors.textPrimary
        PackageButtonState.SYSTEM -> OvlTheme.colors.textTertiary
    }

    val labelText = when (state) {
        PackageButtonState.GET -> stringResource(id = R.string.action_get)
        PackageButtonState.QUEUED -> stringResource(id = R.string.action_queued)
        PackageButtonState.MANAGE -> stringResource(id = R.string.action_manage)
        PackageButtonState.SYSTEM -> stringResource(id = R.string.action_system_managed)
    }

    Box(
        modifier = modifier
            .height(30.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                enabled = state != PackageButtonState.SYSTEM,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = labelText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.3.sp
            ),
            color = contentColor
        )
    }
}

fun formatByteSize(bytes: Long): String {
    if (bytes <= 0) return "0B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.1fGB", gb)
        mb >= 1.0 -> String.format("%.1fMB", mb)
        kb >= 1.0 -> String.format("%.1fKB", kb)
        else -> "${bytes}B"
    }
}
