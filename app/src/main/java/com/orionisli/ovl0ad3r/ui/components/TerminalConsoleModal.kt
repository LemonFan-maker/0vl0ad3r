package com.orionisli.ovl0ad3r.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.data.engine.AnsiParser
import com.orionisli.ovl0ad3r.data.engine.ExecutionState
import com.orionisli.ovl0ad3r.data.engine.ExecutionStatus
import com.orionisli.ovl0ad3r.data.model.ReloadLevel
import com.orionisli.ovl0ad3r.ui.theme.MapleMonoFamily
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

@Composable
fun TerminalConsoleModal(
    executionState: ExecutionState,
    reloadLevel: ReloadLevel = ReloadLevel.NONE,
    onDismiss: () -> Unit,
    onRestartZygote: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    BackHandler(enabled = executionState.status != ExecutionStatus.RUNNING) {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OvlTheme.colors.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume touches to protect background */ }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TerminalHeader(
                executionState = executionState,
                onClose = onDismiss
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (executionState is ExecutionState.Running) {
                TerminalProgressBar(
                    progress = executionState.progress,
                    action = executionState.currentAction
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val logLines = when (executionState) {
                is ExecutionState.Running -> executionState.lines
                is ExecutionState.Completed -> executionState.lines
                is ExecutionState.Failed -> executionState.lines
                ExecutionState.Idle -> emptyList()
            }

            val currentTerminalPalette = OvlTheme.terminal

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(currentTerminalPalette.background)
                    .border(1.dp, currentTerminalPalette.border, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                val listState = rememberLazyListState()

                LaunchedEffect(logLines.size) {
                    if (logLines.isNotEmpty()) {
                        listState.animateScrollToItem(logLines.lastIndex)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(logLines) { index, line ->
                        val parsed = remember(line, currentTerminalPalette) {
                            AnsiParser.parse(line, currentTerminalPalette)
                        }
                        Text(
                            text = parsed,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = MapleMonoFamily,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp
                            ),
                            color = currentTerminalPalette.foreground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = executionState.status == ExecutionStatus.COMPLETED,
                    enter = fadeIn(spring(dampingRatio = 0.82f, stiffness = 140f)),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedSuccessCheckmark(
                            color = OvlTheme.colors.success,
                            size = 28.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.status_success),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = OvlTheme.colors.success
                        )
                    }
                }

                AnimatedVisibility(
                    visible = executionState.status == ExecutionStatus.FAILED,
                    enter = fadeIn(spring(dampingRatio = 0.82f, stiffness = 140f)),
                    exit = fadeOut()
                ) {
                    val errMsg = (executionState as? ExecutionState.Failed)?.errorMessage.orEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning),
                            contentDescription = null,
                            tint = OvlTheme.colors.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (errMsg.isNotEmpty()) errMsg else stringResource(id = R.string.status_failure),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = OvlTheme.colors.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TerminalBottomBar(
                    executionState = executionState,
                    reloadLevel = reloadLevel,
                    onDone = onDismiss,
                    onRestartZygote = onRestartZygote,
                    onCancel = onCancel
                )
            }
        }
    }

@Composable
private fun TerminalHeader(
    executionState: ExecutionState,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(OvlTheme.colors.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_terminal),
                    contentDescription = null,
                    tint = OvlTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(id = R.string.terminal_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = OvlTheme.colors.textPrimary
            )
        }

        if (executionState.status != ExecutionStatus.RUNNING) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(OvlTheme.colors.surfaceContainer)
                    .clickable {
                        com.orionisli.ovl0ad3r.HapticUtil.click()
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = null,
                    tint = OvlTheme.colors.textPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun TerminalProgressBar(
    progress: Float,
    action: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = action.ifEmpty { stringResource(R.string.execution_preparing) },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = OvlTheme.colors.textSecondary
            )
            if (progress in 0f..1f) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = OvlTheme.colors.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (progress in 0f..1f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = OvlTheme.colors.primary,
                trackColor = OvlTheme.colors.surfaceContainer,
                strokeCap = StrokeCap.Round
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = OvlTheme.colors.primary,
                trackColor = OvlTheme.colors.surfaceContainer,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun TerminalBottomBar(
    executionState: ExecutionState,
    reloadLevel: ReloadLevel,
    onDone: () -> Unit,
    onRestartZygote: () -> Unit,
    onCancel: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 180f),
        label = "terminalBtnScale"
    )

    when (executionState.status) {
        ExecutionStatus.RUNNING -> {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(buttonScale)
                    .height(44.dp),
                interactionSource = interaction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OvlTheme.colors.surfaceContainer,
                    contentColor = OvlTheme.colors.textPrimary
                )
            ) {
                Text(
                    text = stringResource(id = R.string.action_cancel),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        ExecutionStatus.COMPLETED -> {
            if (reloadLevel == ReloadLevel.ZYGOTE) {
                Button(
                    onClick = onRestartZygote,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(buttonScale)
                        .height(44.dp),
                    interactionSource = interaction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OvlTheme.colors.warning,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.action_restart_zygote),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            } else {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(buttonScale)
                        .height(44.dp),
                    interactionSource = interaction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OvlTheme.colors.primary,
                        contentColor = OvlTheme.colors.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.action_done),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
        ExecutionStatus.FAILED -> {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(buttonScale)
                    .height(44.dp),
                interactionSource = interaction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OvlTheme.colors.error,
                    contentColor = OvlTheme.colors.onError
                )
            ) {
                Text(
                    text = stringResource(id = R.string.action_done),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        ExecutionStatus.IDLE -> {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(buttonScale)
                    .height(44.dp),
                interactionSource = interaction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OvlTheme.colors.surfaceContainer,
                    contentColor = OvlTheme.colors.textPrimary
                )
            ) {
                Text(
                    text = stringResource(id = R.string.action_done),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
