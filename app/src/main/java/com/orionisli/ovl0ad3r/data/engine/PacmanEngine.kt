package com.orionisli.ovl0ad3r.data.engine

import com.orionisli.ovl0ad3r.data.model.QueueAction
import com.orionisli.ovl0ad3r.R
import com.orionisli.ovl0ad3r.data.model.QueueTask
import com.orionisli.ovl0ad3r.data.repo.RepoPackage
import com.orionisli.ovl0ad3r.data.repo.RepoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class ExecutionStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED
}

sealed class ExecutionState(val status: ExecutionStatus) {
    data object Idle : ExecutionState(ExecutionStatus.IDLE)

    data class Running(
        val progress: Float = 0f,
        val currentAction: String = "",
        val lastLine: String = "",
        val lines: List<String> = emptyList()
    ) : ExecutionState(ExecutionStatus.RUNNING)

    data class Completed(
        val exitCode: Int = 0,
        val lines: List<String> = emptyList()
    ) : ExecutionState(ExecutionStatus.COMPLETED)

    data class Failed(
        val exitCode: Int = -1,
        val errorMessage: String = "",
        val lines: List<String> = emptyList()
    ) : ExecutionState(ExecutionStatus.FAILED)
}

class PacmanEngine private constructor() {

    private val _executionState = MutableStateFlow<ExecutionState>(ExecutionState.Idle)
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    private val _outputFlow = MutableSharedFlow<String>(extraBufferCapacity = 500)
    val outputFlow: SharedFlow<String> = _outputFlow.asSharedFlow()

    private var activeProcess: Process? = null

    @Volatile
    private var cancelRequested = false

    var messageResolver: (Int, Array<out Any>) -> String = { _, _ -> "" }

    private fun message(resId: Int, vararg args: Any): String = messageResolver(resId, args)

    fun getSuBinaryPath(): String {
        val volatileSu = File("/data/local/tmp/su")
        return when {
            volatileSu.exists() && volatileSu.canExecute() -> "/data/local/tmp/su"
            File("/system/bin/su").exists() -> "/system/bin/su"
            File("/system/xbin/su").exists() -> "/system/xbin/su"
            else -> "su"
        }
    }

    fun buildPacmanCommand(args: String): String {
        return "$PACMAN_BIN --config $PACMAN_CONF --root $TARGET_ROOT $args"
    }

    fun buildInstallCommand(packageNames: List<String>): String {
        return buildPacmanCommand(
            "-S --needed --noconfirm $BOOTSTRAP_OVERWRITE ${packageNames.joinToString(" ")}"
        )
    }

    fun buildLocalInstallCommand(filePaths: List<String>): String {
        val quoted = filePaths.joinToString(" ") { shellQuote(it) }
        return buildPacmanCommand("-U --noconfirm $BOOTSTRAP_OVERWRITE $quoted")
    }

    internal fun buildStagePackagesCommand(files: List<File>, stagingDir: String): String {
        if (files.isEmpty()) return "true"
        val dir = shellQuote(stagingDir)
        val baseDir = shellQuote(File(stagingDir).parent ?: "/data/local/tmp/0vl0ad3r")
        return buildString {
            append("mkdir -p ").append(baseDir)
            append(" && chmod 700 ").append(baseDir)
            append(" && mkdir -p ").append(dir)
            append(" && chmod 700 ").append(dir)
            files.forEach { src ->
                val dst = "$stagingDir/${src.name}"
                append(" && cp -f ").append(shellQuote(src.absolutePath))
                    .append(' ').append(shellQuote(dst))
                append(" && test -s ").append(shellQuote(dst))
                append(" && chmod 644 ").append(shellQuote(dst))
            }
        }
    }

    fun buildRemoveCommand(packageNames: List<String>): String {
        return buildPacmanCommand("-R --noconfirm ${packageNames.joinToString(" ")}")
    }

