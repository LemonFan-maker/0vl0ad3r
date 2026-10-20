package com.orionisli.ovl0ad3r.data.model

enum class ReloadLevel(val value: String) {
    NONE("none"),
    UICACHE("uicache"),
    ZYGOTE("zygote"),
    REBOOT("reboot");

    companion object {
        fun fromString(value: String?): ReloadLevel =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: NONE
    }
}

enum class PackageFilter {
    ALL,
    USER,
    SYSTEM
}

data class PackageItem(
    val id: String,
    val name: String,
    val version: String,
    val packageName: String = id,
    val architecture: String = "aarch64",
    val category: String = "",
    val sizeBytes: Long = 0L,
    val installedSizeBytes: Long = 0L,
    val description: String = "",
    val author: String = "",
    val section: String = "",
    val iconUrl: String? = null,
    val targetPartition: String = "system",
    val reloadLevel: ReloadLevel = ReloadLevel.NONE,
    val isInstalled: Boolean = false,
    val isSystemPackage: Boolean = false,
    val hasUpdate: Boolean = false,
    val filename: String = "",
    val sha256: String = "",
    val homepage: String = "",
    val license: String = "",
    val dependencies: List<String> = emptyList()
)

object SystemPackages {
    private val names = setOf(
        "base-layout",
        "pacman",
        "libalpm",
        "libarchive",
        "libcurl",
        "libc++",
        "openssl",
        "zlib",
        "zstd",
        "xz",
        "ncurses",
        "ca-certificates",
        "curl"
    )

    fun isSystem(name: String): Boolean = name in names
}

data class DepictionModel(
    val headerTitle: String,
    val subtitle: String = "",
    val bannerUrl: String? = null,
    val screenshots: List<String> = emptyList(),
    val specs: Map<String, String> = emptyMap(),
    val changelog: String = "",
    val markdownBody: String = "",
    val dependencies: List<String> = emptyList()
)
