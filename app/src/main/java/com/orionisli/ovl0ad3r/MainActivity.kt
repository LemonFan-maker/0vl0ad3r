package com.orionisli.ovl0ad3r

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.orionisli.ovl0ad3r.data.store.SettingsStore
import com.orionisli.ovl0ad3r.ui.theme.getThemePalette
import java.io.File
import com.orionisli.ovl0ad3r.data.engine.BootstrapInstaller
import com.orionisli.ovl0ad3r.data.engine.BootstrapState
import com.orionisli.ovl0ad3r.data.engine.ExecutionState
import com.orionisli.ovl0ad3r.data.engine.InstalledPackagesReader
import com.orionisli.ovl0ad3r.data.engine.PacmanEngine
import com.orionisli.ovl0ad3r.data.db.OvlDatabaseHelper
import com.orionisli.ovl0ad3r.data.repo.RepoPackage
import com.orionisli.ovl0ad3r.data.repo.RepoEndpoints
import com.orionisli.ovl0ad3r.data.repo.RepoRepository
import com.orionisli.ovl0ad3r.data.repo.RepoIndexCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.orionisli.ovl0ad3r.ui.components.BootstrapInstallModal
import com.orionisli.ovl0ad3r.data.ipc.GhostRainBridge
import com.orionisli.ovl0ad3r.data.model.PackageItem
import com.orionisli.ovl0ad3r.data.model.QueueAction
import com.orionisli.ovl0ad3r.data.model.QueueState
import com.orionisli.ovl0ad3r.data.model.QueueTask
import com.orionisli.ovl0ad3r.data.model.ReloadLevel
import com.orionisli.ovl0ad3r.ui.components.PackageActionSheet
import com.orionisli.ovl0ad3r.ui.components.QueueBottomSheet
import com.orionisli.ovl0ad3r.ui.components.QueueFloatingDock
import com.orionisli.ovl0ad3r.ui.components.SileoTab
import com.orionisli.ovl0ad3r.ui.components.SileoTabBar
import com.orionisli.ovl0ad3r.ui.components.PageDragState
import com.orionisli.ovl0ad3r.ui.components.TerminalConsoleModal
import com.orionisli.ovl0ad3r.ui.components.LocalToastState
import com.orionisli.ovl0ad3r.ui.components.OvlToastHost
import com.orionisli.ovl0ad3r.ui.components.ToastType
import com.orionisli.ovl0ad3r.ui.components.rememberToastState
import com.orionisli.ovl0ad3r.ui.screens.AboutScreen
import com.orionisli.ovl0ad3r.ui.screens.DefaultSources
import com.orionisli.ovl0ad3r.ui.screens.FeaturedScreen
import com.orionisli.ovl0ad3r.ui.screens.InstalledScreen
import com.orionisli.ovl0ad3r.ui.screens.PackageDetailScreen
import com.orionisli.ovl0ad3r.ui.screens.SearchScreen
import com.orionisli.ovl0ad3r.ui.screens.SettingsScreen
import com.orionisli.ovl0ad3r.ui.screens.SourcesScreen
import com.orionisli.ovl0ad3r.ui.theme.Ovl0ad3rTheme
import com.orionisli.ovl0ad3r.ui.theme.OvlTheme
import com.orionisli.ovl0ad3r.ui.theme.OvlThemeType
import com.orionisli.ovl0ad3r.ui.theme.TerminalThemeType
import com.orionisli.ovl0ad3r.ui.theme.getTerminalThemePalette
import com.orionisli.ovl0ad3r.ui.theme.getThemePalette
import com.orionisli.ovl0ad3r.data.locale.LanguageController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val bridge = GhostRainBridge.getInstance()
    private val engine = PacmanEngine.getInstance()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageController.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bridge.bind(this)
        HapticUtil.init(this)
        LanguageController.applyDefault(this)
        engine.messageResolver = { resId, args ->
            if (args.isEmpty()) getString(resId) else getString(resId, *args)
        }

        val settingsStore = SettingsStore.getInstance(this)

        setContent {
            var currentTheme by remember { mutableStateOf(settingsStore.uiTheme) }
            var currentTerminalTheme by remember { mutableStateOf(settingsStore.terminalTheme) }
            var terminalBgCustom by remember { mutableStateOf(settingsStore.terminalBgCustom) }
            var terminalFgCustom by remember { mutableStateOf(settingsStore.terminalFgCustom) }
            var currentLanguage by remember { mutableStateOf(LanguageController.current(this@MainActivity)) }
            var hapticStrength by remember { mutableIntStateOf(settingsStore.hapticStrength) }

            val currentPalette = getThemePalette(currentTheme)
            val currentTerminalPalette = remember(currentTerminalTheme, terminalBgCustom, terminalFgCustom) {
                getTerminalThemePalette(currentTerminalTheme, terminalBgCustom, terminalFgCustom)
            }
            val insetsController = remember(window) {
                WindowCompat.getInsetsController(window, window.decorView)
            }
            SideEffect {
                insetsController.isAppearanceLightStatusBars = !currentPalette.isDark
                insetsController.isAppearanceLightNavigationBars = !currentPalette.isDark
            }

            Ovl0ad3rTheme(
                themeType = currentTheme,
                terminalThemeType = currentTerminalTheme,
                customTerminalPalette = currentTerminalPalette
            ) {
                MainAppScaffold(
                    bridge = bridge,
                    engine = engine,
                    currentTheme = currentTheme,
                    onThemeChange = {
                        currentTheme = it
                        settingsStore.uiTheme = it
                    },
                    currentTerminalTheme = currentTerminalTheme,
                    onTerminalThemeChange = {
                        currentTerminalTheme = it
                        settingsStore.terminalTheme = it
                    },
                    terminalBgCustom = terminalBgCustom,
                    terminalFgCustom = terminalFgCustom,
                    onCustomTerminalSave = { bg, fg ->
                        terminalBgCustom = bg
                        terminalFgCustom = fg
                        settingsStore.terminalBgCustom = bg
                        settingsStore.terminalFgCustom = fg
                        currentTerminalTheme = TerminalThemeType.Custom
                        settingsStore.terminalTheme = TerminalThemeType.Custom
                    },
                    currentLanguage = currentLanguage,
                    onLanguageChange = { code -> applyLanguage(code) { currentLanguage = it } },
                    hapticStrength = hapticStrength,
                    onHapticStrengthChange = {
                        hapticStrength = it
                        settingsStore.hapticStrength = it
                    }
                )
            }
        }
    }

    private fun applyLanguage(code: String, onApplied: (String) -> Unit) {
        if (code == LanguageController.current(this)) return
        LanguageController.apply(this, code)
        onApplied(code)
        if (!LanguageController.autoRestartsOnApply) {
            recreate()
        }
    }

    override fun onResume() {
        super.onResume()
        bridge.bind(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        bridge.unbind(this)
    }
}

