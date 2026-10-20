package com.orionisli.ovl0ad3r.data.model

data class RepoSource(
    val id: String,
    val name: String,
    val url: String,
    val isOfficial: Boolean = false,
    val isEnabled: Boolean = true,
    val isPacmanMirror: Boolean = true,
    val packageCount: Int = 0,
    val lastSyncTime: Long = 0L
)
