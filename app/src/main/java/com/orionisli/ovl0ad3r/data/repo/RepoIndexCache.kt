package com.orionisli.ovl0ad3r.data.repo

import java.io.File

object RepoIndexCache {

    const val INDEX_FILE = "last_index.json"
    const val SOURCE_FILE = "last_index_source"

    data class Cached(
        val index: RepoIndex,
        val sourceUrl: String?
    )

    fun read(dir: File): Cached? {
        val indexFile = File(dir, INDEX_FILE)
        if (!indexFile.isFile) return null
        return try {
            val index = RepoIndex.parse(indexFile.readText())
            val sourceUrl = File(dir, SOURCE_FILE)
                .takeIf { it.isFile }
                ?.readText()
                ?.trim()
                ?.takeIf { it.startsWith("https://") }
            Cached(index, sourceUrl)
        } catch (_: Exception) {
            null
        }
    }

    fun write(dir: File, rawIndex: String, sourceUrl: String) {
        try {
            val indexFile = File(dir, INDEX_FILE)
            val tmp = File(dir, "$INDEX_FILE.tmp")
            tmp.writeText(rawIndex)
            if (indexFile.exists()) indexFile.delete()
            if (!tmp.renameTo(indexFile)) {
                indexFile.writeText(rawIndex)
                tmp.delete()
            }
            File(dir, SOURCE_FILE).writeText(sourceUrl)
        } catch (_: Exception) {
        }
    }
}
