package com.orionisli.ovl0ad3r.data.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class BootstrapState {
    NOT_INSTALLED,
    CORRUPTED,
    READY,
    INSTALLING
}

class BootstrapInstaller private constructor() {

    private val _state = MutableStateFlow(BootstrapState.NOT_INSTALLED)
    val state: StateFlow<BootstrapState> = _state.asStateFlow()

    private val _version = MutableStateFlow<String?>(null)
    val version: StateFlow<String?> = _version.asStateFlow()

    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    val logLines: StateFlow<List<String>> = _logLines.asStateFlow()

    val isReady: Boolean
        get() = _state.value == BootstrapState.READY

    val isInstalling: Boolean
        get() = _state.value == BootstrapState.INSTALLING

    fun appendLog(line: String) {
        _logLines.value = _logLines.value + line
    }

    fun clearLogs() {
        _logLines.value = emptyList()
    }

    suspend fun checkStatus(): BootstrapState = withContext(Dispatchers.IO) {
        val su = PacmanEngine.getInstance().getSuBinaryPath()

        val script = buildString {
            append("ALL_EXIST=1; ")
            for (f in CRITICAL_FILES) {
                append("[ -s \"$TARGET_ROOT/$f\" ] || ALL_EXIST=0; ")
            }
            append("echo \$ALL_EXIST")
        }

        val (_, out) = runRootCommand(su, script)
        val allFilesOk = out.trim() == "1"

        if (!allFilesOk) {
            val (_, existsCheck) = runRootCommand(su, "[ -d \"$TARGET_ROOT\" ] && ls -A \"$TARGET_ROOT\" | wc -l || echo 0")
            val count = existsCheck.trim().toIntOrNull() ?: 0
            val newState = if (count > 0) BootstrapState.CORRUPTED else BootstrapState.NOT_INSTALLED
            _state.value = newState
            _version.value = null
            return@withContext newState
        }

        repairPacmanConfigIfNeeded(su)

        val verCmd = "export LD_LIBRARY_PATH=\"$TARGET_ROOT/lib64:\$LD_LIBRARY_PATH\"; $PACMAN_BIN --version"
        val (verExit, verOut) = runRootCommand(su, verCmd)
        if (verExit == 0 && verOut.contains("Pacman v")) {
            val match = Regex("Pacman\\s+v([0-9.]+)").find(verOut)
            _version.value = match?.groupValues?.get(1) ?: "7.1.0"
            _state.value = BootstrapState.READY
            BootstrapState.READY
        } else {
            _state.value = BootstrapState.CORRUPTED
            _version.value = null
            BootstrapState.CORRUPTED
        }
    }

    private fun repairPacmanConfigIfNeeded(su: String) {
        val grepCmd = "grep -nE '^[[:space:]]*(SigLevel|LocalFileSigLevel)[[:space:]]*=.*(Optional|Required)' " +
            "\"$PACMAN_CONF\" 2>/dev/null || true"
        val (_, found) = runRootCommand(su, grepCmd)
        if (found.isBlank()) return

        appendLog("[*] 检测到旧底座的无签名兼容配置, 正在修正 pacman.conf...")
        val fixCmd = "sed -i 's/^[[:space:]]*SigLevel[[:space:]]*=.*/SigLevel = Never/' \"$PACMAN_CONF\" && " +
            "sed -i 's/^[[:space:]]*LocalFileSigLevel[[:space:]]*=.*/LocalFileSigLevel = Never/' \"$PACMAN_CONF\" && " +
            "echo FIXED"
        val (exit, out) = runRootCommand(su, fixCmd)
        if (exit == 0 && out.contains("FIXED")) {
            appendLog("[+] pacman.conf 已修正: SigLevel/LocalFileSigLevel = Never")
        } else {
            appendLog("[-] pacman.conf 自动修正失败, 请重新释放底座")
        }
    }

