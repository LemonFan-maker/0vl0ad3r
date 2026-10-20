package com.orionisli.ovl0ad3r.data

import androidx.compose.ui.graphics.Color
import com.orionisli.ovl0ad3r.data.engine.AnsiParser
import com.orionisli.ovl0ad3r.data.engine.ExecutionState
import com.orionisli.ovl0ad3r.data.engine.PacmanEngine
import com.orionisli.ovl0ad3r.data.model.PackageFilter
import com.orionisli.ovl0ad3r.data.model.PackageItem
import com.orionisli.ovl0ad3r.data.model.QueueAction
import com.orionisli.ovl0ad3r.data.model.QueueState
import com.orionisli.ovl0ad3r.data.model.QueueTask
import com.orionisli.ovl0ad3r.data.model.ReloadLevel
import com.orionisli.ovl0ad3r.data.model.RepoSource
import com.orionisli.ovl0ad3r.data.repo.RepoPackage
import com.orionisli.ovl0ad3r.data.repo.RepoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DataLayerSmokeTest {

    @Test
    fun testAnsiParserColorsAndStyles() {
        val coloredInput = "\u001B[31mError\u001B[0m: \u001B[1;32mSuccess\u001B[0m \u001B[94mInfo\u001B[0m"
        val stripped = AnsiParser.stripAnsi(coloredInput)
        assertEquals("Error: Success Info", stripped)

        val annotated = AnsiParser.parse(coloredInput)
        assertEquals("Error: Success Info", annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())

        val errorSpan = annotated.spanStyles.firstOrNull { it.item.color == Color(0xFFE06C75) }
        assertNotNull(errorSpan)
    }

    @Test
    fun testPacmanEngineCommandBuilding() {
        val engine = PacmanEngine.getInstance()
        val installCmd = engine.buildInstallCommand(listOf("python3", "clang"))
        assertTrue(installCmd.contains("/system/0vl0ad3r/bin/pacman"))
        assertTrue(installCmd.contains("--config /system/0vl0ad3r/etc/pacman.conf"))
        assertTrue(installCmd.contains("--root /system/0vl0ad3r"))
        // 首次安装要顺带收编自举底座预先落盘的未归属文件。
        assertTrue(installCmd.contains("--overwrite 'bin/*'"))
        assertTrue(installCmd.contains("--overwrite 'lib64/*'"))
        assertTrue(installCmd.contains("--needed"))
        assertTrue(installCmd.contains("python3 clang"))

        val removeCmd = engine.buildRemoveCommand(listOf("python3"))
        assertTrue(removeCmd.contains("-R --noconfirm python3"))

        val upgradeCmd = engine.buildUpgradeCommand()
        assertTrue(upgradeCmd.contains("-Syu --noconfirm"))

        val syncCmd = engine.buildSyncCommand()
        assertTrue(syncCmd.contains("-Sy --noconfirm"))
    }

    @Test
    fun testQueueCommandsUsePacmanNameAndSyncFirst() {
        val engine = PacmanEngine.getInstance()
        // 界面 id 带版本号, pacman 只认包名, 两者必须分开。
        val repoPkg = PackageItem(
            id = "nano-9.2-1",
            name = "nano",
            version = "9.2-1",
            packageName = "nano"
        )
        val installCommands = engine.buildQueueCommands(
            listOf(QueueTask(packageItem = repoPkg, action = QueueAction.INSTALL))
        )
        assertEquals(2, installCommands.size)
        assertTrue(installCommands[0].contains("-Sy --noconfirm"))
        assertTrue(installCommands[1].contains("nano"))
        assertTrue(installCommands[1].contains("base-layout"))
        assertFalse(installCommands[1].contains("nano-9.2-1"))

        // 纯卸载不需要动同步数据库。
        val removeCommands = engine.buildQueueCommands(
            listOf(QueueTask(packageItem = repoPkg, action = QueueAction.REMOVE))
        )
        assertEquals(1, removeCommands.size)
        assertTrue(removeCommands[0].contains("-R --noconfirm nano"))
    }

    @Test
    fun testMirrorlistSyncKeepsGuiAndTerminalOnSameServers() {
        val engine = PacmanEngine.getInstance()
        val mirror = engine.buildMirrorlistCommand(
            listOf("https://r3p0.orionisli.com", "https://github.com/LemonFan-maker/P4ck4g3s/releases/download/v0.1.0")
        )
        assertNotNull(mirror)
        assertTrue(mirror!!.contains("Server = https://r3p0.orionisli.com"))
        assertTrue(mirror.contains("releases/download/v0.1.0"))
        assertTrue(mirror.contains("etc/pacman.d/mirrorlist"))

        // 明文源不进 mirrorlist, 也不允许混进去执行。
        assertNull(engine.buildMirrorlistCommand(listOf("http://evil.example")))

        val commands = engine.buildQueueCommands(
            listOf(QueueTask(packageItem = PackageItem(id = "nano-9.2-1", name = "nano", version = "9.2-1", packageName = "nano"), action = QueueAction.INSTALL)),
            mirrorUrls = listOf("https://r3p0.orionisli.com")
        )
        assertEquals(3, commands.size)
        assertTrue(commands[0].contains("mirrorlist"))
        assertTrue(commands[1].contains("-Sy --noconfirm"))
        assertTrue(commands[2].contains("nano"))
    }

    @Test
    fun testQueueStateOperations() {
        val pkg1 = PackageItem(
            id = "python3",
            name = "Python3",
            version = "3.12.0",
            sizeBytes = 25000000L,
            installedSizeBytes = 95000000L,
            reloadLevel = ReloadLevel.NONE
        )
        val pkg2 = PackageItem(
            id = "system-tweak",
            name = "SystemTweak",
            version = "1.0.0",
            sizeBytes = 1024000L,
            installedSizeBytes = 2048000L,
            reloadLevel = ReloadLevel.ZYGOTE
        )

        val tasks = listOf(
            QueueTask(packageItem = pkg1, action = QueueAction.INSTALL),
            QueueTask(packageItem = pkg2, action = QueueAction.UPGRADE)
        )

        val state = QueueState(
            tasks = tasks,
            totalDownloadBytes = 26024000L,
            isExecuting = false
        )

        assertEquals(2, state.count)
        assertFalse(state.isEmpty)
        assertEquals(1, state.installTasks.size)
        assertEquals(1, state.upgradeTasks.size)
        assertEquals(0, state.removeTasks.size)
    }

    @Test
    fun testReloadLevelAndRepoModel() {
        assertEquals(ReloadLevel.ZYGOTE, ReloadLevel.fromString("zygote"))
        assertEquals(ReloadLevel.UICACHE, ReloadLevel.fromString("uicache"))
        assertEquals(ReloadLevel.REBOOT, ReloadLevel.fromString("reboot"))
        assertEquals(ReloadLevel.NONE, ReloadLevel.fromString("unknown"))

        val repo = RepoSource(
            id = "core",
            name = "Core Repository",
            url = "https://raw.githubusercontent.com/orionisli/0vl0ad3r-packages/main/core",
            isOfficial = true,
            isEnabled = true,
            packageCount = 42,
            lastSyncTime = 1700000000L
        )
        assertEquals("core", repo.id)
        assertTrue(repo.isOfficial)
        assertEquals(42, repo.packageCount)
    }

    @Test
    fun testLocalInstallAndQueryCommands() {
        val engine = PacmanEngine.getInstance()
        val archive = "/data/user/0/com.orionisli.ovl0ad3r/cache/packages/nano-9.2-1-aarch64.pkg.tar.zst"
        val local = engine.buildLocalInstallCommand(listOf(archive))
        // GUI 的本地事务: 只吃已经下载校验过的归档, 不再依赖 sync 数据库。
        assertTrue(local.contains("-U --noconfirm"))
        assertFalse(local.contains("-Sy"))
        assertTrue(local.contains("--overwrite 'bin/*'"))
        assertTrue(local.contains(archive))

        val query = engine.buildQueryCommand()
        assertTrue(query.contains("/system/0vl0ad3r/bin/pacman"))
        assertTrue(query.contains(" -Q"))
        // 直接跑 pacman 也要带上私有 lib64, 否则动态链接器找不到 libalpm。
        assertTrue(query.contains("LD_LIBRARY_PATH=\"/system/0vl0ad3r/lib64"))
    }

    @Test
    fun testStagePackagesCopiesToRootVisibleDirectory() {
        val engine = PacmanEngine.getInstance()
        val staging = "/data/local/tmp/0vl0ad3r/packages-test"
        val archive = File("/data/user/0/com.orionisli.ovl0ad3r/cache/packages/nano-9.2-1-aarch64.pkg.tar.zst")
        val command = engine.buildStagePackagesCommand(listOf(archive), staging)

        val stagingBase = "/data/local/tmp/0vl0ad3r"
        assertTrue(command.startsWith("mkdir -p '$stagingBase' && chmod 700 '$stagingBase'"))
        assertTrue(command.contains("mkdir -p '$staging'"))
        assertTrue(command.contains("chmod 700 '$staging'"))
        assertTrue(command.contains("cp -f '${archive.absolutePath}' '$staging/${archive.name}'"))
        assertTrue(command.contains("test -s '$staging/${archive.name}'"))
        assertTrue(command.contains("chmod 644 '$staging/${archive.name}'"))
        // 复制和校验必须用 && 串联, 任何一步失败都不能继续交给 pacman -U。
        assertFalse(command.contains(";"))
    }

    @Test
    fun testSha256HexKnownVector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            RepoRepository.sha256Hex("abc".toByteArray())
        )
        assertEquals(64, RepoRepository.sha256Hex("0vl0ad3r".toByteArray()).length)
    }

    @Test
    fun testRepoQueueFailsFastWithoutCatalogOrSource() = runBlocking {
        val engine = PacmanEngine.getInstance()
        val cacheDir = File(System.getProperty("java.io.tmpdir"), "0vl0ad3r-test-cache")
        val task = QueueTask(
            packageItem = PackageItem(
                id = "nano-9.2-1",
                name = "nano",
                version = "9.2-1",
                packageName = "nano"
            ),
            action = QueueAction.INSTALL
        )

        // 源目录为空: 目标与 base-layout 都找不到, 必须在下载之前失败。
        val noCatalog = engine.executeRepoQueue(
            tasks = listOf(task),
            catalog = emptyList(),
            installed = emptySet(),
            sourceBase = "https://example.invalid",
            cacheDir = cacheDir
        )
        assertTrue(noCatalog is ExecutionState.Failed)

        val repoPkg = RepoPackage(
            name = "nano",
            version = "9.2-1",
            description = "",
            filename = "nano-9.2-1-aarch64.pkg.tar.zst",
            sizeBytes = 1024L,
            installedSizeBytes = 2048L,
            sha256 = "a".repeat(64),
            homepage = "",
            license = "",
            architecture = "aarch64",
            buildDateEpochSec = 0L,
            packager = "0vl0ad3r Packager <packages@0vl0ad3r.org>",
            depends = emptyList()
        )
        val layoutPkg = repoPkg.copy(
            name = "base-layout",
            filename = "base-layout-1.0.0-1-aarch64.pkg.tar.zst"
        )

        // 有目录但没有可用源基址: 同样在下载之前失败, 不能拼出空 URL。
        val noSource = engine.executeRepoQueue(
            tasks = listOf(task),
            catalog = listOf(repoPkg, layoutPkg),
            installed = emptySet(),
            sourceBase = null,
            cacheDir = cacheDir
        )
        assertTrue(noSource is ExecutionState.Failed)

        engine.resetState()
    }
}
