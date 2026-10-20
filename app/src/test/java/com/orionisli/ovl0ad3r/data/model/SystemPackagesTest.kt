package com.orionisli.ovl0ad3r.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemPackagesTest {

    @Test
    fun `bootstrap libraries and tools are system managed`() {
        listOf(
            "base-layout", "pacman", "libalpm", "libarchive", "libcurl",
            "libc++", "openssl", "zlib", "zstd", "xz", "ncurses",
            "ca-certificates", "curl"
        ).forEach { assertTrue("$it should be system", SystemPackages.isSystem(it)) }
    }

    @Test
    fun `user tools stay installable`() {
        listOf("nano", "vim", "vi", "wget", "aria2")
            .forEach { assertFalse("$it should be user", SystemPackages.isSystem(it)) }
    }
}
