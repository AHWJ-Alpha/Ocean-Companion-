package com.projectocean.oceancompanion.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SharedScreenContext {
    @Volatile var visibleText: String = ""
    @Volatile var packageName: String = ""
    @Volatile var updatedAt: Long = 0L
    @Volatile var source: String = "idle"

    private val _snapshot = MutableStateFlow(ScreenContextSnapshot())
    val snapshot: StateFlow<ScreenContextSnapshot> = _snapshot

    fun update(packageName: String, text: String, source: String = "accessibility") {
        this.packageName = packageName
        this.visibleText = text.take(16000)
        this.updatedAt = System.currentTimeMillis()
        this.source = source
        _snapshot.value = ScreenContextSnapshot(
            packageName = this.packageName,
            visibleText = this.visibleText,
            updatedAt = this.updatedAt,
            source = this.source
        )
    }

    fun updatePackage(packageName: String, source: String = "accessibility") {
        if (packageName.isBlank()) return
        this.packageName = packageName
        this.updatedAt = System.currentTimeMillis()
        this.source = source
        _snapshot.value = ScreenContextSnapshot(
            packageName = this.packageName,
            visibleText = this.visibleText,
            updatedAt = this.updatedAt,
            source = this.source
        )
    }
}

data class ScreenContextSnapshot(
    val packageName: String = "",
    val visibleText: String = "",
    val updatedAt: Long = 0L,
    val source: String = "idle"
)
