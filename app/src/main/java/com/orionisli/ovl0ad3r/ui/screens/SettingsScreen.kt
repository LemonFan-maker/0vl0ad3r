package com.orionisli.ovl0ad3r.ui.screens

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.orionisli.ovl0ad3r.ui.components.LocalToastState
import com.orionisli.ovl0ad3r.ui.components.ToastType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.orionisli.ovl0ad3r.HapticUtil
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.data.engine.BootstrapInstaller
import com.orionisli.ovl0ad3r.data.engine.BootstrapState
import com.orionisli.ovl0ad3r.data.ipc.GhostRainBridge
import com.orionisli.ovl0ad3r.ui.components.CustomTerminalSheet
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme
import com.orionisli.ovl0ad3r.ui.theme.OvlThemeType
import com.orionisli.ovl0ad3r.ui.theme.TerminalThemeType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentTheme: OvlThemeType = OvlThemeType.Sakura,
    onThemeChange: (OvlThemeType) -> Unit = {},
    currentTerminalTheme: TerminalThemeType = TerminalThemeType.Parchment,
    onTerminalThemeChange: (TerminalThemeType) -> Unit = {},
    terminalBgCustom: String = "#1E1E2E",
    terminalFgCustom: String = "#CDD6F4",
    onCustomTerminalSave: (String, String) -> Unit = { _, _ -> },
    hapticStrength: Int = 2,
    onHapticStrengthChange: (Int) -> Unit = {},
    currentLanguage: String = "zh-CN",
    onLanguageChange: (String) -> Unit = {},
    bridge: GhostRainBridge? = null,
    onOpenBootstrapModal: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val toastState = LocalToastState.current
    val context = LocalContext.current

    val bootstrapInstaller = remember { BootstrapInstaller.getInstance() }
    val bootstrapState by bootstrapInstaller.state.collectAsState()
    val bootstrapVersion by bootstrapInstaller.version.collectAsState()

    LaunchedEffect(Unit) {
        bootstrapInstaller.checkStatus()
    }

    val msgPartitionsRefreshed = stringResource(R.string.settings_partitions_refreshed)
    val msgSyncInProgress = stringResource(R.string.settings_sync_in_progress)
    val msgSyncSuccess = stringResource(R.string.settings_sync_success)
    val msgSyncFailure = stringResource(R.string.settings_sync_failure)
    val msgZygoteSending = stringResource(R.string.settings_zygote_sending)
    val msgZygoteSent = stringResource(R.string.settings_zygote_sent)
    val msgZygoteFailed = stringResource(R.string.settings_zygote_failed)

    val strThemeApplied = stringResource(R.string.toast_theme_applied)
    val strSakura = stringResource(R.string.theme_sakura)
    val strBeige = stringResource(R.string.theme_beige)
    val strSlate = stringResource(R.string.theme_slate)
    val strCyber = stringResource(R.string.theme_cyber)
    val strMatcha = stringResource(R.string.theme_matcha)
    val strNord = stringResource(R.string.theme_nord)

    val msgTermThemeApplied = stringResource(R.string.toast_terminal_theme_applied)
    val msgCustomTermApplied = stringResource(R.string.toast_custom_term_applied)
    val msgCustomTermInvalid = stringResource(R.string.toast_custom_term_invalid)
    val msgLangChanged = stringResource(R.string.toast_language_changed)
    val msgDeletingBootstrap = stringResource(R.string.toast_deleting_bootstrap)

    var showRestartZygoteDialog by remember { mutableStateOf(false) }
    var showDeleteBootstrapDialog by remember { mutableStateOf(false) }
    var showCustomTerminalSheet by remember { mutableStateOf(false) }

    val isConnected = bridge?.isConnected?.collectAsState()?.value ?: false
    val isRootAlive = bridge?.isRootAlive?.collectAsState()?.value ?: false
    val partitionStates = bridge?.partitionStates?.collectAsState()?.value ?: emptyMap()

    LaunchedEffect(bridge) {
        bridge?.refreshPartitionStates()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OvlTheme.colors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 0.dp, bottom = 120.dp)
        ) {
            item {
                SettingsHeader(onBack = onBack)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsGroupHeader(title = stringResource(R.string.settings_group_appearance))
                SettingsGroupCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_ui_theme_title),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = OvlTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ThemePillButton(
                                name = stringResource(R.string.theme_sakura),
                                swatchColor = Color(0xFFE14979),
                                isSelected = currentTheme == OvlThemeType.Sakura,
                                onClick = {
                                    HapticUtil.confirm()
                                    onThemeChange(OvlThemeType.Sakura)
                                    toastState.show(
                                        msg = strThemeApplied.format(strSakura),
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ThemePillButton(
                                name = stringResource(R.string.theme_beige),
                                swatchColor = Color(0xFF693FB4),
                                isSelected = currentTheme == OvlThemeType.Beige,
                                onClick = {
                                    HapticUtil.confirm()
                                    onThemeChange(OvlThemeType.Beige)
                                    toastState.show(
                                        msg = strThemeApplied.format(strBeige),
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ThemePillButton(
                                name = stringResource(R.string.theme_slate),
                                swatchColor = Color(0xFF1D6FD8),
                                isSelected = currentTheme == OvlThemeType.Slate,
                                onClick = {
                                    HapticUtil.confirm()
                                    onThemeChange(OvlThemeType.Slate)
                                    toastState.show(
                                        msg = strThemeApplied.format(strSlate),
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ThemePillButton(
                                name = stringResource(R.string.theme_cyber),
                                swatchColor = Color(0xFF8B5CF6),
                                isSelected = currentTheme == OvlThemeType.Cyber,
                                onClick = {
                                    HapticUtil.confirm()
                                    onThemeChange(OvlThemeType.Cyber)
                                    toastState.show(
                                        msg = strThemeApplied.format(strCyber),
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ThemePillButton(
                                name = stringResource(R.string.theme_matcha),
                                swatchColor = Color(0xFF2F855A),
                                isSelected = currentTheme == OvlThemeType.Matcha,
                                onClick = {
                                    HapticUtil.confirm()
                                    onThemeChange(OvlThemeType.Matcha)
                                    toastState.show(
                                        msg = strThemeApplied.format(strMatcha),
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ThemePillButton(
                                name = stringResource(R.string.theme_nord),
                                swatchColor = Color(0xFF00E5BC),
                                isSelected = currentTheme == OvlThemeType.Nord,
                                onClick = {
                                    HapticUtil.confirm()
                                    onThemeChange(OvlThemeType.Nord)
                                    toastState.show(
                                        msg = strThemeApplied.format(strNord),
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        SettingsFullDivider(modifier = Modifier.padding(vertical = 14.dp))

                        Text(
                            text = stringResource(R.string.settings_terminal_theme_title),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = OvlTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TerminalPillButton(
                                name = stringResource(R.string.terminal_theme_parchment),
                                isSelected = currentTerminalTheme == TerminalThemeType.Parchment,
                                onClick = {
                                    HapticUtil.click()
                                    onTerminalThemeChange(TerminalThemeType.Parchment)
                                    toastState.show(
                                        msg = msgTermThemeApplied,
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            TerminalPillButton(
                                name = stringResource(R.string.terminal_theme_matrix),
                                isSelected = currentTerminalTheme == TerminalThemeType.Matrix,
                                onClick = {
                                    HapticUtil.click()
                                    onTerminalThemeChange(TerminalThemeType.Matrix)
                                    toastState.show(
                                        msg = msgTermThemeApplied,
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            TerminalPillButton(
                                name = stringResource(R.string.terminal_theme_solarized),
                                isSelected = currentTerminalTheme == TerminalThemeType.Solarized,
                                onClick = {
                                    HapticUtil.click()
                                    onTerminalThemeChange(TerminalThemeType.Solarized)
                                    toastState.show(
                                        msg = msgTermThemeApplied,
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TerminalPillButton(
                                name = stringResource(R.string.terminal_theme_monokai),
                                isSelected = currentTerminalTheme == TerminalThemeType.Monokai,
                                onClick = {
                                    HapticUtil.click()
                                    onTerminalThemeChange(TerminalThemeType.Monokai)
                                    toastState.show(
                                        msg = msgTermThemeApplied,
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            TerminalPillButton(
                                name = stringResource(R.string.terminal_theme_custom),
                                iconRes = R.drawable.ic_palette,
                                isSelected = currentTerminalTheme == TerminalThemeType.Custom,
                                onClick = {
                                    HapticUtil.click()
                                    showCustomTerminalSheet = true
                                },
                                modifier = Modifier.weight(2f)
                            )
                        }

                        SettingsFullDivider(modifier = Modifier.padding(vertical = 14.dp))

                        Text(
                            text = stringResource(R.string.settings_haptic_title),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = OvlTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HapticPillButton(
                                name = stringResource(R.string.haptic_off),
                                strength = 0,
                                isSelected = hapticStrength == 0,
                                onClick = {
                                    HapticUtil.strength = 0
                                    onHapticStrengthChange(0)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HapticPillButton(
                                name = stringResource(R.string.haptic_weak),
                                strength = 1,
                                isSelected = hapticStrength == 1,
                                onClick = {
                                    HapticUtil.strength = 1
                                    HapticUtil.preview(1)
                                    onHapticStrengthChange(1)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HapticPillButton(
                                name = stringResource(R.string.haptic_std),
                                strength = 2,
                                isSelected = hapticStrength == 2,
                                onClick = {
                                    HapticUtil.strength = 2
                                    HapticUtil.preview(2)
                                    onHapticStrengthChange(2)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HapticPillButton(
                                name = stringResource(R.string.haptic_strong),
                                strength = 3,
                                isSelected = hapticStrength == 3,
                                onClick = {
                                    HapticUtil.strength = 3
                                    HapticUtil.preview(3)
                                    onHapticStrengthChange(3)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                SettingsGroupHeader(title = stringResource(R.string.settings_group_general))
                SettingsGroupCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_language_title),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = OvlTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LanguagePillButton(
                                name = "简体中文",
                                isSelected = currentLanguage == "zh-CN" || currentLanguage == "zh",
                                onClick = {
                                    HapticUtil.click()
                                    onLanguageChange("zh-CN")
                                    toastState.show(
                                        msg = msgLangChanged,
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            LanguagePillButton(
                                name = "English",
                                isSelected = currentLanguage == "en",
                                onClick = {
                                    HapticUtil.click()
                                    onLanguageChange("en")
                                    toastState.show(
                                        msg = msgLangChanged,
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            LanguagePillButton(
                                name = "繁體中文",
                                isSelected = currentLanguage == "zh-TW" || currentLanguage == "zh-HK",
                                onClick = {
                                    HapticUtil.click()
                                    onLanguageChange("zh-TW")
                                    toastState.show(
                                        msg = msgLangChanged,
                                        toastType = ToastType.INFO,
                                        durationMs = 1800L,
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                SettingsGroupHeader(title = stringResource(R.string.settings_group_runtime))
                SettingsGroupCard {
                    // AIDL服务与Root存活状态
                    SettingsStatusRowItem(
                        isConnected = isConnected,
                        isRootAlive = isRootAlive
                    )

                    SettingsDivider()

                    PartitionStatusSection(
                        partitionStates = partitionStates,
                        onRefresh = {
                            HapticUtil.click()
                            coroutineScope.launch {
                                bridge?.refreshPartitionStates()
                                toastState.show(
                                    msg = msgPartitionsRefreshed,
                                    toastType = ToastType.SUCCESS,
                                    scope = coroutineScope
                                )
                                HapticUtil.confirm()
                            }
                        }
                    )

                    SettingsDivider()

                    SettingsActionRowItem(
                        iconRes = R.drawable.ic_refresh,
                        title = stringResource(R.string.settings_restart_desktop_title),
                        subtitle = stringResource(R.string.settings_restart_desktop_subtitle),
                        actionButtonText = stringResource(R.string.settings_action_restart),
                        onClick = {
                            HapticUtil.click()
                            showRestartZygoteDialog = true
                        }
                    )

                    SettingsDivider()

                    SettingsActionRowItem(
                        iconRes = R.drawable.ic_check_circle,
                        title = stringResource(R.string.settings_sync_storage_title),
                        subtitle = stringResource(R.string.settings_sync_storage_subtitle),
                        actionButtonText = stringResource(R.string.settings_action_sync),
                        onClick = {
                            HapticUtil.click()
                            coroutineScope.launch {
                                toastState.show(
                                    msg = msgSyncInProgress,
                                    toastType = ToastType.INFO,
                                    durationMs = 1500L,
                                    scope = coroutineScope
                                )
                                val success = bridge?.syncStorage() ?: false
                                if (success) {
                                    toastState.show(
                                        msg = msgSyncSuccess,
                                        toastType = ToastType.SUCCESS,
                                        scope = coroutineScope
                                    )
                                    HapticUtil.success()
                                } else {
                                    toastState.show(
                                        msg = msgSyncFailure,
                                        toastType = ToastType.ERROR,
                                        scope = coroutineScope
                                    )
                                    HapticUtil.error()
                                }
                            }
                        }
                    )

                    SettingsDivider()

                    val bootstrapSubtitle = when (bootstrapState) {
                        com.orionisli.ovl0ad3r.data.engine.BootstrapState.READY ->
                            stringResource(R.string.bootstrap_status_ready, bootstrapVersion ?: "7.1.0")
                        com.orionisli.ovl0ad3r.data.engine.BootstrapState.CORRUPTED ->
                            stringResource(R.string.bootstrap_status_corrupted)
                        com.orionisli.ovl0ad3r.data.engine.BootstrapState.INSTALLING ->
                            stringResource(R.string.status_extracting)
                        com.orionisli.ovl0ad3r.data.engine.BootstrapState.NOT_INSTALLED ->
                            stringResource(R.string.bootstrap_status_not_installed)
                    }

                    val bootstrapActionText = when (bootstrapState) {
                        com.orionisli.ovl0ad3r.data.engine.BootstrapState.READY ->
                            stringResource(R.string.bootstrap_action_reinstall)
                        com.orionisli.ovl0ad3r.data.engine.BootstrapState.CORRUPTED ->
                            stringResource(R.string.bootstrap_action_repair)
                        com.orionisli.ovl0ad3r.data.engine.BootstrapState.INSTALLING ->
                            stringResource(R.string.status_extracting)
                        com.orionisli.ovl0ad3r.data.engine.BootstrapState.NOT_INSTALLED ->
                            stringResource(R.string.bootstrap_action_install)
                    }

                    SettingsActionRowItem(
                        iconRes = R.drawable.ic_package,
                        title = stringResource(R.string.bootstrap_status_title),
                        subtitle = bootstrapSubtitle,
                        actionButtonText = bootstrapActionText,
                        onClick = {
                            HapticUtil.click()
                            onOpenBootstrapModal()
                        }
                    )

                    SettingsDivider()

                    val canDeleteBootstrap = bootstrapState == BootstrapState.READY ||
                            bootstrapState == BootstrapState.CORRUPTED
                    SettingsActionRowItem(
                        iconRes = R.drawable.ic_trash,
                        title = stringResource(R.string.settings_delete_bootstrap_title),
                        subtitle = stringResource(R.string.settings_delete_bootstrap_subtitle),
                        actionButtonText = stringResource(R.string.settings_action_delete),
                        isDestructive = true,
                        enabled = canDeleteBootstrap,
                        onClick = {
                            HapticUtil.click()
                            showDeleteBootstrapDialog = true
                        }
                    )

                    SettingsDivider()

                    SettingsActionRowItem(
                        iconRes = R.drawable.ic_warning,
                        title = stringResource(R.string.settings_clean_ghost_title),
                        subtitle = stringResource(R.string.settings_clean_ghost_subtitle),
                        actionButtonText = stringResource(R.string.settings_action_clean),
                        onClick = {
                            HapticUtil.click()
                            coroutineScope.launch {
                                val (ok, msg) = bootstrapInstaller.clearGhostFiles()
                                toastState.show(
                                    msg = msg,
                                    toastType = if (ok) ToastType.SUCCESS else ToastType.INFO,
                                    durationMs = 2500L,
                                    scope = coroutineScope
                                )
                                if (ok) HapticUtil.confirm()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                SettingsGroupHeader(title = stringResource(R.string.settings_about))
                SettingsGroupCard {
                    SettingsRowItem(
                        iconRes = R.drawable.ic_info,
                        title = stringResource(R.string.settings_about_app, stringResource(R.string.app_name)),
                        subtitle = stringResource(R.string.settings_version),
                        value = "",
                        onClick = {
                            HapticUtil.click()
                            onNavigateToAbout()
                        }
                    )
                }
            }
        }
    }

    if (showRestartZygoteDialog) {
        AlertDialog(
            onDismissRequest = { showRestartZygoteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_restart_desktop_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = OvlTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_restart_desktop_dialog_message),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = OvlTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestartZygoteDialog = false
                        coroutineScope.launch {
                            toastState.show(
                                msg = msgZygoteSending,
                                toastType = ToastType.INFO,
                                durationMs = 1500L,
                                scope = coroutineScope
                            )
                            val ok = bridge?.restartZygote() ?: false
                            if (ok) {
                                toastState.show(
                                    msg = msgZygoteSent,
                                    toastType = ToastType.SUCCESS,
                                    scope = coroutineScope
                                )
                                HapticUtil.success()
                            } else {
                                toastState.show(
                                    msg = msgZygoteFailed,
                                    toastType = ToastType.ERROR,
                                    scope = coroutineScope
                                )
                                HapticUtil.error()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = OvlTheme.colors.primary
                    )
                ) {
                    Text(text = stringResource(R.string.settings_confirm_restart), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestartZygoteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = OvlTheme.colors.textSecondary
                    )
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
            containerColor = OvlTheme.colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showDeleteBootstrapDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteBootstrapDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_bootstrap_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = OvlTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_delete_bootstrap_message),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = OvlTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteBootstrapDialog = false
                        coroutineScope.launch {
                            toastState.show(
                                msg = msgDeletingBootstrap,
                                toastType = ToastType.INFO,
                                durationMs = 2000L,
                                scope = coroutineScope
                            )
                            val res = bootstrapInstaller.uninstallBootstrap(context)
                            res.onSuccess { msg ->
                                toastState.show(
                                    msg = msg,
                                    toastType = ToastType.SUCCESS,
                                    durationMs = 2500L,
                                    scope = coroutineScope
                                )
                                HapticUtil.confirm()
                            }.onFailure { err ->
                                toastState.show(
                                    msg = err.message ?: "删除失败",
                                    toastType = ToastType.ERROR,
                                    durationMs = 2500L,
                                    scope = coroutineScope
                                )
                                HapticUtil.error()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = OvlTheme.colors.error
                    )
                ) {
                    Text(
                        text = stringResource(R.string.dialog_action_delete_confirm),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteBootstrapDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = OvlTheme.colors.textSecondary
                    )
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
            containerColor = OvlTheme.colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showCustomTerminalSheet) {
        CustomTerminalSheet(
            initialBgHex = terminalBgCustom,
            initialFgHex = terminalFgCustom,
            onDismissRequest = { showCustomTerminalSheet = false },
            onSave = { bg, fg ->
                onCustomTerminalSave(bg, fg)
                onTerminalThemeChange(TerminalThemeType.Custom)
                toastState.show(
                    msg = msgCustomTermApplied,
                    toastType = ToastType.SUCCESS,
                    durationMs = 1800L,
                    scope = coroutineScope
                )
            },
            onInvalidInput = {
                toastState.show(
                    msg = msgCustomTermInvalid,
                    toastType = ToastType.ERROR,
                    durationMs = 2500L,
                    scope = coroutineScope
                )
            }
        )
    }
}

@Composable
private fun ThemePillButton(
    name: String,
    swatchColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) OvlTheme.colors.primary else OvlTheme.colors.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) OvlTheme.colors.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
    val textColor = if (isSelected) OvlTheme.colors.primary else OvlTheme.colors.textSecondary

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TerminalPillButton(
    name: String,
    iconRes: Int? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) OvlTheme.colors.primary else OvlTheme.colors.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) OvlTheme.colors.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
    val textColor = if (isSelected) OvlTheme.colors.primary else OvlTheme.colors.textSecondary

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HapticPillButton(
    name: String,
    strength: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) OvlTheme.colors.primary else OvlTheme.colors.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) OvlTheme.colors.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
    val textColor = if (isSelected) OvlTheme.colors.primary else OvlTheme.colors.textSecondary

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LanguagePillButton(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) OvlTheme.colors.primary else OvlTheme.colors.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) OvlTheme.colors.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
    val textColor = if (isSelected) OvlTheme.colors.primary else OvlTheme.colors.textSecondary

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsFullDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.6.dp)
            .background(OvlTheme.colors.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun SettingsHeader(onBack: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        if (onBack != null) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 140f),
                label = "backBtnScale"
            )

            IconButton(
                onClick = onBack,
                interactionSource = interactionSource,
                modifier = Modifier
                    .scale(scale)
                    .padding(bottom = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.settings_back),
                    tint = OvlTheme.colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Text(
            text = stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = OvlTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = stringResource(R.string.settings_header_subtitle),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = OvlTheme.colors.textSecondary
        )
    }
}

@Composable
private fun SettingsGroupCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(OvlTheme.colors.surface)
            .border(
                width = 1.dp,
                color = OvlTheme.colors.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        content()
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        ),
        color = OvlTheme.colors.textSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsRowItem(
    iconRes: Int,
    title: String,
    subtitle: String,
    value: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(OvlTheme.colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = OvlTheme.colors.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = OvlTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = OvlTheme.colors.textSecondary
            )
        }

        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = OvlTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = OvlTheme.colors.textTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsActionRowItem(
    iconRes: Int,
    title: String,
    subtitle: String,
    actionButtonText: String,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.45f
    val iconBgColor = if (isDestructive) {
        OvlTheme.colors.error.copy(alpha = 0.12f)
    } else {
        OvlTheme.colors.primary.copy(alpha = 0.12f)
    }
    val iconTint = if (isDestructive) OvlTheme.colors.error else OvlTheme.colors.primary
    val buttonBgColor = if (isDestructive) {
        OvlTheme.colors.error.copy(alpha = 0.15f)
    } else {
        OvlTheme.colors.primary.copy(alpha = 0.15f)
    }
    val buttonTextColor = if (isDestructive) OvlTheme.colors.error else OvlTheme.colors.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = OvlTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = OvlTheme.colors.textSecondary
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(buttonBgColor)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = actionButtonText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = buttonTextColor
            )
        }
    }
}

@Composable
private fun SettingsStatusRowItem(
    isConnected: Boolean,
    isRootAlive: Boolean
) {
    val statusColor = when {
        isConnected && isRootAlive -> OvlTheme.colors.success
        isConnected -> OvlTheme.colors.warning
        else -> OvlTheme.colors.error
    }

    val statusText = when {
        isConnected && isRootAlive -> stringResource(R.string.settings_status_root_alive)
        isConnected -> stringResource(R.string.settings_status_not_root)
        else -> stringResource(R.string.settings_status_disconnected)
    }

    val subtitle = when {
        isConnected && isRootAlive -> stringResource(R.string.settings_status_root_alive_desc)
        isConnected -> stringResource(R.string.settings_status_connected_no_root_desc)
        else -> stringResource(R.string.settings_status_disconnected_desc)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(statusColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_shield),
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_service_status_title),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = OvlTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = OvlTheme.colors.textSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = statusColor
            )
        }
    }
}

@Composable
private fun PartitionStatusSection(
    partitionStates: Map<String, Int>,
    onRefresh: () -> Unit
) {
    val partitions = listOf("system", "vendor", "product")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_overlay_mount_status),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = OvlTheme.colors.textPrimary
            )

            Text(
                text = stringResource(R.string.settings_refresh_status),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = OvlTheme.colors.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            partitions.forEach { part ->
                val isMounted = (partitionStates[part] ?: 0) == 1
                val mountColor = if (isMounted) OvlTheme.colors.success else OvlTheme.colors.outline

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OvlTheme.colors.background)
                        .border(
                            width = 1.dp,
                            color = if (isMounted) mountColor.copy(alpha = 0.5f) else OvlTheme.colors.outlineVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "/$part",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = OvlTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(mountColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMounted) {
                                    stringResource(R.string.partition_mounted)
                                } else {
                                    stringResource(R.string.partition_unmounted)
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = mountColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp)
            .height(0.5.dp)
            .background(OvlTheme.colors.outlineVariant.copy(alpha = 0.35f))
    )
}
