package com.projectocean.oceancompanion.agent

class ContextAnalyzer {
    fun updateVisibleText(text: String): ContextSnapshot {
        val type = when {
            Regex("integral|derivative|matrix|\\u51fd\\u6570|\\u79ef\\u5206|\\u5bfc\\u6570", RegexOption.IGNORE_CASE).containsMatchIn(text) -> ContextType.MathProblem
            Regex("article|abstract|\\u8bba\\u6587|\\u6458\\u8981|\\u5173\\u952e\\u8bcd", RegexOption.IGNORE_CASE).containsMatchIn(text) -> ContextType.Document
            Regex("\\b(class|fun|public|private)\\b|\\u7b97\\u6cd5|\\u4ee3\\u7801", RegexOption.IGNORE_CASE).containsMatchIn(text) -> ContextType.Code
            else -> ContextType.General
        }
        return ContextSnapshot(type = type, visibleText = text.take(4000))
    }
}

data class ContextSnapshot(val type: ContextType, val visibleText: String)

enum class ContextType { General, MathProblem, Document, Code }
