package com.orionisli.ovl0ad3r.ui.screens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

@Composable
fun AboutScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OvlTheme.colors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 0.dp, bottom = 100.dp)
        ) {
            item {
                AboutTopBar(onBack = onBack)
            }

            item {
                AboutHeaderSection()
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                AboutSectionTitle(title = stringResource(id = R.string.about_section_runtime))
                AboutGroupCard {
                    AboutInfoRow(
                        label = stringResource(id = R.string.about_label_device),
                        value = "vivo iQOO Neo9S Pro (V2339FA)"
                    )
                    AboutDivider()
                    AboutInfoRow(
                        label = stringResource(id = R.string.about_label_os),
                        value = "Android 16 (BP2A)"
                    )
                    AboutDivider()
                    AboutInfoRow(
                        label = stringResource(id = R.string.about_label_kernel),
                        value = "6.1.145"
                    )
                    AboutDivider()
                    AboutInfoRow(
                        label = stringResource(id = R.string.about_label_root_base),
                        value = "GhostLock (" + stringResource(id = R.string.about_volatile_root) + ")"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                AboutSectionTitle(title = stringResource(id = R.string.about_section_architecture))
                AboutGroupCard {
                    AboutInfoRow(
                        label = stringResource(id = R.string.about_label_virtual_fs),
                        value = "PD2339FA-Gh0stra1n (ext4 loop)"
                    )
                    AboutDivider()
                    AboutInfoRow(
                        label = stringResource(id = R.string.about_label_package_manager),
                        value = "0vl0ad3r (" + stringResource(id = R.string.about_package_manager_model) + ")"
                    )
                    AboutDivider()
                    AboutInfoRow(
                        label = stringResource(id = R.string.about_label_runtime_prefix),
                        value = "/system/bin/0vl0ad3r-b1n"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                AboutSectionTitle(title = stringResource(id = R.string.about_section_credits))
                AboutGroupCard {
                    AboutCreditRow(
                        title = "Sileo Team",
                        description = stringResource(id = R.string.about_credit_sileo),
                        badgeText = "UI/UX"
                    )
                    AboutDivider()
                    AboutCreditRow(
                        title = "Arch Linux (ALPM)",
                        description = stringResource(id = R.string.about_credit_arch),
                        badgeText = "ALPM"
                    )
                    AboutDivider()
                    AboutCreditRow(
                        title = "Termux Community",
                        description = stringResource(id = R.string.about_credit_termux),
                        badgeText = "Bionic"
                    )
                    AboutDivider()
                    AboutCreditRow(
                        title = "KernelSU",
                        description = stringResource(id = R.string.about_credit_kernelsu),
                        badgeText = "Kernel"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                AboutSectionTitle(title = stringResource(id = R.string.about_section_license))
                AboutGroupCard {
                    AboutInfoRow(
                        label = stringResource(id = R.string.about_label_open_source_license),
                        value = "Apache-2.0 " + stringResource(id = R.string.about_license_word)
                    )
                    AboutDivider()
                    AboutInfoRow(
                        label = stringResource(id = R.string.about_label_project_focus),
                        value = stringResource(id = R.string.about_value_project_focus)
                    )
                    AboutDivider()
                    AboutInfoRow(
                        label = stringResource(id = R.string.about_label_copyright),
                        value = "Copyright © 2026 0vl0ad3r Project"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(id = R.string.about_footer_tagline),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = OvlTheme.colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AboutTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1f,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 140f),
            label = "backBtnScale"
        )

        IconButton(
            onClick = {
                com.orionisli.ovl0ad3r.HapticUtil.tick()
                onBack()
            },
            interactionSource = interactionSource,
            modifier = Modifier.scale(scale)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(id = R.string.about_action_back),
                tint = OvlTheme.colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = stringResource(id = R.string.settings_about),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            color = OvlTheme.colors.textPrimary
        )
    }
}

@Composable
private fun AboutHeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            OvlTheme.colors.primary,
                            OvlTheme.colors.accent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = OvlTheme.colors.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_package),
                contentDescription = "0vl0ad3r",
                tint = OvlTheme.colors.onPrimary,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "0vl0ad3r",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = OvlTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(id = R.string.about_version_build, "1.0.0-bionic", 20260927),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            color = OvlTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(id = R.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            color = OvlTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun AboutGroupCard(
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
private fun AboutSectionTitle(title: String) {
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
private fun AboutInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = OvlTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp
            ),
            color = OvlTheme.colors.textSecondary,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun AboutCreditRow(
    title: String,
    description: String,
    badgeText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(OvlTheme.colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText.take(2),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = OvlTheme.colors.primary
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = OvlTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp
                ),
                color = OvlTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun AboutDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(0.5.dp)
            .background(OvlTheme.colors.outlineVariant.copy(alpha = 0.35f))
    )
}
