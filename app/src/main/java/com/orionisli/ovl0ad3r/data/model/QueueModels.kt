package com.orionisli.ovl0ad3r.data.model

enum class QueueAction {
    INSTALL,
    UPGRADE,
    REMOVE
}

data class QueueTask(
    val packageItem: PackageItem,
    val action: QueueAction,
    val isDependency: Boolean = false,
    val parentPkgId: String? = null
)

data class QueueState(
    val tasks: List<QueueTask> = emptyList(),
    val totalDownloadBytes: Long = 0L,
    val isExecuting: Boolean = false
) {
    val count: Int get() = tasks.size
    val isEmpty: Boolean get() = tasks.isEmpty()
    val installTasks: List<QueueTask> get() = tasks.filter { it.action == QueueAction.INSTALL }
    val upgradeTasks: List<QueueTask> get() = tasks.filter { it.action == QueueAction.UPGRADE }
    val removeTasks: List<QueueTask> get() = tasks.filter { it.action == QueueAction.REMOVE }
}