    suspend fun clearGhostFiles(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val su = PacmanEngine.getInstance().getSuBinaryPath()
        val cleanScript = buildString {
            append("REMOVED=0; ")
            // 1. 清理 db.lck
            append("if [ -f \"$PACMAN_LOCK\" ]; then rm -f \"$PACMAN_LOCK\"; REMOVED=1; fi; ")
            // 2. 清理临时下载文件
            append("rm -f \"$TARGET_ROOT/var/cache/pacman/pkg\"/*.part 2>/dev/null; ")
            append("echo \$REMOVED")
        }
        val (code, out) = runRootCommand(su, cleanScript)
        val unlocked = out.trim() == "1"
        Pair(code == 0, if (unlocked) "已清除数据库死锁(db.lck)与临时缓存" else "未发现死锁，缓存已清理")
    }

    suspend fun uninstallBootstrap(context: Context): Result<String> = withContext(Dispatchers.IO) {
        if (_state.value == BootstrapState.INSTALLING) {
            return@withContext Result.failure(IllegalStateException("底座正在部署中，请稍候操作"))
        }

        val su = PacmanEngine.getInstance().getSuBinaryPath()
        val (rootExit, rootOut) = runRootCommand(su, "id")
        if (rootExit != 0 || !rootOut.contains("uid=0")) {
            return@withContext Result.failure(IllegalStateException("未检测到有效Root权限(uid=0)"))
        }

        val fallbackTmp = File(context.cacheDir, "bootstrap_whiteout.tmp")
        val (_, checkOut) = runRootCommand(su, "[ -f \"$TARGET_ROOT/whiteout\" ] && echo YES || echo NO")

        val script = buildString {
            append("WHITEOUT=\"$TARGET_ROOT/whiteout\"; ")
            if (checkOut.trim() != "YES") {
                try {
                    context.assets.open("bootstrap/whiteout").use { input ->
                        FileOutputStream(fallbackTmp).use { output -> input.copyTo(output) }
                    }
                    fallbackTmp.setReadable(true, false)
                    append("WHITEOUT=\"${fallbackTmp.absolutePath}\"; ")
                } catch (_: Exception) {
                    append("WHITEOUT=\"$TARGET_ROOT/whiteout\"; ")
                }
            }

            append("COUNT=0; ")
            append("if [ -f \"\$WHITEOUT\" ]; then ")
            append("  while IFS= read -r relpath || [ -n \"\$relpath\" ]; do ")
            append("    relpath=\$(echo \"\$relpath\" | tr -d '\\r\\n'); ")
            append("    [ -z \"\$relpath\" ] && continue; ")
            append("    [ \"\$relpath\" = \"whiteout\" ] && continue; ")
            append("    [ \"\$relpath\" = \".whiteout\" ] && continue; ")
            append("    TARGET_FILE=\"$TARGET_ROOT/\$relpath\"; ")
            append("    if [ -e \"\$TARGET_FILE\" ] || [ -L \"\$TARGET_FILE\" ]; then ")
            append("      rm -f \"\$TARGET_FILE\"; ")
            append("      COUNT=\$((COUNT + 1)); ")
            append("    fi; ")
            append("  done < \"\$WHITEOUT\"; ")
            append("  rm -f \"$TARGET_ROOT/whiteout\" \"$TARGET_ROOT/.whiteout\" 2>/dev/null || true; ")
            append("fi; ")
            append("find \"$TARGET_ROOT\" -depth -mindepth 1 -type d -empty -delete 2>/dev/null || true; ")
            append("[ -d \"$TARGET_ROOT\" ] && rmdir \"$TARGET_ROOT\" 2>/dev/null || true; ")
            append("echo \"DELETED:\$COUNT\"")
        }

        try {
            val (exitCode, out) = runRootCommand(su, script)
            if (fallbackTmp.exists()) fallbackTmp.delete()

            if (exitCode != 0) {
                return@withContext Result.failure(RuntimeException("删除底座失败(exit: $exitCode): $out"))
            }

            val countMatch = Regex("DELETED:([0-9]+)").find(out)
            val deletedCount = countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            checkStatus()

            Result.success("已精准移除${deletedCount}个底座文件与whiteout清单，已保留其他文件")
        } catch (e: Exception) {
            if (fallbackTmp.exists()) fallbackTmp.delete()
            Result.failure(e)
        }
    }

