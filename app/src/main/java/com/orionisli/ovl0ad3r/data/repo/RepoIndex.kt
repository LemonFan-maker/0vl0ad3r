package com.orionisli.ovl0ad3r.data.repo

import com.orionisli.ovl0ad3r.data.model.PackageItem
import com.orionisli.ovl0ad3r.data.model.SystemPackages
import org.json.JSONArray
import org.json.JSONObject

data class RepoPackage(
    val name: String,
    val version: String,
    val description: String,
    val filename: String,
    val sizeBytes: Long,
    val installedSizeBytes: Long,
    val sha256: String,
    val homepage: String,
    val license: String,
    val architecture: String,
    val buildDateEpochSec: Long,
    val packager: String,
    val depends: List<String>
) {
    val publisher: String get() = packager.substringBefore('<').trim()

    fun toPackageItem(isInstalled: Boolean = false, hasUpdate: Boolean = false): PackageItem =
        PackageItem(
            id = "$name-$version",
            name = name,
            version = version,
            packageName = name,
            architecture = architecture,
            description = description,
            author = publisher,
            section = license,
            sizeBytes = sizeBytes,
            installedSizeBytes = installedSizeBytes,
            isInstalled = isInstalled,
            isSystemPackage = SystemPackages.isSystem(name),
            hasUpdate = hasUpdate && !SystemPackages.isSystem(name),
            filename = filename,
            sha256 = sha256,
            homepage = homepage,
            license = license,
            dependencies = depends
        )

    companion object {
        fun from(o: JSONObject): RepoPackage {
            val depends = mutableListOf<String>()
            when (val v = o.opt("depends")) {
                is JSONArray ->
                    for (i in 0 until v.length()) {
                        val d = v.optString(i)
                        if (d.isNotBlank()) depends += d
                    }
                is String ->
                    v.split(' ').forEach { if (it.isNotBlank()) depends += it }
            }
            return RepoPackage(
                name = o.optString("name"),
                version = o.optString("version"),
                description = o.optString("description"),
                filename = o.optString("filename"),
                sizeBytes = o.optLong("size"),
                installedSizeBytes = o.optLong("installedSize"),
                sha256 = o.optString("sha256"),
                homepage = o.optString("url"),
                license = o.optString("license"),
                architecture = o.optString("arch", "aarch64"),
                buildDateEpochSec = o.optLong("buildDate"),
                packager = o.optString("packager"),
                depends = depends
            )
        }
    }
}

data class RepoIndex(
    val schema: Int,
    val dbFilename: String,
    val packages: List<RepoPackage>
) {
    companion object {
        const val SUPPORTED_SCHEMA = 1

        fun parse(text: String): RepoIndex {
            val root = try {
                JSONObject(text)
            } catch (e: Exception) {
                throw RepoParseException("索引不是合法 JSON: ${e.message}")
            }
            val schema = root.optInt("schema", -1)
            if (schema != SUPPORTED_SCHEMA) {
                throw RepoParseException("索引 schema=$schema, 本客户端只支持 v$SUPPORTED_SCHEMA")
            }
            val arr = root.optJSONArray("packages")
                ?: throw RepoParseException("索引缺少 packages 数组")

            val list = ArrayList<RepoPackage>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val pkg = RepoPackage.from(o)
                if (pkg.name.isNotBlank() && pkg.filename.isNotBlank()) list += pkg
            }

            val declared = root.optInt("count", -1)
            if (declared >= 0 && declared != list.size) {
                throw RepoParseException("索引 count=$declared 与实际条目 ${list.size} 不符")
            }
            return RepoIndex(schema = schema, dbFilename = root.optString("db"), packages = list)
        }
    }
}

class RepoParseException(message: String) : Exception(message)
