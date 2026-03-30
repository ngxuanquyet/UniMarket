package com.example.unimarket.data.api.model

data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate>? = null
)

data class GeminiCandidate(
    val content: GeminiCandidateContent? = null
)

data class GeminiCandidateContent(
    val parts: List<GeminiCandidatePart>? = null
)

data class GeminiCandidatePart(
    val text: String? = null
)
