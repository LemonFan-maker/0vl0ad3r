package com.orionisli.ovl0ad3r.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.data.model.QueueAction
import com.orionisli.ovl0ad3r.data.model.QueueState
import com.orionisli.ovl0ad3r.data.model.QueueTask
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queueState: QueueState,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit,
    onClearQueue: () -> Unit,
    onConfirmExecute: () -> Unit,
    onRemoveTask: (QueueTask) -> Unit = {}
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.queue_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = OvlTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(id = R.string.queue_task_count, queueState.count),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OvlTheme.colors.textSecondary
                    )
                }

                if (queueState.count > 0) {
                    TextButton(
                        onClick = onClearQueue,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.action_clear_queue),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = OvlTheme.colors.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (queueState.isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.queue_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OvlTheme.colors.textTertiary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    val installs = queueState.installTasks
                    if (installs.isNotEmpty()) {
                        item {
                            QueueSectionHeader(
                                title = stringResource(id = R.string.action_install),
                                count = installs.size,
                                actionColor = OvlTheme.colors.primary
                            )
                        }
                        items(installs, key = { "install_${it.packageItem.id}" }) { task ->
                            QueueTaskRow(task = task, onRemove = { onRemoveTask(task) })
                        }
                    }

                    val upgrades = queueState.upgradeTasks
                    if (upgrades.isNotEmpty()) {
                        item {
                            QueueSectionHeader(
                                title = stringResource(id = R.string.action_update),
                                count = upgrades.size,
                                actionColor = OvlTheme.colors.warning
                            )
                        }
                        items(upgrades, key = { "upgrade_${it.packageItem.id}" }) { task ->
                            QueueTaskRow(task = task, onRemove = { onRemoveTask(task) })
                        }
                    }

                    val removes = queueState.removeTasks
                    if (removes.isNotEmpty()) {
                        item {
                            QueueSectionHeader(
                                title = stringResource(id = R.string.action_uninstall),
                                count = removes.size,
                                actionColor = OvlTheme.colors.error
                            )
                        }
                        items(removes, key = { "remove_${it.packageItem.id}" }) { task ->
                            QueueTaskRow(task = task, onRemove = { onRemoveTask(task) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OvlTheme.colors.surfaceContainer)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.queue_download_size),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OvlTheme.colors.textSecondary
                    )
                    Text(
                        text = formatByteSize(queueState.totalDownloadBytes),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = OvlTheme.colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val confirmInteraction = remember { MutableInteractionSource() }
                val isConfirmPressed by confirmInteraction.collectIsPressedAsState()
                val confirmScale by animateFloatAsState(
                    targetValue = if (isConfirmPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 180f),
                    label = "confirmScale"
                )

                Button(
                    onClick = onConfirmExecute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(confirmScale)
                        .height(48.dp),
                    interactionSource = confirmInteraction,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OvlTheme.colors.primary,
                        contentColor = OvlTheme.colors.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.action_confirm),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun QueueSectionHeader(
    title: String,
    count: Int,
    actionColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 12.dp)
                .clip(CircleShape)
                .background(actionColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$title($count)",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = actionColor
        )
    }
}

@Composable
private fun QueueTaskRow(
    task: QueueTask,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (task.isDependency) 20.dp else 4.dp,
                top = 4.dp,
                bottom = 4.dp,
                end = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (task.isDependency) {
            Text(
                text = "└─ ",
                style = MaterialTheme.typography.bodySmall,
                color = OvlTheme.colors.textTertiary
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(OvlTheme.colors.surfaceVariant)
                .border(
                    width = 0.5.dp,
                    color = OvlTheme.colors.outlineVariant,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_package),
                contentDescription = null,
                tint = OvlTheme.colors.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task.packageItem.name.ifEmpty { task.packageItem.id },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = OvlTheme.colors.textPrimary
                )
                if (task.isDependency) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.queue_dependency_tag),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = OvlTheme.colors.textSecondary
                    )
                }
            }
            Text(
                text = "${task.packageItem.version} • ${formatByteSize(task.packageItem.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = OvlTheme.colors.textSecondary
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = null,
            tint = OvlTheme.colors.textTertiary,
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = onRemove)
        )
    }
}
