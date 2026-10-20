package com.orionisli.ovl0ad3r.data

import android.content.ComponentName
import android.os.IBinder
import android.os.IInterface
import android.os.RemoteException
import com.orionisli.gh0stra1n.ipc.IGh0stOverlayService
import com.orionisli.ovl0ad3r.data.ipc.GhostRainBridge
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GhostRainBridgeTest {

    private lateinit var bridge: GhostRainBridge

    @Before
    fun setup() {
        bridge = GhostRainBridge.getInstance()
        bridge.resetForTesting()
        bridge.setAutoRebind(false)
    }

    @After
    fun tearDown() {
        bridge.resetForTesting()
    }

    private class FakeGh0stOverlayService(
        var rootAlive: Boolean = true,
        var partitionStatus: Int = 1,
        var upperDir: String = "/data/local/tmp/gh0stra1n/mnt_system/u",
        var freeSpace: Long = 104857600L,
        var mountResult: Boolean = true,
        var unmountResult: Boolean = true,
        var restartZygoteResult: Boolean = true,
    ) : IGh0stOverlayService.Stub() {

        var syncStorageCalled = false
        var deathRecipient: IBinder.DeathRecipient? = null
        var unlinked = false

        override fun queryLocalInterface(descriptor: String): IInterface = this

        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) {
            deathRecipient = recipient
        }

        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int): Boolean {
            if (deathRecipient == recipient) {
                deathRecipient = null
                unlinked = true
                return true
            }
            return false
        }

        fun triggerCrash() {
            deathRecipient?.binderDied()
        }

        override fun isRootAlive(): Boolean = rootAlive
        override fun getPartitionStatus(partitionName: String?): Int = partitionStatus
        override fun getUpperDir(partitionName: String?): String = upperDir
        override fun getPartitionFreeSpace(partitionName: String?): Long = freeSpace
        override fun mountPartition(partitionName: String?): Boolean = mountResult
        override fun unmountPartition(partitionName: String?): Boolean = unmountResult
        override fun syncStorage() { syncStorageCalled = true }
        override fun restartZygote(): Boolean = restartZygoteResult
    }

    private class FakeRemoteBinder : IGh0stOverlayService.Stub() {
        override fun queryLocalInterface(descriptor: String): IInterface? = null
        override fun isRootAlive(): Boolean = false
        override fun getPartitionStatus(p: String?): Int = 0
        override fun getUpperDir(p: String?): String? = null
        override fun getPartitionFreeSpace(p: String?): Long = 0L
        override fun mountPartition(p: String?): Boolean = false
        override fun unmountPartition(p: String?): Boolean = false
        override fun syncStorage() {}
        override fun restartZygote(): Boolean = false
    }

    @Test
    fun testStubAndProxyResolution() {
        val testStub = FakeGh0stOverlayService()
        val resolvedStub = IGh0stOverlayService.Stub.asInterface(testStub)
        assertSame(testStub, resolvedStub)
        assertTrue(resolvedStub.isRootAlive)

        val remoteBinder = FakeRemoteBinder()
        val resolvedProxy = IGh0stOverlayService.Stub.asInterface(remoteBinder)
        assertNotNull(resolvedProxy)
        assertNotSame(remoteBinder, resolvedProxy)
        assertSame(remoteBinder, resolvedProxy.asBinder())
    }

    @Test
    fun testServiceConnectedAndStateUpdates() = runBlocking {
        val testStub = FakeGh0stOverlayService(
            rootAlive = true,
            partitionStatus = 1,
            upperDir = "/data/local/tmp/gh0stra1n/mnt_system/u",
            freeSpace = 52428800L
        )

        assertFalse(bridge.isConnected.value)
        assertFalse(bridge.isRootAlive.value)

        bridge.connection.onServiceConnected(
            ComponentName("com.orionisli.gh0stra1n", "Gh0stOverlayService"),
            testStub
        )

        assertTrue(bridge.isConnected.value)
        assertTrue(bridge.isRootAlive.value)
        assertNotNull(testStub.deathRecipient)

        assertTrue(bridge.checkRoot())
        assertEquals(1, bridge.getPartitionStatus("system"))
        assertTrue(bridge.isPartitionMounted("system"))
        assertEquals("/data/local/tmp/gh0stra1n/mnt_system/u", bridge.getUpperDir("system"))
        assertEquals(52428800L, bridge.getPartitionFreeSpace("system"))

        assertTrue(bridge.mountPartition("system"))
        assertTrue(bridge.unmountPartition("system"))
        assertTrue(bridge.syncStorage())
        assertTrue(testStub.syncStorageCalled)
        assertTrue(bridge.restartZygote())

        val states = bridge.refreshPartitionStates(listOf("system", "vendor"))
        assertEquals(1, states["system"])
        assertEquals(1, states["vendor"])
        assertEquals(1, bridge.partitionStates.value["system"])
    }

    @Test
    fun testDeathRecipientTriggersDisconnect() = runBlocking {
        val testStub = FakeGh0stOverlayService()

        bridge.connection.onServiceConnected(
            ComponentName("com.orionisli.gh0stra1n", "Gh0stOverlayService"),
            testStub
        )
        assertTrue(bridge.isConnected.value)
        assertTrue(bridge.isRootAlive.value)

        testStub.triggerCrash()

        assertFalse(bridge.isConnected.value)
        assertFalse(bridge.isRootAlive.value)
        assertTrue(testStub.unlinked)
    }

    @Test
    fun testLocalSuFallbackWhenDisconnected() = runBlocking {
        bridge.localCommandExecutor = { cmd ->
            when (cmd) {
                "id" -> Pair(0, "uid=0(root) gid=0(root)")
                "cat /proc/mounts" -> Pair(
                    0,
                    "ovl_gh0stra1n_system /system overlay rw,relatime," +
                        "lowerdir=/system,upperdir=/data/local/tmp/gh0stra1n/mnt_system/u,workdir=/w 0 0\n" +
                        "/dev/block/dm-1 /vendor erofs ro,relatime 0 0"
                )
                "df -k /system" -> Pair(
                    0,
                    "Filesystem 1K-blocks Used Available Use% Mounted on\n" +
                    "/dev/block/dm-0 102400 51200 51200 50% /system"
                )
                "sync" -> Pair(0, "")
                "setprop ctl.restart zygote" -> Pair(0, "")
                "echo hello" -> Pair(0, "hello")
                "umount -l /system" -> Pair(0, "")
                else -> Pair(-1, "")
            }
        }

        assertTrue(bridge.checkRoot())
        assertTrue(bridge.isRootAlive.value)

        assertEquals(1, bridge.getPartitionStatus("system"))
        assertEquals(0, bridge.getPartitionStatus("product"))
        assertTrue(bridge.isPartitionMounted("system"))
        assertFalse(bridge.isPartitionMounted("product"))

        assertEquals("/data/local/tmp/gh0stra1n/mnt_system/u", bridge.getUpperDir("system"))
        assertEquals(51200L * 1024L, bridge.getPartitionFreeSpace("system"))

        assertTrue(bridge.syncStorage())
        assertTrue(bridge.restartZygote())
        assertTrue(bridge.unmountPartition("system"))

        val (exit, out) = bridge.executeRootCommandWithOutput("echo hello")
        assertEquals(0, exit)
        assertEquals("hello", out)
    }

    @Test
    fun testFallbackWhenRemoteExceptionOccurs() = runBlocking {
        val throwingStub = object : IGh0stOverlayService.Stub() {
            override fun queryLocalInterface(descriptor: String): IInterface = this
            override fun isRootAlive(): Boolean = throw RemoteException("Dead")
            override fun getPartitionStatus(p: String?): Int = throw RemoteException("Dead")
            override fun getUpperDir(p: String?): String = throw RemoteException("Dead")
            override fun getPartitionFreeSpace(p: String?): Long = throw RemoteException("Dead")
            override fun mountPartition(p: String?): Boolean = throw RemoteException("Dead")
            override fun unmountPartition(p: String?): Boolean = throw RemoteException("Dead")
            override fun syncStorage() { throw RemoteException("Dead") }
            override fun restartZygote(): Boolean = throw RemoteException("Dead")
        }

        bridge.localCommandExecutor = { cmd ->
            when (cmd) {
                "id" -> Pair(0, "uid=0(root) gid=0(root)")
                "sync" -> Pair(0, "")
                "setprop ctl.restart zygote" -> Pair(0, "")
                else -> Pair(0, "")
            }
        }

        bridge.attachTestService(throwingStub)
        assertTrue(bridge.isConnected.value)

        assertTrue(bridge.checkRoot())
        assertTrue(bridge.syncStorage())
        assertTrue(bridge.restartZygote())
    }

    @Test
    fun testMockDefaultStatusWhenSuUnavailable() = runBlocking {
        bridge.localCommandExecutor = { _ -> Pair(-1, "su: not found") }

        assertFalse(bridge.checkRoot())
        assertFalse(bridge.isRootAlive.value)
        assertEquals(0, bridge.getPartitionStatus("system"))
        assertEquals(0L, bridge.getPartitionFreeSpace("system"))
        assertFalse(bridge.mountPartition("system"))
        assertFalse(bridge.unmountPartition("system"))
        assertFalse(bridge.syncStorage())
        assertFalse(bridge.restartZygote())
    }
}
