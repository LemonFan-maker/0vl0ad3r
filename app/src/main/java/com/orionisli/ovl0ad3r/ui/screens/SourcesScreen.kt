package com.orionisli.ovl0ad3r.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.data.model.RepoSource
import com.orionisli.ovl0ad3r.data.repo.RepoEndpoints
import com.orionisli.ovl0ad3r.ui.components.formatByteSize
import com.orionisli.ovl0ad3r.ui.components.PullToRefreshRing
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun DefaultSources(): List<RepoSource> = listOf(
    RepoSource(
        id = "official_pages",
        name = stringResource(id = R.string.sources_default_official),
        url = RepoEndpoints.PRIMARY,
        isOfficial = true,
        isEnabled = true
    ),
    RepoSource(
        id = "official_release_mirror",
        name = stringResource(id = R.string.sources_default_mirror),
        url = RepoEndpoints.GITHUB_RELEASES,
        isOfficial = true,
        isEnabled = true,
        isPacmanMirror = false
    )
)

@Composable
fun SourcesScreen(
    sources: List<RepoSource>? = null,
    onAddSource: (String) -> Unit = {},
    onToggleSource: (RepoSource, Boolean) -> Unit = { _, _ -> },
    onRefreshAll: suspend () -> Int = { 0 },
    syncBytes: Long = -1L,
    modifier: Modifier = Modifier
) {
    val resolvedSources = sources ?: DefaultSources()

    var showAddDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val toastState = com.orionisli.ovl0ad3r.ui.components.LocalToastState.current
    val msgSourcesRefreshed = stringResource(R.string.toast_sources_refreshed)
    val msgSourceAdded = stringResource(R.string.toast_source_added)
    val msgSyncFailed = stringResource(R.string.sources_sync_failed)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OvlTheme.colors.background)
            .statusBarsPadding()
    ) {
        SourcesTopBar(
            syncBytes = if (isRefreshing) syncBytes else -1L,
            onAddClick = { showAddDialog = true },
            onRefreshClick = {
                if (!isRefreshing) {
                    com.orionisli.ovl0ad3r.HapticUtil.click()
                    scope.launch {
                        isRefreshing = true
                        val failed = try {
                            onRefreshAll()
                        } finally {
                            isRefreshing = false
                        }
                        val attempted = resolvedSources.count { it.isEnabled }
                        val level = when {
                            failed == 0 -> com.orionisli.ovl0ad3r.ui.components.ToastType.SUCCESS
                            failed < attempted ->
                                com.orionisli.ovl0ad3r.ui.components.ToastType.WARNING
                            else -> com.orionisli.ovl0ad3r.ui.components.ToastType.ERROR
                        }
                        toastState.show(
                            msg = if (failed == 0) msgSourcesRefreshed
                            else msgSyncFailed.format(failed),
                            toastType = level,
                            scope = scope
                        )
                        if (failed == 0) com.orionisli.ovl0ad3r.HapticUtil.confirm()
                    }
                }
            }
        )

        AnimatedVisibility(visible = isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                PullToRefreshRing(
                    progress = 1f,
                    isRefreshing = true,
                    size = 28.dp,
                    color = OvlTheme.colors.primary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = resolvedSources,
                key = { it.id },
                contentType = { "source" }
            ) { repo ->
                RepoCard(
                    repo = repo,
                    onToggle = { isEnabled -> onToggleSource(repo, isEnabled) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddSourceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { url ->
                onAddSource(url)
                showAddDialog = false
                toastState.show(
                    msg = msgSourceAdded,
                    toastType = com.orionisli.ovl0ad3r.ui.components.ToastType.SUCCESS,
                    scope = scope
                )
                com.orionisli.ovl0ad3r.HapticUtil.confirm()
            }
        )
    }
}

@Composable
private fun SourcesTopBar(
    onAddClick: () -> Unit,
    onRefreshClick: () -> Unit,
    syncBytes: Long
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
                text = stringResource(id = R.string.tab_sources),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = OvlTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(id = R.string.sources_subtitle),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = OvlTheme.colors.textSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (syncBytes >= 0L) {
                Text(
                    text = stringResource(
                        R.string.sources_sync_progress,
                        formatByteSize(syncBytes)
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = OvlTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = {
                com.orionisli.ovl0ad3r.HapticUtil.click()
                onRefreshClick()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = stringResource(id = R.string.action_refresh),
                    tint = OvlTheme.colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = {
                com.orionisli.ovl0ad3r.HapticUtil.click()
                onAddClick()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = stringResource(id = R.string.action_add_source),
                    tint = OvlTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun RepoCard(
    repo: RepoSource,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OvlTheme.colors.surface)
            .border(
                width = 1.dp,
                color = OvlTheme.colors.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (repo.isOfficial) OvlTheme.colors.primary.copy(alpha = 0.12f)
                    else OvlTheme.colors.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (repo.isOfficial) R.drawable.ic_shield else R.drawable.ic_tab_sources
                ),
                contentDescription = null,
                tint = if (repo.isOfficial) OvlTheme.colors.primary else OvlTheme.colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = OvlTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (repo.isOfficial) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(OvlTheme.colors.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.sources_official_badge),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = OvlTheme.colors.primary
                        )
                    }
                }
                if (!repo.isPacmanMirror) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(OvlTheme.colors.warning.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.sources_index_only_badge),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = OvlTheme.colors.warning
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = repo.url,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = OvlTheme.colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (repo.lastSyncTime <= 0L) {
                    stringResource(R.string.sources_never_synced)
                } else {
                    stringResource(R.string.sources_sync_summary, repo.packageCount,
                        SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(repo.lastSyncTime)))
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = OvlTheme.colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = repo.isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OvlTheme.colors.onPrimary,
                checkedTrackColor = OvlTheme.colors.primary,
                uncheckedThumbColor = OvlTheme.colors.textTertiary,
                uncheckedTrackColor = OvlTheme.colors.surfaceVariant
            )
        )
    }
}

@Composable
private fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    val context = LocalContext.current
    var inputUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0)?.text?.toString().orEmpty().trim()
            if (text.startsWith("http://") || text.startsWith("https://")) {
                inputUrl = text
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.action_add_source),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OvlTheme.colors.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.sources_add_dialog_hint),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = OvlTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    placeholder = {
                        Text(
                            text = "https://example.com/repo",
                            style = MaterialTheme.typography.bodySmall,
                            color = OvlTheme.colors.textTertiary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OvlTheme.colors.primary,
                        unfocusedBorderColor = OvlTheme.colors.outlineVariant,
                        focusedTextColor = OvlTheme.colors.textPrimary,
                        unfocusedTextColor = OvlTheme.colors.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    com.orionisli.ovl0ad3r.HapticUtil.confirm()
                    if (inputUrl.isNotBlank()) {
                        onAdd(inputUrl.trim())
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OvlTheme.colors.primary,
                    contentColor = OvlTheme.colors.onPrimary
                )
            ) {
                Text(
                    text = stringResource(id = R.string.action_install),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                com.orionisli.ovl0ad3r.HapticUtil.tick()
                onDismiss()
            }) {
                Text(
                    text = stringResource(id = R.string.action_cancel),
                    style = MaterialTheme.typography.labelMedium,
                    color = OvlTheme.colors.textSecondary
                )
            }
        },
        containerColor = OvlTheme.colors.surface,
        shape = RoundedCornerShape(20.dp)
    )
}
