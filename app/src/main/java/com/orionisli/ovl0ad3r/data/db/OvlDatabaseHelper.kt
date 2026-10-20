package com.orionisli.ovl0ad3r.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.orionisli.ovl0ad3r.data.model.PackageItem
import com.orionisli.ovl0ad3r.data.model.ReloadLevel
import com.orionisli.ovl0ad3r.data.model.RepoSource

data class PackageFileRecord(
    val path: String,
    val pkgId: String,
    val sha256: String = "",
    val isSymlink: Boolean = false,
    val targetLink: String? = null,
    val overridesLower: Boolean = false
)

class OvlDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS packages (
                pkg_id TEXT PRIMARY KEY,
                name TEXT,
                version TEXT NOT NULL,
                install_time INTEGER NOT NULL,
                status TEXT NOT NULL,
                description TEXT,
                size_bytes INTEGER NOT NULL,
                is_base_toolchain INTEGER DEFAULT 0,
                author TEXT,
                section TEXT,
                target_partition TEXT DEFAULT 'system',
                reload_level TEXT DEFAULT 'none'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS package_files (
                path TEXT PRIMARY KEY,
                pkg_id TEXT NOT NULL,
                sha256 TEXT NOT NULL,
                is_symlink INTEGER NOT NULL,
                target_link TEXT,
                overrides_lower INTEGER DEFAULT 0,
                FOREIGN KEY(pkg_id) REFERENCES packages(pkg_id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS repositories (
                repo_id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                url TEXT NOT NULL,
                is_official INTEGER DEFAULT 0,
                is_enabled INTEGER DEFAULT 1,
                package_count INTEGER DEFAULT 0,
                last_sync_time INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS package_files")
        db.execSQL("DROP TABLE IF EXISTS packages")
        db.execSQL("DROP TABLE IF EXISTS repositories")
        onCreate(db)
    }

    fun queryInstalledPackages(): List<PackageItem> {
        val list = mutableListOf<PackageItem>()
        val db = readableDatabase
        val cursor = db.query(
            "packages",
            null,
            "status = ?",
            arrayOf("installed"),
            null,
            null,
            "install_time DESC"
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("pkg_id")
            val nameIdx = it.getColumnIndex("name")
            val verIdx = it.getColumnIndexOrThrow("version")
            val descIdx = it.getColumnIndex("description")
            val sizeIdx = it.getColumnIndexOrThrow("size_bytes")
            val authorIdx = it.getColumnIndex("author")
            val sectionIdx = it.getColumnIndex("section")
            val partIdx = it.getColumnIndex("target_partition")
            val reloadIdx = it.getColumnIndex("reload_level")

            while (it.moveToNext()) {
                val pkgId = it.getString(idIdx)
                val name = if (nameIdx >= 0) it.getString(nameIdx) ?: pkgId else pkgId
                val version = it.getString(verIdx)
                val description = if (descIdx >= 0) it.getString(descIdx) ?: "" else ""
                val sizeBytes = it.getLong(sizeIdx)
                val author = if (authorIdx >= 0) it.getString(authorIdx) ?: "" else ""
                val section = if (sectionIdx >= 0) it.getString(sectionIdx) ?: "" else ""
                val targetPartition = if (partIdx >= 0) it.getString(partIdx) ?: "system" else "system"
                val reloadLevelStr = if (reloadIdx >= 0) it.getString(reloadIdx) else null

                list.add(
                    PackageItem(
                        id = pkgId,
                        name = name,
                        version = version,
                        packageName = name.ifBlank { pkgId },
                        description = description,
                        sizeBytes = sizeBytes,
                        installedSizeBytes = sizeBytes,
                        author = author,
                        section = section,
                        targetPartition = targetPartition,
                        reloadLevel = ReloadLevel.fromString(reloadLevelStr),
                        isInstalled = true
                    )
                )
            }
        }
        return list
    }

    fun recordInstallation(
        pkg: PackageItem,
        files: List<PackageFileRecord> = emptyList(),
        isBaseToolchain: Boolean = false
    ): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val pkgValues = ContentValues().apply {
                put("pkg_id", pkg.packageName)
                put("name", pkg.name)
                put("version", pkg.version)
                put("install_time", System.currentTimeMillis())
                put("status", "installed")
                put("description", pkg.description)
                put("size_bytes", if (pkg.installedSizeBytes > 0) pkg.installedSizeBytes else pkg.sizeBytes)
                put("is_base_toolchain", if (isBaseToolchain) 1 else 0)
                put("author", pkg.author)
                put("section", pkg.section)
                put("target_partition", pkg.targetPartition)
                put("reload_level", pkg.reloadLevel.value)
            }
            db.insertWithOnConflict("packages", null, pkgValues, SQLiteDatabase.CONFLICT_REPLACE)

            for (file in files) {
                val fileValues = ContentValues().apply {
                    put("path", file.path)
                    put("pkg_id", pkg.packageName)
                    put("sha256", file.sha256)
                    put("is_symlink", if (file.isSymlink) 1 else 0)
                    put("target_link", file.targetLink)
                    put("overrides_lower", if (file.overridesLower) 1 else 0)
                }
                db.insertWithOnConflict("package_files", null, fileValues, SQLiteDatabase.CONFLICT_REPLACE)
            }

            db.setTransactionSuccessful()
            true
        } catch (_: Exception) {
            false
        } finally {
            db.endTransaction()
        }
    }

    fun recordUninstallation(pkgId: String): List<PackageFileRecord> {
        val removedFiles = mutableListOf<PackageFileRecord>()
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.query(
                "package_files",
                null,
                "pkg_id = ?",
                arrayOf(pkgId),
                null,
                null,
                null
            )
            cursor.use {
                val pathIdx = it.getColumnIndexOrThrow("path")
                val shaIdx = it.getColumnIndexOrThrow("sha256")
                val symIdx = it.getColumnIndexOrThrow("is_symlink")
                val targetIdx = it.getColumnIndex("target_link")
                val ovrIdx = it.getColumnIndex("overrides_lower")

                while (it.moveToNext()) {
                    removedFiles.add(
                        PackageFileRecord(
                            path = it.getString(pathIdx),
                            pkgId = pkgId,
                            sha256 = it.getString(shaIdx),
                            isSymlink = it.getInt(symIdx) == 1,
                            targetLink = if (targetIdx >= 0) it.getString(targetIdx) else null,
                            overridesLower = if (ovrIdx >= 0) it.getInt(ovrIdx) == 1 else false
                        )
                    )
                }
            }

            db.delete("packages", "pkg_id = ?", arrayOf(pkgId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return removedFiles
    }

    fun queryPackageFiles(pkgId: String): List<PackageFileRecord> {
        val files = mutableListOf<PackageFileRecord>()
        val db = readableDatabase
        val cursor = db.query(
            "package_files",
            null,
            "pkg_id = ?",
            arrayOf(pkgId),
            null,
            null,
            "path ASC"
        )
        cursor.use {
            val pathIdx = it.getColumnIndexOrThrow("path")
            val shaIdx = it.getColumnIndexOrThrow("sha256")
            val symIdx = it.getColumnIndexOrThrow("is_symlink")
            val targetIdx = it.getColumnIndex("target_link")
            val ovrIdx = it.getColumnIndex("overrides_lower")

            while (it.moveToNext()) {
                files.add(
                    PackageFileRecord(
                        path = it.getString(pathIdx),
                        pkgId = pkgId,
                        sha256 = it.getString(shaIdx),
                        isSymlink = it.getInt(symIdx) == 1,
                        targetLink = if (targetIdx >= 0) it.getString(targetIdx) else null,
                        overridesLower = if (ovrIdx >= 0) it.getInt(ovrIdx) == 1 else false
                    )
                )
            }
        }
        return files
    }

    fun queryRepositories(): List<RepoSource> {
        val list = mutableListOf<RepoSource>()
        val db = readableDatabase
        val cursor = db.query("repositories", null, null, null, null, null, "repo_id ASC")
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("repo_id")
            val nameIdx = it.getColumnIndexOrThrow("name")
            val urlIdx = it.getColumnIndexOrThrow("url")
            val offIdx = it.getColumnIndex("is_official")
            val enIdx = it.getColumnIndex("is_enabled")
            val countIdx = it.getColumnIndex("package_count")
            val syncIdx = it.getColumnIndex("last_sync_time")

            while (it.moveToNext()) {
                list.add(
                    RepoSource(
                        id = it.getString(idIdx),
                        name = it.getString(nameIdx),
                        url = it.getString(urlIdx),
                        isOfficial = if (offIdx >= 0) it.getInt(offIdx) == 1 else false,
                        isEnabled = if (enIdx >= 0) it.getInt(enIdx) == 1 else true,
                        packageCount = if (countIdx >= 0) it.getInt(countIdx) else 0,
                        lastSyncTime = if (syncIdx >= 0) it.getLong(syncIdx) else 0L
                    )
                )
            }
        }
        return list
    }

    fun upsertRepository(repo: RepoSource): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("repo_id", repo.id)
            put("name", repo.name)
            put("url", repo.url)
            put("is_official", if (repo.isOfficial) 1 else 0)
            put("is_enabled", if (repo.isEnabled) 1 else 0)
            put("package_count", repo.packageCount)
            put("last_sync_time", repo.lastSyncTime)
        }
        val rows = db.insertWithOnConflict("repositories", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return rows != -1L
    }

    fun deleteRepository(repoId: String): Boolean {
        val db = writableDatabase
        return db.delete("repositories", "repo_id = ?", arrayOf(repoId)) > 0
    }

    companion object {
        const val DATABASE_NAME = "0vl0ad3r.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var instance: OvlDatabaseHelper? = null

        fun getInstance(context: Context): OvlDatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: OvlDatabaseHelper(context.applicationContext).also { instance = it }
            }
    }
}
