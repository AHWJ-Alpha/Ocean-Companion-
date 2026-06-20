package com.projectocean.oceancompanion.ai

class DeepSeekClient(private val apiKey: String) : AIClient {
    override suspend fun complete(request: AIRequest): AIResponse {
        return AIResponse(
            text = "DeepSeek 适配器已配置。正式使用前请补全 Retrofit 请求体与鉴权参数。",
            provider = "deepseek"
        )
    }
}