    suspend fun installBootstrap(
        context: Context,
        force: Boolean = false,
        onProgress: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (_state.value == BootstrapState.INSTALLING) {
            return@withContext Result.failure(IllegalStateException("自举底座正在部署中，请勿重复操作"))
        }

        _state.value = BootstrapState.INSTALLING
        clearLogs()

        appendLog("\u001B[1;34m[*] >>> 开始执行0vl0ad3r自举底座部署任务...\u001B[0m")
        appendLog("\u001B[1;30m目标路径: $TARGET_ROOT\u001B[0m")
        onProgress?.invoke("正在校验宿主环境与Root权限...")

        val su = PacmanEngine.getInstance().getSuBinaryPath()
        val (rootExit, rootOut) = runRootCommand(su, "id")
        if (rootExit != 0 || !rootOut.contains("uid=0")) {
            val err = "未检测到有效Root权限(uid=0)，无法部署至系统目录"
            appendLog("\u001B[1;31m[-] 错误: $err\u001B[0m")
            _state.value = BootstrapState.NOT_INSTALLED
            return@withContext Result.failure(IllegalStateException(err))
        }
        appendLog("\u001B[1;32m[+] 宿主Root权限验证正常($su)\u001B[0m")

        val (rwExit, rwOut) = runRootCommand(su, "touch \"/system/.0vl_rw_test\" 2>&1 && rm -f \"/system/.0vl_rw_test\" || echo RO")
        if (rwOut.contains("RO") || rwOut.contains("Read-only file system", ignoreCase = true)) {
            val err = "目标/system分区处于只读状态，请确认已在设置中挂载/system OverlayFS"
            appendLog("\u001B[1;31m[-] 错误: $err\u001B[0m")
            _state.value = BootstrapState.NOT_INSTALLED
            return@withContext Result.failure(IllegalStateException(err))
        }
        appendLog("\u001B[1;32m[+] /system OverlayFS可写层挂载校验通过\u001B[0m")

        appendLog("\u001B[1;34m[*] 正在清理目标环境幽灵残留...\u001B[0m")
        runRootCommand(su, "rm -rf \"$TARGET_ROOT\"/* \"$TARGET_ROOT\"/.* 2>/dev/null || true")

        val cacheZstd = File(context.cacheDir, "zstd_bootstrap")
        val cacheArchive = File(context.cacheDir, "0vl0ad3r-bootstrap.tar.zst")
        val cacheWhiteout = File(context.cacheDir, "bootstrap.whiteout")

        try {
            appendLog("\u001B[1;34m[*] 正在释放安装引擎与自举镜像到缓存...\u001B[0m")
            context.assets.open(BOOTSTRAP_ZSTD_ASSET).use { input ->
                FileOutputStream(cacheZstd).use { output -> input.copyTo(output) }
            }
            cacheZstd.setExecutable(true, false)
            cacheZstd.setReadable(true, false)

            context.assets.open(BOOTSTRAP_ARCHIVE_ASSET).use { input ->
                FileOutputStream(cacheArchive).use { output -> input.copyTo(output) }
            }
            cacheArchive.setReadable(true, false)

            context.assets.open(BOOTSTRAP_WHITEOUT_ASSET).use { input ->
                FileOutputStream(cacheWhiteout).use { output -> input.copyTo(output) }
            }
            cacheWhiteout.setReadable(true, false)
            appendLog("\u001B[1;32m[+] 缓存资源准备就绪(${cacheArchive.length() / 1024}KB)\u001B[0m")

            appendLog("\u001B[1;34m[*] 开始执行流式解压: zstd -dc | tar -xf - -C $TARGET_ROOT...\u001B[0m")
            onProgress?.invoke("正在向/system/0vl0ad3r解压写入底座文件...")

            val deployScript = buildString {
                append("mkdir -p \"$TARGET_ROOT\" && ")
                append("\"${cacheZstd.absolutePath}\" -dc \"${cacheArchive.absolutePath}\" | tar -xf - -C \"$TARGET_ROOT\" && ")
                append("cp \"${cacheWhiteout.absolutePath}\" \"$TARGET_ROOT/whiteout\" && ")
                append("chmod 644 \"$TARGET_ROOT/whiteout\"; ")
                append("chmod 755 \"$TARGET_ROOT/bin\"/* 2>/dev/null || true; ")
                append("chmod 755 \"$TARGET_ROOT/lib64\"/* 2>/dev/null || true; ")
                append("chown -R 0:0 \"$TARGET_ROOT\" 2>/dev/null || true; ")
                append("[ -x \"$TARGET_ROOT/bin/pacman\" ] && echo \"DEPLOY_OK\"")
            }

            val (deployExit, deployOut) = runRootCommand(su, deployScript)
            if (deployExit != 0 || !deployOut.contains("DEPLOY_OK")) {
                val err = "自举底座解压写入失败(exit: $deployExit): $deployOut"
                appendLog("\u001B[1;31m[-] $err\u001B[0m")
                _state.value = BootstrapState.CORRUPTED
                return@withContext Result.failure(RuntimeException(err))
            }
            appendLog("\u001B[1;32m[+] 核心文件释放完成，已配置权限0755与所有者0:0\u001B[0m")

            appendLog("\u001B[1;34m[*] 正在执行Bionic动态链接与自举完整性验证...\u001B[0m")
            val status = checkStatus()
            if (status != BootstrapState.READY) {
                val err = "部署后自检失败，可能缺少必要动态库"
                appendLog("\u001B[1;31m[-] 错误: $err\u001B[0m")
                return@withContext Result.failure(IllegalStateException(err))
            }

            val finalVer = _version.value ?: "7.1.0"
            appendLog("\u001B[1;32m[+] 校验通过: Pacman v$finalVer(libalpm 16.0.0)\u001B[0m")
            appendLog("\u001B[1;32m[+] 0vl0ad3r自举底座部署成功，系统处于就绪状态！\u001B[0m")
            onProgress?.invoke("自举底座部署成功")
            Result.success("自举底座部署成功(Pacman v$finalVer)")
        } catch (e: Exception) {
            val err = e.message ?: "未知异常"
            appendLog("\u001B[1;31m[-] 异常: $err\u001B[0m")
            _state.value = BootstrapState.CORRUPTED
            Result.failure(e)
        } finally {
            try {
                if (cacheZstd.exists()) cacheZstd.delete()
                if (cacheArchive.exists()) cacheArchive.delete()
                if (cacheWhiteout.exists()) cacheWhiteout.delete()
            } catch (_: Exception) {
            }
        }
    }

