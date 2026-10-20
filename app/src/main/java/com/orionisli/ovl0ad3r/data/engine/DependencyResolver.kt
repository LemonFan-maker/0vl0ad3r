package com.orionisli.ovl0ad3r.data.engine

import com.orionisli.ovl0ad3r.data.repo.RepoPackage

object DependencyResolver {

    data class Plan(
        val ordered: List<RepoPackage>,
        val missing: List<String>
    )

    fun resolve(
        targets: List<String>,
        catalog: List<RepoPackage>,
        installed: Set<String>
    ): Plan {
        val byName = catalog.associateBy { it.name }
        val ordered = LinkedHashSet<String>()
        val visiting = mutableSetOf<String>()
        val missing = LinkedHashSet<String>()

        fun visit(name: String, isTarget: Boolean) {
            if (name in ordered) return
            if (name in installed && !isTarget) return
            if (!visiting.add(name)) return
            val pkg = byName[name]
            if (pkg == null) {
                if (name !in installed) missing += name
                visiting.remove(name)
                return
            }
            pkg.depends.forEach { visit(it, false) }
            visiting.remove(name)
            ordered += name
        }

        targets.forEach { visit(it, true) }
        return Plan(
            ordered = ordered.mapNotNull { byName[it] },
            missing = missing.toList()
        )
    }
}
