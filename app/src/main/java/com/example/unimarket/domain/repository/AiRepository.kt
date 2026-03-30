package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.AiListingInput
import com.example.unimarket.domain.model.AiListingSuggestion

interface AiRepository {
    suspend fun generateListingSuggestion(input: AiListingInput): Result<AiListingSuggestion>
}