private fun Modifier.stackedPage(
    visible: Boolean,
    index: Int,
    position: () -> Float
): Modifier = this.layout { measurable, constraints ->
    if (!visible) {
        layout(0, 0) {}
    } else {
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}.graphicsLayer {
    translationX = (index - position()) * size.width
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    bridge: GhostRainBridge,
    engine: PacmanEngine,
    currentTheme: OvlThemeType = OvlThemeType.Sakura,
    onThemeChange: (OvlThemeType) -> Unit = {},
    currentTerminalTheme: TerminalThemeType = TerminalThemeType.Parchment,
    onTerminalThemeChange: (TerminalThemeType) -> Unit = {},
    terminalBgCustom: String = "#1E1E2E",
    terminalFgCustom: String = "#CDD6F4",
    onCustomTerminalSave: (String, String) -> Unit = { _, _ -> },
    currentLanguage: String = "zh-CN",
    onLanguageChange: (String) -> Unit = {},
    hapticStrength: Int = 2,
    onHapticStrengthChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val toastState = rememberToastState()
    val pageDrag = remember { PageDragState(SileoTab.entries.lastIndex, SileoTab.FEATURED.ordinal) }
    var selectedTab by remember { mutableStateOf(SileoTab.FEATURED) }
    var selectedPackageForDetail by remember { mutableStateOf<PackageItem?>(null) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showTerminalModal by remember { mutableStateOf(false) }
    var packageActionsFor by remember { mutableStateOf<PackageItem?>(null) }

    var queueState by remember { mutableStateOf(QueueState()) }
    var installedPackages by remember { mutableStateOf<List<PackageItem>>(emptyList()) }
    var pendingReloadLevel by remember { mutableStateOf(ReloadLevel.NONE) }
    val customSourceName = stringResource(R.string.sources_custom_name)
    val msgSourceMustHttps = stringResource(R.string.sources_error_https_only)

    val repoRepository = RepoRepository.INSTANCE
    val ovlDb = remember { OvlDatabaseHelper.getInstance(context) }
    val defaultSources = DefaultSources()
    var sourcesList by remember { mutableStateOf(defaultSources) }
    var sourcesLoaded by remember { mutableStateOf(false) }
    var repoPackages by remember { mutableStateOf<List<RepoPackage>>(emptyList()) }
    var catalogSourceUrl by remember { mutableStateOf<String?>(null) }
    var syncBytes by remember { mutableLongStateOf(-1L) }

    LaunchedEffect(Unit) {
        val stored = withContext(Dispatchers.IO) { ovlDb.queryRepositories() }
        if (stored.isEmpty()) {
            withContext(Dispatchers.IO) { defaultSources.forEach { ovlDb.upsertRepository(it) } }
            sourcesList = defaultSources
        } else {
            val factory = defaultSources.associateBy { it.id }
            val fixed = stored.map { s ->
                factory[s.id]?.let {
                    s.copy(name = it.name, url = it.url, isPacmanMirror = it.isPacmanMirror)
                } ?: s
            }
            val corrected = fixed.filterIndexed { i, s -> s != stored[i] }
            if (corrected.isNotEmpty()) {
                withContext(Dispatchers.IO) { corrected.forEach { ovlDb.upsertRepository(it) } }
            }
            sourcesList = fixed
        }
        sourcesLoaded = true
    }

    LaunchedEffect(Unit) {
        val cached = withContext(Dispatchers.IO) { RepoIndexCache.read(context.filesDir) }
        if (cached != null && cached.sourceUrl == RepoEndpoints.GITHUB_RELEASES) {
            repoPackages = cached.index.packages
            catalogSourceUrl = cached.sourceUrl
        }
    }

    val executionState by engine.executionState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isRootAlive by bridge.isRootAlive.collectAsState()
    val bootstrapInstaller = remember { BootstrapInstaller.getInstance() }
    val bootstrapState by bootstrapInstaller.state.collectAsState()
    val bootstrapVersion by bootstrapInstaller.version.collectAsState()
    val bootstrapLogs by bootstrapInstaller.logLines.collectAsState()
    var showBootstrapModal by remember { mutableStateOf(false) }

    LaunchedEffect(isRootAlive) {
        if (isRootAlive) {
            val status = bootstrapInstaller.checkStatus()
            if (status != BootstrapState.READY && status != BootstrapState.INSTALLING) {
                showBootstrapModal = true
            }
        }
    }

    val queuedNames = remember(queueState.tasks) {
        queueState.tasks.map { it.packageItem.packageName }.toSet()
    }
    val installedNames = remember(installedPackages) {
        installedPackages.map { it.packageName }.toSet()
    }
    val catalogPackages = remember(repoPackages, installedPackages) {
        val byName = installedPackages.associateBy { it.packageName }
        repoPackages.map { rp ->
            val cur = byName[rp.name]
            rp.toPackageItem(
                isInstalled = cur != null,
                hasUpdate = cur != null &&
                    !cur.isSystemPackage &&
                    cur.version != rp.version
            )
        }
    }
    val storePackages = remember(catalogPackages) {
        catalogPackages.filterNot { it.isSystemPackage }
    }

    suspend fun loadInstalledPackages() {
        val (exit, output) = bridge.executeRootCommandWithOutput(engine.buildQueryCommand())
        if (exit == 0) {
            val parsed = InstalledPackagesReader.parse(output, repoPackages)
            if (parsed.isNotEmpty()) {
                installedPackages = parsed
                return
            }
        }
        val direct = withContext(Dispatchers.IO) {
            try {
                File(PacmanEngine.TARGET_ROOT, "var/lib/pacman/local")
                    .list()?.joinToString("\n")
            } catch (_: Exception) {
                null
            }
        }
        if (!direct.isNullOrBlank()) {
            installedPackages = InstalledPackagesReader.parseLocalDbEntries(direct, repoPackages)
            return
        }
        val (_, dirs) = bridge.executeRootCommandWithOutput(
            "ls -1 \"${PacmanEngine.TARGET_ROOT}/var/lib/pacman/local\" 2>/dev/null"
        )
        installedPackages = InstalledPackagesReader.parseLocalDbEntries(dirs, repoPackages)
    }

    suspend fun refreshAllSources(): Int {
        syncBytes = 0L
        val results = try {
            repoRepository.syncAll(sourcesList) { syncBytes = it }
        } finally {
            syncBytes = -1L
        }
        val failed = results.count { !it.ok }
        sourcesList = sourcesList.map { src ->
            val r = results.firstOrNull { it.source.id == src.id }
            if (r?.ok == true && r.index != null) {
                src.copy(packageCount = r.index.packages.size, lastSyncTime = System.currentTimeMillis())
            } else {
                src
            }
        }
        withContext(Dispatchers.IO) { sourcesList.forEach { ovlDb.upsertRepository(it) } }
        val successful = results.filter { it.ok }
        val best = successful.firstOrNull { !it.source.isPacmanMirror }
            ?: successful.firstOrNull()
        best?.let {
            repoPackages = it.index?.packages.orEmpty()
            catalogSourceUrl = it.source.url
            it.rawIndex?.let { raw ->
                withContext(Dispatchers.IO) {
                    RepoIndexCache.write(context.filesDir, raw, it.source.url)
                }
            }
        }
        return failed
    }

    LaunchedEffect(sourcesLoaded) {
        if (sourcesLoaded) refreshAllSources()
    }

    LaunchedEffect(bootstrapState, repoPackages) {
        if (bootstrapState == BootstrapState.READY) {
            loadInstalledPackages()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == SileoTab.INSTALLED && bootstrapState == BootstrapState.READY) {
            loadInstalledPackages()
        }
    }

    BackHandler(enabled = showAboutScreen || packageActionsFor != null || selectedPackageForDetail != null || showQueueSheet || (showBootstrapModal && bootstrapState != BootstrapState.INSTALLING)) {
        if (showAboutScreen) {
            showAboutScreen = false
        } else if (packageActionsFor != null) {
            packageActionsFor = null
        } else if (selectedPackageForDetail != null) {
            selectedPackageForDetail = null
        } else if (showQueueSheet) {
            showQueueSheet = false
        } else if (showBootstrapModal) {
            showBootstrapModal = false
        }
    }

    fun publishQueue(tasks: List<QueueTask>) {
        queueState = queueState.copy(
            tasks = tasks,
            totalDownloadBytes = tasks
                .filter { it.action != QueueAction.REMOVE }
                .sumOf { it.packageItem.sizeBytes }
        )
    }

    fun enqueuePackageAction(pkg: PackageItem, action: QueueAction) {
        if (pkg.isSystemPackage) {
            HapticUtil.warning()
            toastState.show(
                msg = context.getString(R.string.toast_system_package_managed),
                toastType = ToastType.INFO,
                durationMs = 2000L,
                scope = coroutineScope
            )
            return
        }
        HapticUtil.click()
        val sameTaskQueued = queueState.tasks.any {
            it.packageItem.packageName == pkg.packageName && it.action == action
        }
        if (sameTaskQueued) {
            toastState.show(
                msg = context.getString(R.string.toast_package_queued, pkg.name),
                toastType = ToastType.INFO,
                durationMs = 1800L,
                scope = coroutineScope
            )
            return
        }
        val updated = queueState.tasks.filterNot {
            it.packageItem.packageName == pkg.packageName
        } + QueueTask(packageItem = pkg, action = action)
        publishQueue(updated)
        toastState.show(
            msg = context.getString(
                if (action == QueueAction.REMOVE) {
                    R.string.toast_package_remove_queued
                } else {
                    R.string.toast_package_queued
                },
                pkg.name
            ),
            toastType = ToastType.INFO,
            durationMs = 1800L,
            scope = coroutineScope
        )
    }

    fun dequeuePackage(pkg: PackageItem) {
        HapticUtil.click()
        val updated = queueState.tasks.filterNot {
            it.packageItem.packageName == pkg.packageName
        }
        if (updated.size == queueState.tasks.size) return
        publishQueue(updated)
        toastState.show(
            msg = context.getString(R.string.toast_package_dequeued, pkg.name),
            toastType = ToastType.INFO,
            durationMs = 1800L,
            scope = coroutineScope
        )
    }

    fun handleQueueToggle(pkg: PackageItem) {
        val existing = queueState.tasks.any {
            it.packageItem.packageName == pkg.packageName
        }
        if (existing) {
            dequeuePackage(pkg)
        } else {
            enqueuePackageAction(
                pkg,
                if (pkg.isInstalled) QueueAction.UPGRADE else QueueAction.INSTALL
            )
        }
    }

    fun handleManagePackage(pkg: PackageItem) {
        HapticUtil.tick()
        packageActionsFor = pkg
    }

    CompositionLocalProvider(LocalToastState provides toastState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OvlTheme.colors.background)
        ) {
        AnimatedContent(
            targetState = selectedPackageForDetail,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screenTransition"
        ) { detailPkg ->
            if (detailPkg != null) {
                PackageDetailScreen(
                    packageItem = detailPkg,
                    isQueued = queuedNames.contains(detailPkg.packageName),
                    isInstalled = installedNames.contains(detailPkg.packageName),
                    onBack = { selectedPackageForDetail = null },
                    onActionClick = {
                        if (detailPkg.isInstalled) {
                            handleManagePackage(detailPkg)
                        } else {
                            handleQueueToggle(detailPkg)
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    val lo = pageDrag.windowLow
                    val hi = pageDrag.windowHigh
                    SileoTab.entries.forEachIndexed { index, tab ->
                        Box(
                            modifier = Modifier.stackedPage(
                                visible = index in lo..hi,
                                index = index,
                                position = { pageDrag.position.value }
                            )
                        ) {
                            when (tab) {
                                SileoTab.FEATURED -> {
                                    FeaturedScreen(
                                        packages = storePackages,
                                        onPackageClick = { selectedPackageForDetail = it },
                                        onQueueAction = { pkg ->
                                            if (pkg.isInstalled) {
                                                handleManagePackage(pkg)
                                            } else {
                                                handleQueueToggle(pkg)
                                            }
                                        },
                                        queuedPackageNames = queuedNames,
                                        installedPackageNames = installedNames
                                    )
                                }
                                SileoTab.SOURCES -> {
                                    SourcesScreen(
                                        sources = sourcesList,
                                        syncBytes = syncBytes,
                                        onAddSource = { url ->
                                            if (!url.startsWith("https://")) {
                                                toastState.show(
                                                    msg = msgSourceMustHttps,
                                                    toastType = com.orionisli.ovl0ad3r.ui.components.ToastType.ERROR,
                                                    scope = coroutineScope
                                                )
                                            } else {
                                                val newRepo =
                                                    com.orionisli.ovl0ad3r.data.model.RepoSource(
                                                        id = "repo_${System.currentTimeMillis()}",
                                                        name = url.substringAfterLast("/")
                                                            .ifEmpty { customSourceName },
                                                        url = url,
                                                        isOfficial = false,
                                                        isEnabled = true,
                                                        packageCount = 0,
                                                        lastSyncTime = 0L
                                                    )
                                                sourcesList = sourcesList + newRepo
                                                coroutineScope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        ovlDb.upsertRepository(newRepo)
                                                    }
                                                }
                                            }
                                        },
                                        onToggleSource = { repo, isEnabled ->
                                            val updated = repo.copy(isEnabled = isEnabled)
                                            sourcesList = sourcesList.map {
                                                if (it.id == repo.id) updated else it
                                            }
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    ovlDb.upsertRepository(updated)
                                                }
                                            }
                                        },
                                        onRefreshAll = { refreshAllSources() }
                                    )
                                }
                                SileoTab.SEARCH -> {
                                    SearchScreen(
                                        packages = storePackages,
                                        onPackageClick = { selectedPackageForDetail = it },
                                        onQueueAction = { pkg ->
                                            if (pkg.isInstalled) {
                                                handleManagePackage(pkg)
                                            } else {
                                                handleQueueToggle(pkg)
                                            }
                                        },
                                        queuedPackageNames = queuedNames,
                                        installedPackageNames = installedNames
                                    )
                                }
                                SileoTab.INSTALLED -> {
                                    InstalledScreen(
                                        installedPackages = installedPackages,
                                        onPackageClick = { selectedPackageForDetail = it },
                                        onQueueAction = { pkg ->
                                            if (pkg.isInstalled) {
                                                handleManagePackage(pkg)
                                            } else {
                                                handleQueueToggle(pkg)
                                            }
                                        },
                                        onBatchUpdate = { updateList ->
                                            updateList.forEach {
                                                enqueuePackageAction(it, QueueAction.UPGRADE)
                                            }
                                            showQueueSheet = true
                                        },
                                        queuedPackageNames = queuedNames
                                    )
                                }
                                SileoTab.SETTINGS -> {
                                    SettingsScreen(
                                        currentTheme = currentTheme,
                                        onThemeChange = onThemeChange,
                                        currentTerminalTheme = currentTerminalTheme,
                                        onTerminalThemeChange = onTerminalThemeChange,
                                        terminalBgCustom = terminalBgCustom,
                                        terminalFgCustom = terminalFgCustom,
                                        onCustomTerminalSave = onCustomTerminalSave,
                                        hapticStrength = hapticStrength,
                                        onHapticStrengthChange = onHapticStrengthChange,
                                        currentLanguage = currentLanguage,
                                        onLanguageChange = onLanguageChange,
                                        bridge = bridge,
                                        onOpenBootstrapModal = { showBootstrapModal = true },
                                        onNavigateToAbout = { showAboutScreen = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showAboutScreen,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.82f, stiffness = 140f)) +
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 140f)
                    ),
            exit = fadeOut(animationSpec = spring(dampingRatio = 0.82f, stiffness = 140f)) +
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 140f)
                    )
        ) {
            AboutScreen(
                onBack = { showAboutScreen = false }
            )
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OvlToastHost(
                    toastState = toastState,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = if (selectedPackageForDetail != null || showAboutScreen) 32.dp else 28.dp
                    )
                )

                if (selectedPackageForDetail == null && !showAboutScreen) {
                    QueueFloatingDock(
                        queueState = queueState,
                        onClick = { showQueueSheet = true }
                    )
                    SileoTabBar(
                        selectedTab = selectedTab,
                        onTabSelected = { targetTab ->
                            selectedTab = targetTab
                        },
                        dragState = pageDrag,
                        dragScope = coroutineScope
                    )
                } else {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }

        if (showQueueSheet) {
            QueueBottomSheet(
                queueState = queueState,
                sheetState = sheetState,
                onDismissRequest = { showQueueSheet = false },
                onClearQueue = {
                    HapticUtil.warning()
                    queueState = QueueState()
                    toastState.show(
                        msg = context.getString(R.string.toast_queue_cleared),
                        toastType = ToastType.INFO,
                        durationMs = 1800L,
                        scope = coroutineScope
                    )
                },
                onConfirmExecute = {
                    HapticUtil.heavyClick()
                    when {
                        !isRootAlive -> {
                            HapticUtil.error()
                            toastState.show(
                                msg = context.getString(R.string.queue_root_required),
                                toastType = ToastType.ERROR,
                                scope = coroutineScope
                            )
                        }
                        bootstrapState != BootstrapState.READY -> {
                            HapticUtil.error()
                            showQueueSheet = false
                            showBootstrapModal = true
                            toastState.show(
                                msg = context.getString(R.string.queue_bootstrap_required),
                                toastType = ToastType.ERROR,
                                scope = coroutineScope
                            )
                        }
                        else -> {
                            showQueueSheet = false
                            showTerminalModal = true
                            val tasks = queueState.tasks
                            pendingReloadLevel = if (tasks.any {
                                    it.packageItem.reloadLevel == ReloadLevel.ZYGOTE
                                }
                            ) ReloadLevel.ZYGOTE else ReloadLevel.NONE
                            coroutineScope.launch {
                                loadInstalledPackages()
                                val currentInstalled = installedPackages
                                    .map { it.packageName }
                                    .toSet()
                                val enabledMirrors = sourcesList
                                    .filter { it.isEnabled && it.isPacmanMirror }
                                    .map { it.url }
                                val resultState = engine.executeRepoQueue(
                                    tasks = tasks,
                                    catalog = repoPackages,
                                    installed = currentInstalled,
                                    sourceBase = catalogSourceUrl,
                                    mirrorUrls = enabledMirrors,
                                    cacheDir = context.cacheDir
                                )
                                if (resultState is ExecutionState.Completed &&
                                    resultState.exitCode == 0
                                ) {
                                    HapticUtil.success()
                                    queueState = QueueState()
                                    loadInstalledPackages()
                                } else if (resultState is ExecutionState.Failed) {
                                    HapticUtil.error()
                                }
                            }
                        }
                    }
                },
                onRemoveTask = { task ->
                    HapticUtil.tick()
                    publishQueue(queueState.tasks.filterNot {
                        it.packageItem.packageName == task.packageItem.packageName
                    })
                }
            )
        }

        packageActionsFor?.let { pkg ->
            val queuedTask = queueState.tasks.firstOrNull {
                it.packageItem.packageName == pkg.packageName
            }
            PackageActionSheet(
                packageItem = pkg,
                queuedAction = queuedTask?.action,
                onDismissRequest = { packageActionsFor = null },
                onUpdate = { enqueuePackageAction(pkg, QueueAction.UPGRADE) },
                onUninstall = { enqueuePackageAction(pkg, QueueAction.REMOVE) },
                onDequeue = { dequeuePackage(pkg) }
            )
        }

        AnimatedVisibility(
            visible = showTerminalModal,
            enter = fadeIn(spring(dampingRatio = 0.85f, stiffness = 220f)),
            exit = fadeOut(spring(dampingRatio = 0.85f, stiffness = 220f))
        ) {
            TerminalConsoleModal(
                executionState = executionState,
                reloadLevel = pendingReloadLevel,
                onDismiss = {
                    HapticUtil.click()
                    showTerminalModal = false
                    pendingReloadLevel = ReloadLevel.NONE
                    engine.resetState()
                },
                onRestartZygote = {
                    HapticUtil.click()
                    coroutineScope.launch {
                        bridge.restartZygote()
                        showTerminalModal = false
                        pendingReloadLevel = ReloadLevel.NONE
                        engine.resetState()
                    }
                },
                onCancel = {
                    HapticUtil.warning()
                    engine.cancelExecution()
                }
            )
        }

        if (showBootstrapModal) {
            BootstrapInstallModal(
                state = bootstrapState,
                version = bootstrapVersion,
                logLines = bootstrapLogs,
                onStartInstall = {
                    coroutineScope.launch {
                        val force = bootstrapState == BootstrapState.CORRUPTED || bootstrapState == BootstrapState.READY
                        val result = bootstrapInstaller.installBootstrap(context, force = force)
                        if (result.isSuccess) {
                            toastState.show(
                                msg = context.getString(
                                    R.string.bootstrap_toast_success,
                                    bootstrapInstaller.version.value ?: "7.1.0"
                                ),
                                toastType = ToastType.SUCCESS,
                                durationMs = 2500L,
                                scope = coroutineScope
                            )
                            HapticUtil.confirm()
                        } else {
                            toastState.show(
                                msg = context.getString(
                                    R.string.bootstrap_toast_failed,
                                    result.exceptionOrNull()?.message ?: ""
                                ),
                                toastType = ToastType.WARNING,
                                durationMs = 3500L,
                                scope = coroutineScope
                            )
                        }
                    }
                },
                onDismiss = {
                    showBootstrapModal = false
                }
            )
        }
    }
}
}
