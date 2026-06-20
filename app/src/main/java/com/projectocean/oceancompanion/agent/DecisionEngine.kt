package com.projectocean.oceancompanion.agent

class DecisionEngine {
    fun decide(snapshot: ContextSnapshot): AgentAction {
        return when (snapshot.type) {
            ContextType.MathProblem -> AgentAction.SolveProblem
            ContextType.Document -> AgentAction.Summarize
            ContextType.Code -> AgentAction.ExplainCode
            ContextType.General -> AgentAction.WaitQuietly
        }
    }
}

enum class AgentAction { WaitQuietly, Summarize, SolveProblem, ExplainCode }
