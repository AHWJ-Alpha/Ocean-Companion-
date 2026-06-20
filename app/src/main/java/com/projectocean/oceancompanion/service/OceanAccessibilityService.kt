package com.projectocean.oceancompanion.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.projectocean.oceancompanion.agent.ContextAnalyzer
import com.projectocean.oceancompanion.agent.SharedScreenContext

class OceanAccessibilityService : AccessibilityService() {
    private val analyzer = ContextAnalyzer()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventPackage = event?.packageName?.toString().orEmpty()
        SharedScreenContext.updatePackage(eventPackage, source = "accessibility_event")
        val text = rootInActiveWindow?.let(::collectText).orEmpty()
        if (text.isNotBlank()) {
            analyzer.updateVisibleText(text)
            SharedScreenContext.update(eventPackage.ifBlank { SharedScreenContext.packageName }, text, source = "accessibility")
        }
    }

    override fun onInterrupt() = Unit

    private fun collectText(node: AccessibilityNodeInfo): String {
        val own = listOfNotNull(node.text, node.contentDescription).joinToString(" ")
        val children = buildString {
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { append(' ').append(collectText(it)) }
            }
        }
        return "$own $children".trim()
    }
}
