package com.orionisli.ovl0ad3r.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.orionisli.ovl0ad3r.data.model.QueueAction
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageActionSheet(
    packageItem: PackageItem,
    queuedAction: QueueAction?,
    onDismissRequest: () -> Unit,
    onUpdate: () -> Unit,
    onUninstall: () -> Unit,
    onDequeue: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = OvlTheme.colors.surface,
        contentColor = OvlTheme.colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(OvlTheme.colors.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.package_actions_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = OvlTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.size(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OvlTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_package),
                        contentDescription = null,
                        tint = OvlTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = packageItem.name.ifEmpty { packageItem.id },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = OvlTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "v${packageItem.version}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OvlTheme.colors.textSecondary
                    )
                }
            }

            if (queuedAction != null) {
                Spacer(modifier = Modifier.size(14.dp))
                val queuedLabel = stringResource(
                    when (queuedAction) {
                        QueueAction.INSTALL -> R.string.action_install
                        QueueAction.UPGRADE -> R.string.action_update
                        QueueAction.REMOVE -> R.string.action_uninstall
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(OvlTheme.colors.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.package_actions_queued, queuedLabel),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OvlTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            PackageActionRow(
                iconRes = R.drawable.ic_refresh,
                title = stringResource(R.string.action_update),
                subtitle = stringResource(R.string.package_actions_update_subtitle),
                accent = OvlTheme.colors.primary,
                onClick = {
                    onUpdate()
                    onDismissRequest()
                }
            )

            Spacer(modifier = Modifier.size(6.dp))

            PackageActionRow(
                iconRes = R.drawable.ic_trash,
                title = stringResource(R.string.action_uninstall),
                subtitle = stringResource(R.string.package_actions_uninstall_subtitle),
                accent = OvlTheme.colors.error,
                titleColor = OvlTheme.colors.error,
                onClick = {
                    onUninstall()
                    onDismissRequest()
                }
            )

            if (queuedAction != null) {
                TextButton(
                    onClick = {
                        onDequeue()
                        onDismissRequest()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(R.string.package_actions_dequeue),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = OvlTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun PackageActionRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    accent: Color,
    titleColor: Color = OvlTheme.colors.textPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = titleColor
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = OvlTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = OvlTheme.colors.textTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
