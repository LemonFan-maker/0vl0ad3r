package com.orionisli.ovl0ad3r.data.engine

import com.orionisli.ovl0ad3r.data.model.PackageItem
import com.orionisli.ovl0ad3r.data.model.SystemPackages
import com.orionisli.ovl0ad3r.data.repo.RepoPackage

object InstalledPackagesReader {

    private val WHITESPACE = Regex("\\s+")
    private val PACKAGE_NAME = Regex("^[A-Za-z0-9@._+-]+$")

    private fun isDiagnostic(firstToken: String): Boolean =
        firstToken.endsWith(":") ||
            firstToken.equals("warning", ignoreCase = true) ||
            firstToken.equals("error", ignoreCase = true)

    fun parse(output: String, catalog: List<RepoPackage>): List<PackageItem> {
        val byName = catalog.associateBy { it.name }
        return output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(WHITESPACE, limit = 2)
                val name = parts.getOrNull(0).orEmpty()
                if (name.isEmpty() || isDiagnostic(name) || !PACKAGE_NAME.matches(name)) {
                    return@mapNotNull null
                }
                val version = parts.getOrNull(1).orEmpty()
                val repo = byName[name]
                if (repo == null) {
                    PackageItem(
                        id = "$name-$version",
                        name = name,
                        version = version,
                        packageName = name,
                        isInstalled = true,
                        isSystemPackage = SystemPackages.isSystem(name)
                    )
                } else {
                    repo.toPackageItem(
                        isInstalled = true,
                        hasUpdate = repo.version != version
                    ).copy(id = "$name-$version", version = version)
                }
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    fun parseLocalDbEntries(output: String, catalog: List<RepoPackage>): List<PackageItem> {
        val lines = output.lineSequence()
            .map { it.trim().trimEnd('/') }
            .filter { it.isNotEmpty() && it != "ALPM_DB_VERSION" && !it.startsWith(".") }
            .mapNotNull { dirName ->
                val relDash = dirName.lastIndexOf('-')
                if (relDash <= 0) return@mapNotNull null
                val verDash = dirName.lastIndexOf('-', relDash - 1)
                if (verDash <= 0) return@mapNotNull null
                val name = dirName.substring(0, verDash)
                val version = dirName.substring(verDash + 1)
                "$name $version"
            }
            .joinToString("\n")
        return parse(lines, catalog)
    }
}
