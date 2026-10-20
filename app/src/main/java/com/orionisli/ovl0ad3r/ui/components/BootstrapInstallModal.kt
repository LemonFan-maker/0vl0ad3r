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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.orionisli.ovl0ad3r.HapticUtil
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.data.engine.AnsiParser
import com.orionisli.ovl0ad3r.data.engine.BootstrapState
import com.orionisli.ovl0ad3r.ui.theme.MapleMonoFamily
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

@Composable
fun BootstrapInstallModal(
    state: BootstrapState,
    version: String?,
    logLines: List<String>,
    onStartInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = state != BootstrapState.INSTALLING) {
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            if (state != BootstrapState.INSTALLING) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = state != BootstrapState.INSTALLING,
            dismissOnClickOutside = state != BootstrapState.INSTALLING,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (state != BootstrapState.INSTALLING) {
                        onDismiss()
                    }
                }
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(OvlTheme.colors.surface)
                    .border(1.dp, OvlTheme.colors.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 消费内部点击，防止误触关闭 */ }
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OvlTheme.colors.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_package),
                            contentDescription = null,
                            tint = OvlTheme.colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = stringResource(R.string.bootstrap_modal_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = OvlTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    val badgeColor = when (state) {
                        BootstrapState.READY -> OvlTheme.colors.success
                        BootstrapState.INSTALLING -> OvlTheme.colors.primary
                        BootstrapState.CORRUPTED -> OvlTheme.colors.error
                        BootstrapState.NOT_INSTALLED -> OvlTheme.colors.outline
                    }
                    val badgeText = when (state) {
                        BootstrapState.READY -> stringResource(R.string.bootstrap_status_ready, version ?: "7.1.0")
                        BootstrapState.INSTALLING -> stringResource(R.string.status_extracting)
                        BootstrapState.CORRUPTED -> stringResource(R.string.bootstrap_status_corrupted)
                        BootstrapState.NOT_INSTALLED -> stringResource(R.string.bootstrap_status_not_installed)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = badgeColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val terminalPalette = OvlTheme.terminal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(terminalPalette.background)
                        .border(1.dp, terminalPalette.border, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    val listState = rememberLazyListState()

                    LaunchedEffect(logLines.size) {
                        if (logLines.isNotEmpty()) {
                            listState.animateScrollToItem(logLines.lastIndex)
                        }
                    }

                    if (logLines.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state == BootstrapState.CORRUPTED) {
                                    stringResource(R.string.bootstrap_log_corrupted_hint)
                                } else {
                                    stringResource(R.string.bootstrap_log_idle_hint)
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = MapleMonoFamily,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                ),
                                color = terminalPalette.foreground.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 6.dp)
                        ) {
                            items(logLines) { line ->
                                val parsed = remember(line, terminalPalette) {
                                    AnsiParser.parse(line, terminalPalette)
                                }
                                Text(
                                    text = parsed,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = MapleMonoFamily,
                                        fontSize = 11.sp,
                                        lineHeight = 16.5.sp
                                    ),
                                    color = terminalPalette.foreground
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (state == BootstrapState.INSTALLING) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = OvlTheme.colors.primary,
                        trackColor = OvlTheme.colors.outlineVariant.copy(alpha = 0.3f),
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                val buttonInteraction = remember { MutableInteractionSource() }
                val isButtonPressed by buttonInteraction.collectIsPressedAsState()
                val buttonScale by animateFloatAsState(
                    targetValue = if (isButtonPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f),
                    label = "btnScale"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state == BootstrapState.READY) {
                        Button(
                            onClick = {
                                HapticUtil.click()
                                onDismiss()
                            },
                            modifier = Modifier.scale(buttonScale),
                            interactionSource = buttonInteraction,
                            colors = ButtonDefaults.buttonColors(containerColor = OvlTheme.colors.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.action_done),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else if (state == BootstrapState.INSTALLING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = OvlTheme.colors.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.bootstrap_deploying_label),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = OvlTheme.colors.textSecondary
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                HapticUtil.click()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                color = OvlTheme.colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val buttonText = if (state == BootstrapState.CORRUPTED) {
                            stringResource(R.string.bootstrap_action_repair)
                        } else {
                            stringResource(R.string.bootstrap_action_install_now)
                        }

                        Button(
                            onClick = {
                                HapticUtil.confirm()
                                onStartInstall()
                            },
                            modifier = Modifier.scale(buttonScale),
                            interactionSource = buttonInteraction,
                            colors = ButtonDefaults.buttonColors(containerColor = OvlTheme.colors.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = buttonText,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
