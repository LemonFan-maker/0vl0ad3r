package com.orionisli.ovl0ad3r.data.ipc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.os.SystemClock
import com.orionisli.gh0stra1n.ipc.IGh0stOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class GhostRainBridge private constructor() {

    private var service: IGh0stOverlayService? = null
    private var boundBinder: IBinder? = null
    private var appContext: Context? = null
    private var isBound = false
    private var autoRebindEnabled = true
    private var rebindJob: Job? = null
    private var lastServiceWakeAt = 0L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isRootAlive = MutableStateFlow(false)
    val isRootAlive: StateFlow<Boolean> = _isRootAlive.asStateFlow()

    private val _partitionStates = MutableStateFlow<Map<String, Int>>(emptyMap())
    val partitionStates: StateFlow<Map<String, Int>> = _partitionStates.asStateFlow()

    private val deathRecipient = IBinder.DeathRecipient {
        handleBinderDied()
    }

    internal val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            handleConnected(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            handleDisconnected()
        }
    }

    internal var localCommandExecutor: (String) -> Pair<Int, String> = { cmd ->
        val su = findLocalSu()
        try {
            val process = ProcessBuilder(su, "-c", cmd)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(5, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                Pair(-1, "")
            } else {
                val out = process.inputStream.bufferedReader().use { it.readText().trim() }
                Pair(process.exitValue(), out)
            }
        } catch (_: Exception) {
            Pair(-1, "")
        }
    }

    fun bind(context: Context): Boolean {
        val app = context.applicationContext
        appContext = app
        if (isBound) {
            if (_isConnected.value) return true
            ensureConnectSupervisor(app)
            return false
        }
        isBound = true
        val bound = bindInternal(app)
        ensureConnectSupervisor(app)
        scope.launch { updateStatus() }
        return bound
    }

    private fun bindInternal(context: Context): Boolean {
        val intent = Intent(SERVICE_ACTION).apply {
            component = ComponentName(SERVICE_PACKAGE, SERVICE_CLASS)
        }
        return try {
            val ok = context.applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            android.util.Log.d("GhostRainBridge", "bindService called, returned: $ok")
            ok
        } catch (e: Exception) {
            android.util.Log.e("GhostRainBridge", "bindService threw exception", e)
            false
        }
    }

    fun unbind(context: Context) {
        rebindJob?.cancel()
        rebindJob = null
        if (!isBound) return
        isBound = false
        try {
            context.applicationContext.unbindService(connection)
        } catch (_: Exception) {
        } finally {
            unlinkDeathRecipient()
            service = null
            _isConnected.value = false
            _isRootAlive.value = false
        }
    }

    fun setAutoRebind(enabled: Boolean) {
        autoRebindEnabled = enabled
        if (!enabled) {
            rebindJob?.cancel()
            rebindJob = null
        } else if (isBound) {
            appContext?.let { ensureConnectSupervisor(it) }
        }
    }

    internal fun handleConnected(binder: IBinder?) {
        unlinkDeathRecipient()
        rebindJob?.cancel()
        rebindJob = null
        isBound = true
        boundBinder = binder
        binder?.let {
            try {
                it.linkToDeath(deathRecipient, 0)
            } catch (_: RemoteException) {
                handleBinderDied()
                return
            }
        }
        service = IGh0stOverlayService.Stub.asInterface(binder)
        _isConnected.value = service != null
        updateStatus()
    }

    internal fun handleDisconnected() {
        unlinkDeathRecipient()
        service = null
        _isConnected.value = false
        _isRootAlive.value = false
        scheduleAutoRebind()
    }

    internal fun handleBinderDied() {
        unlinkDeathRecipient()
        service = null
        _isConnected.value = false
        _isRootAlive.value = false
        scheduleAutoRebind()
    }

    private fun unlinkDeathRecipient() {
        try {
            boundBinder?.unlinkToDeath(deathRecipient, 0)
        } catch (_: Exception) {
        } finally {
            boundBinder = null
        }
    }

    internal fun scheduleAutoRebind() {
        if (!autoRebindEnabled || !isBound) return
        appContext?.let { ensureConnectSupervisor(it) }
    }

    private fun ensureConnectSupervisor(ctx: Context) {
        if (!autoRebindEnabled || !isBound) return
        if (rebindJob?.isActive == true) return

        val app = ctx.applicationContext
        rebindJob = scope.launch {
            var retryDelay = REBIND_RETRY_MS
            var attempts = 0
            while (isActive && isBound) {
                delay(retryDelay)
                if (!isBound || _isConnected.value) break
                attempts += 1

                service = null
                try {
                    app.unbindService(connection)
                } catch (_: Exception) {
                }
                if (!isBound || _isConnected.value) break

                val ok = bindInternal(app)
                retryDelay = if (ok) {
                    REBIND_RETRY_MS
                } else {
                    (retryDelay * 2).coerceAtMost(MAX_REBIND_RETRY_MS)
                }

                if (!ok || attempts >= 2) {
                    wakeServiceWithRoot()
                }
            }
        }
    }

    private fun wakeServiceWithRoot() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastServiceWakeAt < SERVICE_WAKE_INTERVAL_MS) return
        lastServiceWakeAt = now
        val command = "am start-foreground-service --user 0 -n " +
            "$SERVICE_PACKAGE/.Gh0stOverlayService"
        val (exit, out) = try {
            localCommandExecutor(command)
        } catch (_: Exception) {
            Pair(-1, "")
        }
        android.util.Log.d(
            "GhostRainBridge",
            "daemon wake via root: exit=$exit${if (out.isBlank()) "" else " out=$out"}"
        )
    }

    suspend fun reconnect(): Boolean = withContext(Dispatchers.IO) {
        val ctx = appContext
        if (ctx == null) {
            _isRootAlive.value = checkRoot()
            refreshPartitionStates()
            return@withContext _isConnected.value
        }
        if (isBound) {
            rebindJob?.cancel()
            rebindJob = null
            try {
                ctx.unbindService(connection)
            } catch (_: Exception) {
            }
            service = null
            _isConnected.value = false
            isBound = true
            bindInternal(ctx)
            ensureConnectSupervisor(ctx)
        }
        var waited = 0L
        while (waited < MANUAL_RECONNECT_WAIT_MS && !_isConnected.value) {
            delay(WATCHDOG_POLL_MS)
            waited += WATCHDOG_POLL_MS
        }
        _isRootAlive.value = checkRoot()
        refreshPartitionStates()
        _isConnected.value
    }

    private fun updateStatus() {
        val currentService = service
        if (currentService != null) {
            try {
                _isRootAlive.value = currentService.isRootAlive
            } catch (_: RemoteException) {
                _isRootAlive.value = checkLocalRoot()
            }
        } else {
            _isRootAlive.value = checkLocalRoot()
        }
    }

    suspend fun refreshStatus(): Boolean = withContext(Dispatchers.IO) {
        val alive = checkRoot()
        _isRootAlive.value = alive
        alive
    }

    suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        val currentService = service
        val alive = if (currentService != null) {
            try {
                currentService.isRootAlive
            } catch (_: RemoteException) {
                checkLocalRoot()
            }
        } else {
            checkLocalRoot()
        }
        _isRootAlive.value = alive
        alive
    }

    suspend fun getPartitionStatus(partitionName: String): Int = withContext(Dispatchers.IO) {
        val currentService = service
        val status = if (currentService != null) {
            try {
                currentService.getPartitionStatus(partitionName)
            } catch (_: RemoteException) {
                getLocalPartitionStatus(partitionName)
            }
        } else {
            getLocalPartitionStatus(partitionName)
        }
        _partitionStates.value = _partitionStates.value + (partitionName to status)
        status
    }

    suspend fun isPartitionMounted(partitionName: String): Boolean =
        getPartitionStatus(partitionName) == 1

    suspend fun getUpperDir(partitionName: String): String? = withContext(Dispatchers.IO) {
        val currentService = service
        if (currentService != null) {
            try {
                currentService.getUpperDir(partitionName) ?: getLocalUpperDir(partitionName)
            } catch (_: RemoteException) {
                getLocalUpperDir(partitionName)
            }
        } else {
            getLocalUpperDir(partitionName)
        }
    }

    suspend fun getPartitionFreeSpace(partitionName: String): Long = withContext(Dispatchers.IO) {
        val currentService = service
        if (currentService != null) {
            try {
                currentService.getPartitionFreeSpace(partitionName)
            } catch (_: RemoteException) {
                getLocalPartitionFreeSpace(partitionName)
            }
        } else {
            getLocalPartitionFreeSpace(partitionName)
        }
    }

    suspend fun mountPartition(partitionName: String): Boolean = withContext(Dispatchers.IO) {
        val currentService = service
        val success = if (currentService != null) {
            try {
                currentService.mountPartition(partitionName)
            } catch (_: RemoteException) {
                false
            }
        } else {
            false
        }
        if (success) {
            getPartitionStatus(partitionName)
        }
        success
    }

    suspend fun unmountPartition(partitionName: String): Boolean = withContext(Dispatchers.IO) {
        val currentService = service
        val success = if (currentService != null) {
            try {
                currentService.unmountPartition(partitionName)
            } catch (_: RemoteException) {
                localUnmountPartition(partitionName)
            }
        } else {
            localUnmountPartition(partitionName)
        }
        if (success) {
            getPartitionStatus(partitionName)
        }
        success
    }

    suspend fun syncStorage(): Boolean = withContext(Dispatchers.IO) {
        val currentService = service
        if (currentService != null) {
            try {
                currentService.syncStorage()
                true
            } catch (_: RemoteException) {
                localSyncStorage()
            }
        } else {
            localSyncStorage()
        }
    }

    suspend fun restartZygote(): Boolean = withContext(Dispatchers.IO) {
        val currentService = service
        if (currentService != null) {
            try {
                currentService.restartZygote()
            } catch (_: RemoteException) {
                localRestartZygote()
            }
        } else {
            localRestartZygote()
        }
    }

    suspend fun executeRootCommandWithOutput(command: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        localCommandExecutor(command)
    }

    suspend fun refreshPartitionStates(
        partitions: List<String> = listOf("system", "vendor", "product")
    ): Map<String, Int> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Int>()
        for (part in partitions) {
            results[part] = getPartitionStatus(part)
        }
        _partitionStates.value = results
        results
    }

    private fun findLocalSu(): String {
        val candidates = listOf(
            "/data/local/tmp/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/system_ext/bin/su",
            "/product/bin/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/ap/bin/su",
            "/data/adb/magisk/su"
        )
        for (candidate in candidates) {
            val file = File(candidate)
            if (file.exists() && file.canExecute()) {
                return candidate
            }
        }
        return "su"
    }

    private fun checkLocalRoot(): Boolean {
        val (exit, out) = localCommandExecutor("id")
        return exit == 0 && out.contains("uid=0")
    }

    private fun getLocalPartitionStatus(partitionName: String): Int {
        val (exit, out) = localCommandExecutor("cat /proc/mounts")
        if (exit != 0) return 0
        val target = "/$partitionName"
        val isOverlayLive = out.lineSequence().any { line ->
            val tokens = line.trim().split("\\s+".toRegex())
            tokens.size >= 4 &&
                tokens[1] == target &&
                tokens[2] == OVERLAY_FSTYPE &&
                tokens[0].startsWith(OVERLAY_DEVICE_PREFIX)
        }
        return if (isOverlayLive) 1 else 0
    }

    private fun getLocalUpperDir(partitionName: String): String? {
        val (exit, out) = localCommandExecutor("cat /proc/mounts")
        if (exit == 0) {
            val target = "/$partitionName"
            val line = out.lineSequence().firstOrNull { line ->
                val tokens = line.trim().split("\\s+".toRegex())
                tokens.size >= 4 && tokens[1] == target && tokens[2] == OVERLAY_FSTYPE
            }
            if (line != null) {
                val match = Regex("upperdir=([^,]+)").find(line)
                if (match != null) return match.groupValues[1]
            }
        }
        return "/data/local/tmp/gh0stra1n/mnt_$partitionName/u"
    }

    private fun getLocalPartitionFreeSpace(partitionName: String): Long {
        val (exit, out) = localCommandExecutor("df -k /$partitionName")
        if (exit == 0) {
            val lines = out.lines().filter { it.isNotBlank() }
            if (lines.size >= 2) {
                val tokens = lines[1].trim().split("\\s+".toRegex())
                if (tokens.size >= 4) {
                    tokens[3].toLongOrNull()?.let { return it * 1024L }
                }
            }
        }
        return 0L
    }

    private fun localUnmountPartition(partitionName: String): Boolean {
        val (exit, _) = localCommandExecutor("umount -l /$partitionName")
        return exit == 0
    }

    private fun localSyncStorage(): Boolean {
        val (exit, _) = localCommandExecutor("sync")
        return exit == 0
    }

    private fun localRestartZygote(): Boolean {
        val (exit, _) = localCommandExecutor("setprop ctl.restart zygote")
        return exit == 0
    }

    internal fun attachTestService(testService: IGh0stOverlayService?, testBinder: IBinder? = null) {
        unlinkDeathRecipient()
        service = testService
        boundBinder = testBinder
        testBinder?.let {
            try {
                it.linkToDeath(deathRecipient, 0)
            } catch (_: Exception) {
            }
        }
        _isConnected.value = testService != null
        if (testService != null) {
            updateStatus()
        } else {
            _isRootAlive.value = false
        }
    }

    internal fun triggerDeathRecipient() {
        deathRecipient.binderDied()
    }

    internal fun resetForTesting() {
        unlinkDeathRecipient()
        rebindJob?.cancel()
        rebindJob = null
        service = null
        isBound = false
        appContext = null
        _isConnected.value = false
        _isRootAlive.value = false
        _partitionStates.value = emptyMap()
    }

    companion object {
        private const val OVERLAY_FSTYPE = "overlay"
        private const val OVERLAY_DEVICE_PREFIX = "ovl"

        private const val REBIND_RETRY_MS = 5000L

        private const val MAX_REBIND_RETRY_MS = 30000L

        private const val SERVICE_WAKE_INTERVAL_MS = 30000L

        private const val WATCHDOG_POLL_MS = 150L

        private const val MANUAL_RECONNECT_WAIT_MS = 6000L

        const val SERVICE_ACTION = "com.orionisli.gh0stra1n.OVERLAY_SERVICE"
        const val SERVICE_PACKAGE = "com.orionisli.gh0stra1n"
        const val SERVICE_CLASS = "com.orionisli.gh0stra1n.Gh0stOverlayService"

        @Volatile
        private var instance: GhostRainBridge? = null

        fun getInstance(): GhostRainBridge =
            instance ?: synchronized(this) {
                instance ?: GhostRainBridge().also { instance = it }
            }
    }
}
