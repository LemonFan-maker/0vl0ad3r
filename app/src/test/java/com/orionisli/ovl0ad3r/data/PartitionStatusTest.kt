package com.orionisli.ovl0ad3r.data

import com.orionisli.ovl0ad3r.data.ipc.GhostRainBridge
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PartitionStatusTest {

    private lateinit var bridge: GhostRainBridge

    @Before
    fun setUp() {
        bridge = GhostRainBridge.getInstance()
        bridge.resetForTesting()
        bridge.setAutoRebind(false)
    }

    @After
    fun tearDown() {
        bridge.resetForTesting()
        bridge.setAutoRebind(true)
    }

    private fun withMounts(mounts: String, block: suspend () -> Unit) {
        bridge.localCommandExecutor = { cmd ->
            if (cmd == "cat /proc/mounts") Pair(0, mounts) else Pair(-1, "")
        }
        runBlocking { block() }
    }

    @Test
    fun stockErofsPartitionsAreNotReportedAsOverlayMounted() = withMounts(
        STOCK_MOUNTS
    ) {
        assertEquals(0, bridge.getPartitionStatus("system"))
        assertEquals(0, bridge.getPartitionStatus("vendor"))
        assertEquals(0, bridge.getPartitionStatus("product"))
        assertFalse(bridge.isPartitionMounted("vendor"))
        assertFalse(bridge.isPartitionMounted("product"))
    }

    @Test
    fun liveGh0stra1nOverlayIsReportedAsMounted() = withMounts(
        STOCK_MOUNTS + "\n" + LIVE_OVERLAY_SYSTEM
    ) {
        assertEquals(1, bridge.getPartitionStatus("system"))
        assertTrue(bridge.isPartitionMounted("system"))
        assertEquals(
            "/data/local/gh0stra1n-overlayfs/mnt_system/u",
            bridge.getUpperDir("system")
        )
    }

    @Test
    fun stockVendorMountNeverYieldsAnUpperDir() = withMounts(STOCK_MOUNTS) {
        assertEquals(
            "/data/local/tmp/gh0stra1n/mnt_vendor/u",
            bridge.getUpperDir("vendor")
        )
    }

    @Test
    fun unrelatedOverlayOnAnotherMountPointIsNotAttributed() = withMounts(
        STOCK_MOUNTS + "\n" + FOREIGN_OVERLAY_VENDOR
    ) {
        assertEquals(0, bridge.getPartitionStatus("vendor"))
    }

    @Test
    fun unreadableMountsReportUnmountedRatherThanMounted() {
        bridge.localCommandExecutor = { cmd ->
            if (cmd == "cat /proc/mounts") Pair(-1, "") else Pair(-1, "")
        }
        runBlocking {
            assertEquals(0, bridge.getPartitionStatus("vendor"))
        }
    }

    private companion object {
        val STOCK_MOUNTS = """
            tmpfs /dev tmpfs rw,seclabel,nosuid,relatime,size=5777816k 0 0
            /dev/block/dm-15 / erofs ro,seclabel,relatime,user_xattr,acl 0 0
            /dev/block/dm-16 /system_ext erofs ro,seclabel,relatime,user_xattr,acl 0 0
            /dev/block/dm-17 /vendor erofs ro,seclabel,relatime,user_xattr,acl 0 0
            /dev/block/dm-18 /product erofs ro,seclabel,relatime,user_xattr,acl 0 0
            /dev/block/dm-19 /odm erofs ro,seclabel,relatime,user_xattr,acl 0 0
        """.trimIndent()

        const val LIVE_OVERLAY_SYSTEM =
            "ovl_gh0stra1n_system /system overlay rw,relatime," +
                "lowerdir=/system:/product," +
                "upperdir=/data/local/gh0stra1n-overlayfs/mnt_system/u," +
                "workdir=/data/local/gh0stra1n-overlayfs/mnt_system/w 0 0"

        const val FOREIGN_OVERLAY_VENDOR =
            "some_other_ovl /vendor overlay rw,relatime," +
                "lowerdir=/vendor,upperdir=/data/other/u,workdir=/data/other/w 0 0"
    }
}
