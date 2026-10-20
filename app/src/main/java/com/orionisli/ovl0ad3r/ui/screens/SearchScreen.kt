package com.orionisli.ovl0ad3r.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.data.model.PackageItem
import com.orionisli.ovl0ad3r.ui.components.PackageButtonState
import com.orionisli.ovl0ad3r.ui.components.PackageCard
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

@Composable
fun SearchScreen(
    packages: List<PackageItem>,
    onPackageClick: (PackageItem) -> Unit,
    onQueueAction: (PackageItem) -> Unit,
    queuedPackageNames: Set<String>,
    installedPackageNames: Set<String>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val allCategory = stringResource(R.string.search_category_all)
    val categoryList = remember(packages, allCategory) {
        listOf(allCategory) +
            packages.map { it.section }.filter { it.isNotBlank() }.distinct().sorted()
    }
    var selectedCategory by remember(allCategory) { mutableStateOf(allCategory) }
    val effectiveCategory = if (categoryList.contains(selectedCategory)) selectedCategory else allCategory

    val filteredPackages = remember(searchQuery, effectiveCategory, packages) {
        packages.filter { pkg ->
            val matchesCategory = effectiveCategory == allCategory ||
                pkg.category.equals(effectiveCategory, ignoreCase = true) ||
                pkg.section.equals(effectiveCategory, ignoreCase = true)

            val query = searchQuery.trim().lowercase()
            val matchesQuery = query.isEmpty() ||
                pkg.name.lowercase().contains(query) ||
                pkg.id.lowercase().contains(query) ||
                pkg.description.lowercase().contains(query) ||
                pkg.author.lowercase().contains(query)

            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OvlTheme.colors.background)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.search_hint),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = OvlTheme.colors.textTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tab_search),
                        contentDescription = null,
                        tint = OvlTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            com.orionisli.ovl0ad3r.HapticUtil.tick()
                            searchQuery = ""
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = null,
                                tint = OvlTheme.colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OvlTheme.colors.surface,
                    unfocusedContainerColor = OvlTheme.colors.surface,
                    focusedBorderColor = OvlTheme.colors.primary,
                    unfocusedBorderColor = OvlTheme.colors.outlineVariant.copy(alpha = 0.6f),
                    focusedTextColor = OvlTheme.colors.textPrimary,
                    unfocusedTextColor = OvlTheme.colors.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoryList) { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isSelected) OvlTheme.colors.primary
                            else OvlTheme.colors.surfaceContainer
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) OvlTheme.colors.primary
                            else OvlTheme.colors.outlineVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable {
                            com.orionisli.ovl0ad3r.HapticUtil.tick()
                            selectedCategory = cat
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) OvlTheme.colors.onPrimary else OvlTheme.colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredPackages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tab_search),
                        contentDescription = null,
                        tint = OvlTheme.colors.textTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.search_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OvlTheme.colors.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(
                    items = filteredPackages,
                    key = { it.id },
                    contentType = { "package" }
                ) { pkg ->
                    val buttonState = when {
                        pkg.isSystemPackage -> PackageButtonState.SYSTEM
                        queuedPackageNames.contains(pkg.packageName) -> PackageButtonState.QUEUED
                        installedPackageNames.contains(pkg.packageName) -> PackageButtonState.MANAGE
                        else -> PackageButtonState.GET
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