    fun buildQueryCommand(): String =
        NORMALIZE_PACMAN_CONFIG +
            "export PATH=\"$TARGET_ROOT/bin:\$PATH\"; " +
            "export LD_LIBRARY_PATH=\"$TARGET_ROOT/lib64:\$LD_LIBRARY_PATH\"; " +
            buildPacmanCommand("-Q")

    fun buildSyncCommand(): String {
        return buildPacmanCommand("-Sy --noconfirm")
    }

    fun buildMirrorlistCommand(sourceUrls: List<String>): String? {
        val servers = sourceUrls.filter { it.startsWith("https://") }
        if (servers.isEmpty()) return null
        val args = servers.joinToString(" ") { shellQuote("Server = $it") }
        return "mkdir -p \"$TARGET_ROOT/etc/pacman.d\" && " +
            "printf '%s\\n' $args > \"$TARGET_ROOT/etc/pacman.d/mirrorlist\""
    }

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    fun buildUpgradeCommand(): String {
        return buildPacmanCommand("-Syu --noconfirm $BOOTSTRAP_OVERWRITE")
    }

    suspend fun executeCommand(
        rawCommand: String,
        preludeLines: List<String> = emptyList()
    ): ExecutionState = withContext(Dispatchers.IO) {
        cancelRequested = false
        val su = getSuBinaryPath()
        val wrappedCommand = buildString {
            append(NORMALIZE_PACMAN_CONFIG)
            append("export PATH=\"$TARGET_ROOT/bin:\$PATH\"; ")
            append("export LD_LIBRARY_PATH=\"$TARGET_ROOT/lib64:\$LD_LIBRARY_PATH\"; ")
            append("export SSL_CERT_FILE=\"$TARGET_ROOT/etc/ssl/bootstrap-ca.pem\"; ")
            append("export CURL_CA_BUNDLE=\"$TARGET_ROOT/etc/ssl/bootstrap-ca.pem\"; ")
            append(rawCommand)
        }

        val accumulatedLines = preludeLines.toMutableList()
        var currentProgress = 0f
        var currentAction = ""

        _executionState.value = ExecutionState.Running(
            progress = 0f,
            currentAction = "",
            lastLine = preludeLines.lastOrNull() ?: "",
            lines = preludeLines
        )

        try {
            val process = ProcessBuilder(su, "-c", wrappedCommand)
                .redirectErrorStream(true)
                .start()
            activeProcess = process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String? = null

            while (coroutineContext.isActive && reader.readLine().also { line = it } != null) {
                val currentLine = line ?: break
                accumulatedLines.add(currentLine)
                _outputFlow.tryEmit(currentLine)

                val detectedProgress = detectProgress(currentLine)
                if (detectedProgress >= 0f) {
                    currentProgress = detectedProgress
                }

                val detectedAction = detectAction(currentLine)
                if (detectedAction.isNotEmpty()) {
                    currentAction = detectedAction
                }

                _executionState.value = ExecutionState.Running(
                    progress = currentProgress,
                    currentAction = currentAction,
                    lastLine = currentLine,
                    lines = accumulatedLines.toList()
                )
            }

            val exitCode = process.waitFor()
            activeProcess = null

            val finalState = if (exitCode == 0) {
                ExecutionState.Completed(
                    exitCode = 0,
                    lines = accumulatedLines.toList()
                )
            } else {
                ExecutionState.Failed(
                    exitCode = exitCode,
                    errorMessage = message(R.string.execution_failed_exit, exitCode),
                    lines = accumulatedLines.toList()
                )
            }
            _executionState.value = finalState
            finalState
        } catch (_: CancellationException) {
            cancelExecution()
            val cancelState = ExecutionState.Failed(
                exitCode = -1,
                errorMessage = message(R.string.execution_cancelled),
                lines = accumulatedLines.toList()
            )
            _executionState.value = cancelState
            cancelState
        } catch (e: Exception) {
            val failState = ExecutionState.Failed(
                exitCode = -1,
                errorMessage = e.message ?: message(R.string.execution_error),
                lines = accumulatedLines.toList()
            )
            _executionState.value = failState
            failState
        } finally {
            activeProcess = null
        }
    }

