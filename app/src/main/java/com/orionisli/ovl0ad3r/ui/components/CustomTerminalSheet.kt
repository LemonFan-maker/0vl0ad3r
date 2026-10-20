package com.orionisli.ovl0ad3r.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.HapticUtil
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.ui.theme.MapleMonoFamily
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

data class CustomTerminalPreset(
    val name: String,
    val bgHex: String,
    val fgHex: String
)

val CUSTOM_TERMINAL_PRESETS = listOf(
    CustomTerminalPreset("Catppuccin", "#24273A", "#CAD3F5"),
    CustomTerminalPreset("Tokyo Night", "#1A1B26", "#C0CAF5"),
    CustomTerminalPreset("Dracula", "#282A36", "#F8F8F2"),
    CustomTerminalPreset("Nord", "#2E3440", "#ECEFF4"),
    CustomTerminalPreset("Gruvbox", "#282828", "#EBDBB2"),
    CustomTerminalPreset("Cyberpunk", "#0A0A12", "#00FFCC")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTerminalSheet(
    initialBgHex: String = "#1E1E2E",
    initialFgHex: String = "#CDD6F4",
    onDismissRequest: () -> Unit,
    onSave: (bgHex: String, fgHex: String) -> Unit,
    onInvalidInput: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editBg by remember { mutableStateOf(initialBgHex) }
    var editFg by remember { mutableStateOf(initialFgHex) }

    val parsedBg = remember(editBg) {
        runCatching { Color(android.graphics.Color.parseColor(editBg.trim())) }
            .getOrDefault(Color(0xFF1E1E2E))
    }

    val parsedFg = remember(editFg) {
        runCatching { Color(android.graphics.Color.parseColor(editFg.trim())) }
            .getOrDefault(Color(0xFFCDD6F4))
    }

    val previewBorder = remember(parsedFg) {
        Color(parsedFg.red, parsedFg.green, parsedFg.blue, alpha = 0.35f)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = OvlTheme.colors.surface,
        contentColor = OvlTheme.colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
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
                            .background(OvlTheme.colors.primaryContainer),
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
                        text = stringResource(id = R.string.dialog_custom_term_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = OvlTheme.colors.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OvlTheme.colors.primaryContainer)
                        .border(1.dp, OvlTheme.colors.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "RGB / HEX",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = MapleMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = OvlTheme.colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(parsedBg)
                    .border(1.5.dp, previewBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                val previewAnnotated = remember(parsedFg) {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF8C867B))) {
                            append("[12:00:00] ")
                        }
                        withStyle(SpanStyle(color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)) {
                            append("[INFO] ")
                        }
                        withStyle(SpanStyle(color = parsedFg)) {
                            append("OverlayFS live\n")
                        }

                        withStyle(SpanStyle(color = Color(0xFF8C867B))) {
                            append("[12:00:01] ")
                        }
                        withStyle(SpanStyle(color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)) {
                            append("[SU] ")
                        }
                        withStyle(SpanStyle(color = parsedFg)) {
                            append("truncate -s 1024M system.img ")
                        }
                        withStyle(SpanStyle(color = Color(0xFF15803D), fontWeight = FontWeight.Bold)) {
                            append("[OK]\n")
                        }

                        withStyle(SpanStyle(color = Color(0xFF8C867B))) {
                            append("[12:00:02] ")
                        }
                        withStyle(SpanStyle(color = Color(0xFF64748B), fontWeight = FontWeight.Bold)) {
                            append("[DEBUG] ")
                        }
                        withStyle(SpanStyle(color = parsedFg)) {
                            append("losetup /dev/loop8 -> live\n")
                        }

                        withStyle(SpanStyle(color = parsedFg, fontWeight = FontWeight.Medium)) {
                            append("0vl0ad3r Terminal Preview")
                        }
                    }
                }

                Text(
                    text = previewAnnotated,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = MapleMonoFamily,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(parsedBg)
                        .border(1.dp, OvlTheme.colors.outlineVariant, RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = editBg,
                    onValueChange = { editBg = it },
                    label = { Text(stringResource(id = R.string.dialog_custom_term_bg), fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = MapleMonoFamily,
                        fontSize = 13.sp,
                        color = OvlTheme.colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OvlTheme.colors.primary,
                        unfocusedBorderColor = OvlTheme.colors.outlineVariant,
                        focusedLabelColor = OvlTheme.colors.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(parsedFg)
                        .border(1.dp, OvlTheme.colors.outlineVariant, RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = editFg,
                    onValueChange = { editFg = it },
                    label = { Text(stringResource(id = R.string.dialog_custom_term_fg), fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = MapleMonoFamily,
                        fontSize = 13.sp,
                        color = OvlTheme.colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OvlTheme.colors.primary,
                        unfocusedBorderColor = OvlTheme.colors.outlineVariant,
                        focusedLabelColor = OvlTheme.colors.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(id = R.string.dialog_custom_term_preset),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = OvlTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CUSTOM_TERMINAL_PRESETS.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OvlTheme.colors.surfaceContainer)
                            .border(1.dp, OvlTheme.colors.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable {
                                HapticUtil.tick()
                                editBg = preset.bgHex
                                editFg = preset.fgHex
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp
                            ),
                            color = OvlTheme.colors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        HapticUtil.tick()
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OvlTheme.colors.textSecondary
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = {
                        val bgClean = editBg.trim()
                        val fgClean = editFg.trim()
                        val isBgValid = runCatching { android.graphics.Color.parseColor(bgClean) }.isSuccess
                        val isFgValid = runCatching { android.graphics.Color.parseColor(fgClean) }.isSuccess
                        if (!isBgValid || !isFgValid) {
                            HapticUtil.error()
                            onInvalidInput()
                        } else {
                            HapticUtil.success()
                            onSave(bgClean, fgClean)
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OvlTheme.colors.primary,
                        contentColor = OvlTheme.colors.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.btn_apply_term_theme),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
