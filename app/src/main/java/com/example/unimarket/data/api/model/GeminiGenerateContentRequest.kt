package com.example.unimarket.data.api.model

import com.google.gson.annotations.SerializedName

data class GeminiGenerateContentRequest(
    @SerializedName("system_instruction")
    val systemInstruction: GeminiContent? = null,
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null,
    val thinkingConfig: GeminiThinkingConfig? = null
)

data class GeminiThinkingConfig(
    val thinkingBudget: Int
)