    fun buildQueueCommands(
        tasks: List<QueueTask>,
        mirrorUrls: List<String> = emptyList()
    ): List<String> {
        val removeTasks = tasks.filter { it.action == QueueAction.REMOVE }
        val installTasks = tasks.filter {
            it.action == QueueAction.INSTALL || it.action == QueueAction.UPGRADE
        }

        val commands = mutableListOf<String>()
        if (removeTasks.isNotEmpty()) {
            commands.add(buildRemoveCommand(removeTasks.map { it.packageItem.packageName }))
        }
        if (installTasks.isNotEmpty()) {
            buildMirrorlistCommand(mirrorUrls)?.let { commands.add(it) }
            commands.add(buildSyncCommand())
            val targets = (installTasks.map { it.packageItem.packageName } + BASE_LAYOUT_PACKAGE)
                .distinct()
            commands.add(buildInstallCommand(targets))
        }
        return commands
    }

    suspend fun executeQueue(
        tasks: List<QueueTask>,
        mirrorUrls: List<String> = emptyList()
    ): ExecutionState = withContext(Dispatchers.IO) {
        val commands = buildQueueCommands(tasks, mirrorUrls)
        if (commands.isEmpty()) {
            val idle = ExecutionState.Idle
            _executionState.value = idle
            return@withContext idle
        }
        executeCommand(commands.joinToString(" && "))
    }

