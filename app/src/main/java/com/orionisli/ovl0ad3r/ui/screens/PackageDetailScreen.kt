package com.orionisli.ovl0ad3r.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.data.model.DepictionModel
import com.orionisli.ovl0ad3r.data.model.PackageItem
import com.orionisli.ovl0ad3r.data.model.ReloadLevel
import com.orionisli.ovl0ad3r.ui.components.PackageButtonState
import com.orionisli.ovl0ad3r.ui.components.SileoPillActionButton
import com.orionisli.ovl0ad3r.ui.components.formatByteSize
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PackageDetailScreen(
    packageItem: PackageItem,
    depiction: DepictionModel? = null,
    isQueued: Boolean = false,
    isInstalled: Boolean = false,
    onBack: () -> Unit,
    onActionClick: () -> Unit,
    onDependencyClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val effectiveDepiction = depiction ?: DepictionModel(
        headerTitle = packageItem.name,
        subtitle = packageItem.description,
        screenshots = emptyList(),
        specs = buildMap {
            put(stringResource(id = R.string.module_version), packageItem.version)
            put(stringResource(id = R.string.detail_spec_architecture), packageItem.architecture)
            put(
                stringResource(id = R.string.detail_spec_package_size),
                formatByteSize(packageItem.sizeBytes)
            )
            put(
                stringResource(id = R.string.module_author),
                packageItem.author.ifEmpty { stringResource(id = R.string.detail_default_author) }
            )
            if (packageItem.license.isNotBlank()) {
                put(stringResource(id = R.string.detail_spec_license), packageItem.license)
            }
            if (packageItem.homepage.isNotBlank()) {
                put(stringResource(id = R.string.detail_spec_homepage), packageItem.homepage)
            }
            put(
                stringResource(id = R.string.detail_spec_target_partition),
                "/${packageItem.targetPartition}"
            )
            put(
                stringResource(id = R.string.detail_spec_runtime_requirement),
                when (packageItem.reloadLevel) {
                    ReloadLevel.ZYGOTE -> stringResource(id = R.string.action_restart_zygote)
                    ReloadLevel.REBOOT -> stringResource(id = R.string.detail_runtime_reboot)
                    ReloadLevel.UICACHE -> stringResource(id = R.string.detail_runtime_refresh_icons)
                    ReloadLevel.NONE -> stringResource(id = R.string.detail_runtime_none)
                }
            )
        },
        changelog = "",
        dependencies = packageItem.dependencies
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OvlTheme.colors.background)
            .statusBarsPadding()
    ) {
        DetailTopBar(onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                DetailHeaderBanner(
                    packageItem = packageItem,
                    isQueued = isQueued,
                    isInstalled = isInstalled,
                    onActionClick = onActionClick
                )
            }

            if (effectiveDepiction.screenshots.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    ScreenshotGallery(screenshots = effectiveDepiction.screenshots)
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
                SpecsSection(specs = effectiveDepiction.specs)
            }

            if (effectiveDepiction.dependencies.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    DependenciesSection(
                        dependencies = effectiveDepiction.dependencies,
                        onDependencyClick = onDependencyClick
                    )
                }
            }

            if (effectiveDepiction.changelog.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    ChangelogSection(changelog = effectiveDepiction.changelog)
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            com.orionisli.ovl0ad3r.HapticUtil.tick()
            onBack()
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = null,
                tint = OvlTheme.colors.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = stringResource(id = R.string.module_details),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            color = OvlTheme.colors.textPrimary
        )
    }
}

@Composable
private fun DetailHeaderBanner(
    packageItem: PackageItem,
    isQueued: Boolean,
    isInstalled: Boolean,
    onActionClick: () -> Unit
) {
    val bannerBrush = Brush.linearGradient(
        colors = listOf(
            OvlTheme.colors.primary.copy(alpha = 0.2f),
            OvlTheme.colors.surfaceContainer
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bannerBrush)
            .border(1.dp, OvlTheme.colors.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(OvlTheme.colors.surface)
                        .border(1.dp, OvlTheme.colors.outlineVariant, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_package),
                        contentDescription = null,
                        tint = OvlTheme.colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = packageItem.name.ifEmpty { packageItem.id },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = OvlTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${packageItem.author.ifEmpty { "0vl0ad3r" }} • v${packageItem.version}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OvlTheme.colors.textSecondary
                    )
                }

                val buttonState = when {
                    packageItem.isSystemPackage -> PackageButtonState.SYSTEM
                    isQueued -> PackageButtonState.QUEUED
                    isInstalled -> PackageButtonState.MANAGE
                    else -> PackageButtonState.GET
                }
                SileoPillActionButton(
                    state = buttonState,
                    onClick = onActionClick
                )
            }

            if (packageItem.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = packageItem.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = OvlTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun ScreenshotGallery(screenshots: List<String>) {
    Column {
        Text(
            text = stringResource(id = R.string.detail_preview_title),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = OvlTheme.colors.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(screenshots) { id ->
                Box(
                    modifier = Modifier
                        .size(width = 140.dp, height = 240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(OvlTheme.colors.surfaceContainer)
                        .border(1.dp, OvlTheme.colors.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_terminal),
                            contentDescription = null,
                            tint = OvlTheme.colors.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.detail_preview_placeholder, id),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = OvlTheme.colors.textTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecsSection(specs: Map<String, String>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(id = R.string.detail_specs_title),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = OvlTheme.colors.textPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(OvlTheme.colors.surface)
                .border(1.dp, OvlTheme.colors.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            specs.entries.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OvlTheme.colors.textSecondary
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = OvlTheme.colors.textPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DependenciesSection(
    dependencies: List<String>,
    onDependencyClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(id = R.string.module_dependencies),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = OvlTheme.colors.textPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dependencies.forEach { dep ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(OvlTheme.colors.surfaceVariant)
                        .border(1.dp, OvlTheme.colors.outlineVariant.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            com.orionisli.ovl0ad3r.HapticUtil.click()
                            onDependencyClick(dep)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = dep,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = OvlTheme.colors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangelogSection(changelog: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(id = R.string.detail_changelog_title),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = OvlTheme.colors.textPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(OvlTheme.colors.surface)
                .border(1.dp, OvlTheme.colors.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(
                text = changelog,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                ),
                color = OvlTheme.colors.textPrimary
            )
        }
    }
}
