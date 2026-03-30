package com.example.unimarket.domain.usecase.ai

import com.example.unimarket.domain.model.AiListingInput
import com.example.unimarket.domain.model.AiListingSuggestion
import com.example.unimarket.domain.repository.AiRepository
import javax.inject.Inject

class GenerateListingSuggestionUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(input: AiListingInput): Result<AiListingSuggestion> {
        return aiRepository.generateListingSuggestion(input)
    }
}