    private fun runRootCommand(su: String, command: String): Pair<Int, String> {
        return try {
            val process = ProcessBuilder(su, "-c", command)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(25, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                Pair(-1, "命令执行超时")
            } else {
                val text = process.inputStream.bufferedReader().use { it.readText().trim() }
                Pair(process.exitValue(), text)
            }
        } catch (e: Exception) {
            Pair(-1, e.message ?: "执行异常")
        }
    }

    companion object {
        const val TARGET_ROOT = PacmanEngine.TARGET_ROOT
        const val PACMAN_BIN = PacmanEngine.PACMAN_BIN
        const val PACMAN_CONF = PacmanEngine.PACMAN_CONF
        const val PACMAN_LOCK = "$TARGET_ROOT/var/lib/pacman/db.lck"
        const val BOOTSTRAP_ZSTD_ASSET = "bootstrap/zstd"
        const val BOOTSTRAP_ARCHIVE_ASSET = "bootstrap/0vl0ad3r-bootstrap-aarch64.tar.zst"

        const val BOOTSTRAP_WHITEOUT_ASSET = "bootstrap/whiteout"

        private val CRITICAL_FILES = listOf(
            "bin/pacman",
            "bin/pacman-conf",
            "bin/zstd",
            "bin/curl",
            "etc/pacman.conf",
            "etc/ssl/bootstrap-ca.pem",
            "lib64/libalpm.so",
            "lib64/libarchive.so",
            "lib64/libcrypto.so",
            "lib64/libssl.so",
            "lib64/libcurl.so",
            "lib64/liblzma.so",
            "lib64/libzstd.so",
            "lib64/libz.so"
        )

        @Volatile
        private var instance: BootstrapInstaller? = null

        fun getInstance(): BootstrapInstaller =
            instance ?: synchronized(this) {
                instance ?: BootstrapInstaller().also { instance = it }
            }
    }
}
