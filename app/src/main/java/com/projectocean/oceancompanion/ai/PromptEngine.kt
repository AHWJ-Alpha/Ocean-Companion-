package com.projectocean.oceancompanion.ai

class PromptEngine {
    fun buildScreenAnalysisPrompt(text: String, persona: Persona): AIRequest {
        val prompt = """
            请严格基于当前屏幕文字进行分析，不要补写屏幕上没有出现的信息。
            如果文字很少、噪声很大或无法判断，请明确说明“不确定”，不要强行推断。

            输出格式：
            1. 已识别事实：引用屏幕里的关键词或短句作为依据。
            2. 可能意图：只根据可见文字推断，并标注不确定点。
            3. 可执行动作：给出下一步可以直接做的操作或提问。
            4. 不确定内容：列出需要再次截图、打开文件或开启无障碍才能确认的部分。

            屏幕文字：
            $text
        """.trimIndent()
        return AIRequest(prompt = prompt, persona = persona, context = text)
    }

    fun buildProblemSolvingPrompt(text: String, persona: Persona): AIRequest {
        val prompt = """
            请解答或解释下面的学习题目。
            使用清晰步骤，必要时说明假设条件；如果题目缺少条件，请先指出缺失信息。

            题目内容：
            $text
        """.trimIndent()
        return AIRequest(prompt = prompt, persona = persona, context = text)
    }

    fun buildCompanionPrompt(screenText: String, customPersona: String, memory: String): AIRequest {
        val prompt = """
            你是桌面伴随式 AI。请根据当前屏幕信息主动说一句有用、简短的话。
            不要打扰用户，不要编造屏幕上没有的信息。

            自定义人格：
            $customPersona

            长时记忆：
            $memory

            当前屏幕文字：
            $screenText
        """.trimIndent()
        return AIRequest(prompt = prompt, persona = Persona.OceanNative, context = screenText)
    }

    fun buildProactiveCompanionPrompt(
        screenText: String,
        customPersona: String,
        memory: String,
        operationHistory: String,
        triggerApps: String,
        currentPackage: String,
        maxChars: Int
    ): AIRequest {
        val lengthRule = if (maxChars > 0) {
            "必须在 ${maxChars} 个中文字符以内完成表达，句子要完整，不要用省略号补尾，不要输出超过限制的内容。"
        } else {
            "弹幕长度不受限制，可以完整输出必要内容，但仍要自然、克制，不要啰嗦。"
        }
        val prompt = """
            你是 Ocean Companion，一个桌面伴随式 AI Agent。
            这不是聊天回复，而是一条短暂出现的桌面弹幕。

            请结合当前桌面内容、当前应用、最近操作/对话历史、长期记忆和人格说明，主动说一条新的、自然的、有场景感的话。

            要求：
            1. 优先回应用户正在看的内容或正在做的事。
            2. 如果屏幕文字足够明确，必须点出一个具体关键词、题目、任务或文件内容；如果文字不足，只能说明“画面文字不足”，不要编造。
            3. 必须有实际信息量，可以提醒、总结、提问或给下一步建议，但不能只是陪伴式寒暄。
            4. 不要说“某应用有新消息”“我捡重点说一下”“需要我总结吗”“随时叫我”这类空话。
            5. 最近历史只用于理解偏好和连续性，不要复述旧对话。
            6. 只输出一到两句，不要写标题、列表或系统解释。
            7. 语气必须遵循人格说明。
            8. $lengthRule

            人格说明：
            $customPersona

            触发应用关键词：
            $triggerApps

            当前应用包名：
            $currentPackage

            长期记忆：
            $memory

            最近操作/对话历史：
            $operationHistory

            当前桌面/文件文字：
            $screenText
        """.trimIndent()
        return AIRequest(prompt = prompt, persona = Persona.OceanNative, context = screenText)
    }

    fun buildLongConversationPrompt(
        userText: String,
        screenText: String,
        customPersona: String,
        memory: String,
        conversation: String,
        maxChars: Int = 0,
        searchContext: String = ""
    ): AIRequest {
        val lengthRule = if (maxChars > 0) "本次回复控制在 ${maxChars} 个中文字符以内。" else "本次回复不设硬性字数限制。"
        val searchRule = if (searchContext.isBlank()) {
            "没有可用联网搜索结果；不要假装已经联网。"
        } else {
            "下面提供了联网搜索结果。需要引用外部信息时，请优先使用这些结果，并在句末保留来源标题或链接。"
        }
        val prompt = """
            你是 Ocean Companion，一个常驻桌面的伴随式 AI。
            请直接回答用户，并主动结合当前桌面/文件文字、长期记忆、最近对话和可用搜索结果。
            如果屏幕文字为空或不可用，必须说明依据不足，只基于长期记忆、搜索结果和当前对话继续，不要编造屏幕内容。
            回答不能只“说说看法”，要尽量做到：先判断依据，再给明确动作，例如提炼、计算、改写、列步骤、生成可执行方案或指出下一步操作。
            $lengthRule
            $searchRule

            长对话窗口支持 Markdown。如内容需要结构化，可以使用标题、粗体、列表、行内代码或代码块。

            自定义人格：
            $customPersona

            长期记忆：
            $memory

            当前桌面/文件文字：
            $screenText

            搜索结果：
            ${searchContext.ifBlank { "无" }}

            最近对话：
            $conversation

            用户刚刚说：
            $userText
        """.trimIndent()
        return AIRequest(prompt = prompt, persona = Persona.OceanNative, context = screenText)
    }
}
