package com.projectocean.oceancompanion.ai

class BailianClient(private val apiKey: String) : AIClient {
    override suspend fun complete(request: AIRequest): AIResponse {
        return AIResponse(
            text = "阿里云百炼适配器已配置。正式使用前请接入 DashScope 兼容调用。",
            provider = "bailian"
        )
    }
}