    private suspend fun downloadPackageWithFreshIndex(
        sourceBase: String,
        pkg: RepoPackage,
        dest: File,
        onBytes: (Long) -> Unit
    ): Long {
        try {
            return RepoRepository.INSTANCE.downloadPackage(
                sourceBase = sourceBase,
                filename = pkg.filename,
                expectedSha256 = pkg.sha256,
                dest = dest,
                onBytes = onBytes
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (!e.message.orEmpty().contains("sha256 校验失败")) throw e
            val fresh = try {
                RepoRepository.fetchIndex(sourceBase).first
            } catch (_: Exception) {
                throw e
            }
            val freshPkg = fresh.packages.firstOrNull { it.name == pkg.name } ?: throw e
            if (freshPkg.sha256 == pkg.sha256 || freshPkg.filename != pkg.filename) throw e
            return RepoRepository.INSTANCE.downloadPackage(
                sourceBase = sourceBase,
                filename = freshPkg.filename,
                expectedSha256 = freshPkg.sha256,
                dest = dest,
                onBytes = onBytes
            )
        }
    }

    suspend fun executeRepoQueue(
        tasks: List<QueueTask>,
        catalog: List<RepoPackage>,
        installed: Set<String>,
        sourceBase: String?,
        mirrorUrls: List<String> = emptyList(),
        cacheDir: File
    ): ExecutionState = withContext(Dispatchers.IO) {
        cancelRequested = false

        val removeNames = tasks.filter { it.action == QueueAction.REMOVE }
            .map { it.packageItem.packageName }
            .distinct()
        val targetNames = tasks.filter {
            it.action == QueueAction.INSTALL || it.action == QueueAction.UPGRADE
        }.map { it.packageItem.packageName }.distinct()

        if (removeNames.isEmpty() && targetNames.isEmpty()) {
            _executionState.value = ExecutionState.Idle
            return@withContext ExecutionState.Idle
        }

        val prelude = mutableListOf<String>()

        val requested = if (targetNames.isEmpty()) {
            emptyList()
        } else {
            targetNames + if (BASE_LAYOUT_PACKAGE !in installed) listOf(BASE_LAYOUT_PACKAGE) else emptyList()
        }
        val plan = DependencyResolver.resolve(requested, catalog, installed)

        if (plan.missing.isNotEmpty()) {
            val failed = ExecutionState.Failed(
                exitCode = -1,
                errorMessage = message(R.string.execution_packages_missing, plan.missing.joinToString(", ")),
                lines = prelude.toList()
            )
            _executionState.value = failed
            return@withContext failed
        }

        val downloads = plan.ordered.filter { it.filename.isNotBlank() }
        val base = sourceBase?.takeIf { it.isNotBlank() }
        if (downloads.isNotEmpty() && base == null) {
            val failed = ExecutionState.Failed(
                exitCode = -1,
                errorMessage = message(R.string.execution_source_missing),
                lines = prelude.toList()
            )
            _executionState.value = failed
            return@withContext failed
        }

        try {
            File(cacheDir, "packages").deleteRecursively()
        } catch (_: Exception) {
        }
        val runId = "${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
        val packageDir = File(cacheDir, "packages-$runId").apply { mkdirs() }
        val localPaths = mutableListOf<String>()
        var stagingDir: String? = null
        val totalBytes = downloads.sumOf { it.sizeBytes }.coerceAtLeast(1L)
        var doneBytes = 0L

        try {
            downloads.forEachIndexed { index, pkg ->
                if (cancelRequested) throw CancellationException("cancelled")
                val dest = File(packageDir, pkg.filename.substringAfterLast('/'))
                if (dest.exists()) dest.delete()

                prelude += message(R.string.execution_download_start, pkg.name, index + 1, downloads.size)
                val baseBytes = doneBytes
                val bytes = downloadPackageWithFreshIndex(
                    sourceBase = base!!,
                    pkg = pkg,
                    dest = dest
                ) { received ->
                    if (cancelRequested) throw CancellationException("cancelled")
                    _executionState.value = ExecutionState.Running(
                        progress = ((baseBytes + received).toFloat() / totalBytes).coerceIn(0f, 1f),
                        currentAction = pkg.name,
                        lastLine = message(
                            R.string.execution_download_progress,
                            pkg.name,
                            formatBytes(received),
                            formatBytes(pkg.sizeBytes)
                        ),
                        lines = prelude.toList()
                    )
                }
                if (!dest.isFile || dest.length() <= 0L) {
                    throw IOException("下载后包体不存在或为空: ${dest.absolutePath}")
                }
                doneBytes += bytes
                prelude += message(R.string.execution_download_done, pkg.name, formatBytes(bytes))
                localPaths += dest.absolutePath
            }

            val commands = mutableListOf<String>()
            buildMirrorlistCommand(mirrorUrls)?.let { commands += it }
            if (removeNames.isNotEmpty()) commands += buildRemoveCommand(removeNames)
            if (localPaths.isNotEmpty()) {
                prelude += message(R.string.execution_local_install, localPaths.size)
                val runStagingDir = "$ROOT_STAGING_BASE/packages-$runId"
                stagingDir = runStagingDir
                prelude += message(R.string.execution_stage_packages, runStagingDir)
                val stagedPaths = localPaths.map { "$runStagingDir/${File(it).name}" }
                commands += buildStagePackagesCommand(localPaths.map(::File), runStagingDir)
                commands += buildLocalInstallCommand(stagedPaths)
            }
            if (commands.isEmpty()) {
                val completed = ExecutionState.Completed(exitCode = 0, lines = prelude.toList())
                _executionState.value = completed
                return@withContext completed
            }
            if (cancelRequested) throw CancellationException("cancelled")
            val commandLine = commands.joinToString(" && ")
            val commandWithCleanup = stagingDir?.let { dir ->
                "$commandLine; RC=\$?; rm -rf ${shellQuote(dir)}; exit \$RC"
            } ?: commandLine
            executeCommand(commandWithCleanup, prelude.toList())
        } catch (_: CancellationException) {
            val cancelled = ExecutionState.Failed(
                exitCode = -1,
                errorMessage = message(R.string.execution_cancelled),
                lines = prelude.toList()
            )
            _executionState.value = cancelled
            cancelled
        } catch (e: Exception) {
            val failed = ExecutionState.Failed(
                exitCode = -1,
                errorMessage = e.message ?: message(R.string.execution_error),
                lines = prelude.toList()
            )
            _executionState.value = failed
            failed
        } finally {
            localPaths.forEach { path ->
                try {
                    File(path).delete()
                } catch (_: Exception) {
                }
            }
            try {
                packageDir.deleteRecursively()
            } catch (_: Exception) {
            }
            stagingDir?.let { cleanupRootStaging(it) }
        }
    }

    private fun cleanupRootStaging(stagingDir: String) {
        try {
            val process = ProcessBuilder(
                getSuBinaryPath(),
                "-c",
                "rm -rf ${shellQuote(stagingDir)}"
            ).redirectErrorStream(true).start()
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
        } catch (_: Exception) {
        }
    }

    fun cancelExecution() {
        cancelRequested = true
        try {
            activeProcess?.destroy()
        } catch (_: Exception) {
        } finally {
            activeProcess = null
        }
    }

    fun resetState() {
        _executionState.value = ExecutionState.Idle
    }

    private fun detectProgress(line: String): Float {
        val percentMatch = PERCENT_REGEX.find(line)
        if (percentMatch != null) {
            val p = percentMatch.groupValues[1].toIntOrNull()
            if (p != null) {
                return (p / 100f).coerceIn(0f, 1f)
            }
        }

        val stepMatch = STEP_REGEX.find(line)
        if (stepMatch != null) {
            val current = stepMatch.groupValues[1].toFloatOrNull()
            val total = stepMatch.groupValues[2].toFloatOrNull()
            if (current != null && total != null && total > 0f) {
                return (current / total).coerceIn(0f, 1f)
            }
        }
        return -1f
    }

    private fun detectAction(line: String): String {
        val stepMatch = STEP_REGEX.find(line)
        if (stepMatch != null) {
            val text = stepMatch.groupValues[3].trim()
            if (text.isNotEmpty()) return text
        }
        return ""
    }

    companion object {
        private const val NORMALIZE_PACMAN_CONFIG =
            "sed -i 's/^[[:space:]]*SigLevel[[:space:]]*=.*/SigLevel = Never/' /system/0vl0ad3r/etc/pacman.conf 2>/dev/null; " +
                "sed -i 's/^[[:space:]]*LocalFileSigLevel[[:space:]]*=.*/LocalFileSigLevel = Never/' /system/0vl0ad3r/etc/pacman.conf 2>/dev/null; "

        const val TARGET_ROOT = "/system/0vl0ad3r"
        const val PACMAN_BIN = "/system/0vl0ad3r/bin/pacman"
        const val PACMAN_CONF = "/system/0vl0ad3r/etc/pacman.conf"

        private const val ROOT_STAGING_BASE = "/data/local/tmp/0vl0ad3r"

        const val BASE_LAYOUT_PACKAGE = "base-layout"

        private const val BOOTSTRAP_OVERWRITE =
            "--overwrite 'bin/*' --overwrite 'lib64/*' " +
                "--overwrite 'etc/pacman.conf' --overwrite 'etc/pacman.d/mirrorlist' " +
                "--overwrite 'etc/0vl0ad3r.env'"

        private val PERCENT_REGEX = Regex("([0-9]{1,3})%")
        private val STEP_REGEX = Regex("\\(([0-9]+)/([0-9]+)\\)\\s*(.*)")

        internal fun formatBytes(bytes: Long): String {
            val kb = 1024.0
            return when {
                bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (kb * kb))
                bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / kb)
                else -> "$bytes B"
            }
        }

        @Volatile
        private var instance: PacmanEngine? = null

        fun getInstance(): PacmanEngine =
            instance ?: synchronized(this) {
                instance ?: PacmanEngine().also { instance = it }
            }
    }
}
