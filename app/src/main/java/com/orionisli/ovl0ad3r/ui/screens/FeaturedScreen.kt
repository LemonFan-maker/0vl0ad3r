package com.orionisli.ovl0ad3r.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.data.model.PackageItem
import com.orionisli.ovl0ad3r.ui.components.PackageButtonState
import com.orionisli.ovl0ad3r.ui.components.PackageCard
import com.orionisli.ovl0ad3r.ui.components.SileoPillActionButton
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme

data class FeaturedHeroItem(
    val title: String,
    val subtitle: String,
    val tag: String,
    val packageItem: PackageItem,
    val gradientColors: List<Color>
)

private val HERO_GRADIENTS = listOf(
    listOf(Color(0xFF1E3C72), Color(0xFF2A5298)),
    listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
    listOf(Color(0xFFD4145A), Color(0xFFFBB03B))
)

private fun heroItemsOf(packages: List<PackageItem>): List<FeaturedHeroItem> =
    packages.take(HERO_GRADIENTS.size).mapIndexed { index, pkg ->
        FeaturedHeroItem(
            title = pkg.name,
            subtitle = pkg.description,
            tag = pkg.section.ifBlank { pkg.category }.ifBlank { pkg.architecture },
            packageItem = pkg,
            gradientColors = HERO_GRADIENTS[index]
        )
    }

private fun categoriesOf(packages: List<PackageItem>): List<String> =
    packages.map { it.section }.filter { it.isNotBlank() }.distinct().sorted()

@Composable
fun FeaturedScreen(
    packages: List<PackageItem>,
    onPackageClick: (PackageItem) -> Unit,
    onQueueAction: (PackageItem) -> Unit,
    queuedPackageNames: Set<String>,
    installedPackageNames: Set<String>,
    modifier: Modifier = Modifier
) {
    val heroItems = remember(packages) { heroItemsOf(packages) }
    val categories = remember(packages) { categoriesOf(packages) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val effectiveCategory = selectedCategory?.takeIf { it in categories }
    val visiblePackages = remember(packages, effectiveCategory) {
        if (effectiveCategory == null) packages else packages.filter { it.section == effectiveCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OvlTheme.colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            FeaturedHeader()
        }

        if (packages.isEmpty()) {
            item {
                FeaturedEmptyState()
            }
            return@LazyColumn
        }

        if (heroItems.isNotEmpty()) {
            item {
                HeroCarousel(
                    heroItems = heroItems,
                    queuedPackageNames = queuedPackageNames,
                    installedPackageNames = installedPackageNames,
                    onPackageClick = onPackageClick,
                    onQueueAction = onQueueAction
                )
            }
        }

        if (categories.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                CategoriesSection(
                    categories = categories,
                    selectedCategory = effectiveCategory,
                    onSelect = { category ->
                        selectedCategory = if (effectiveCategory == category) null else category
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(title = stringResource(R.string.featured_section_toolchains))
        }

        items(visiblePackages, key = { it.id }) { pkg ->
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

@Composable
private fun FeaturedEmptyState() {
    Text(
        text = stringResource(R.string.featured_empty_catalog),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
        color = OvlTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp)
    )
}

@Composable
private fun FeaturedHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.tab_featured),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
            ),
            color = OvlTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(id = R.string.featured_subtitle),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = OvlTheme.colors.textSecondary
        )
    }
}

@Composable
private fun HeroCarousel(
    heroItems: List<FeaturedHeroItem>,
    queuedPackageNames: Set<String>,
    installedPackageNames: Set<String>,
    onPackageClick: (PackageItem) -> Unit,
    onQueueAction: (PackageItem) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { heroItems.size })

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp
        ) { page ->
            val item = heroItems[page]
            HeroCard(
                item = item,
                isQueued = queuedPackageNames.contains(item.packageItem.packageName),
                isInstalled = installedPackageNames.contains(item.packageItem.packageName),
                onClick = { onPackageClick(item.packageItem) },
                onActionClick = { onQueueAction(item.packageItem) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(heroItems.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = if (isSelected) 16.dp else 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) OvlTheme.colors.primary
                            else OvlTheme.colors.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    item: FeaturedHeroItem,
    isQueued: Boolean,
    isInstalled: Boolean,
    onClick: () -> Unit,
    onActionClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val cardScale = if (isPressed) 0.98f else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .scale(cardScale)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(item.gradientColors))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    com.orionisli.ovl0ad3r.HapticUtil.tick()
                    onClick()
                }
            )
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.tag,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = Color.White
                )
            }

            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.packageItem.version,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = Color.White.copy(alpha = 0.75f)
                )

                val buttonState = when {
                    item.packageItem.isSystemPackage -> PackageButtonState.SYSTEM
                    isQueued -> PackageButtonState.QUEUED
                    isInstalled -> PackageButtonState.MANAGE
                    else -> PackageButtonState.GET
                }
                SileoPillActionButton(
                    state = buttonState,
                    onClick = onActionClick
                )
            }
        }
    }
}

@Composable
private fun CategoriesSection(
    categories: List<String>,
    selectedCategory: String?,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            val isSelected = cat == selectedCategory
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) OvlTheme.colors.primary
                        else OvlTheme.colors.surfaceContainer
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) OvlTheme.colors.primary
                        else OvlTheme.colors.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        com.orionisli.ovl0ad3r.HapticUtil.tick()
                        onSelect(cat)
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = cat,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = if (isSelected) OvlTheme.colors.onPrimary else OvlTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        ),
        color = OvlTheme.colors.textPrimary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}
