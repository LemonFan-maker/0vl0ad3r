package com.orionisli.ovl0ad3r.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.orionisli.ovl0ad3r.data.model.PackageFilter
import com.orionisli.ovl0ad3r.data.model.PackageItem
import com.orionisli.ovl0ad3r.ui.components.PackageButtonState
import com.orionisli.ovl0ad3r.ui.components.PackageCard
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

@Composable
fun InstalledScreen(
    installedPackages: List<PackageItem>,
    onPackageClick: (PackageItem) -> Unit,
    onQueueAction: (PackageItem) -> Unit,
    onBatchUpdate: (List<PackageItem>) -> Unit = {},
    queuedPackageNames: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(PackageFilter.ALL) }

    val filteredList = remember(installedPackages, selectedFilter) {
        when (selectedFilter) {
            PackageFilter.ALL -> installedPackages
            PackageFilter.USER -> installedPackages.filterNot { it.isSystemPackage }
            PackageFilter.SYSTEM -> installedPackages.filter { it.isSystemPackage }
        }
    }

    val updateAvailableList = remember(installedPackages) {
        installedPackages.filter { it.hasUpdate }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OvlTheme.colors.background)
            .statusBarsPadding()
    ) {
        InstalledTopHeader(
            totalCount = installedPackages.size,
            updateCount = updateAvailableList.size,
            onBatchUpdate = { onBatchUpdate(updateAvailableList) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        SegmentedControl(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.installed_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OvlTheme.colors.textTertiary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(
                    items = filteredList,
                    key = { it.id },
                    contentType = { "package" }
                ) { pkg ->
                    val buttonState = when {
                        pkg.isSystemPackage -> PackageButtonState.SYSTEM
                        queuedPackageNames.contains(pkg.packageName) -> PackageButtonState.QUEUED
                        else -> PackageButtonState.MANAGE
                    }
                    PackageCard(
                        packageItem = pkg,
                        buttonState = buttonState,
                        onClick = { onPackageClick(pkg) },
                        onActionClick = { onQueueAction(pkg) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InstalledTopHeader(
    totalCount: Int,
    updateCount: Int,
    onBatchUpdate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.tab_installed),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = OvlTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(id = R.string.module_installed_count, totalCount),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = OvlTheme.colors.textSecondary
            )
        }

        AnimatedVisibility(visible = updateCount > 0) {
            val interaction = remember { MutableInteractionSource() }
            val isPressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 180f),
                label = "updateBtnScale"
            )

            Button(
                onClick = {
                    com.orionisli.ovl0ad3r.HapticUtil.click()
                    onBatchUpdate()
                },
                modifier = Modifier.scale(scale),
                interactionSource = interaction,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OvlTheme.colors.primary,
                    contentColor = OvlTheme.colors.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(id = R.string.installed_update_all, updateCount),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SegmentedControl(
    selectedFilter: PackageFilter,
    onFilterSelected: (PackageFilter) -> Unit
) {
    val filters = listOf(
        PackageFilter.ALL to stringResource(R.string.installed_filter_all),
        PackageFilter.USER to stringResource(R.string.installed_filter_user),
        PackageFilter.SYSTEM to stringResource(R.string.installed_filter_system)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(CircleShape)
            .background(OvlTheme.colors.surfaceContainer)
            .border(1.dp, OvlTheme.colors.outlineVariant.copy(alpha = 0.4f), CircleShape)
            .padding(4.dp)
    ) {
        filters.forEach { (filter, label) ->
            val isSelected = filter == selectedFilter
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) OvlTheme.colors.surface else androidx.compose.ui.graphics.Color.Transparent,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 160f),
                label = "filterBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) OvlTheme.colors.primary else OvlTheme.colors.textSecondary,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 160f),
                label = "filterText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onFilterSelected(filter) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = textColor
                )
            }
        }
    }
}
