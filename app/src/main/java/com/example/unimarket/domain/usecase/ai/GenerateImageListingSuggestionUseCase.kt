package com.example.unimarket.domain.usecase.ai

import com.example.unimarket.domain.model.AiImageListingInput
import com.example.unimarket.domain.model.AiImageListingSuggestion
import com.example.unimarket.domain.repository.AiRepository
import javax.inject.Inject

class GenerateImageListingSuggestionUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(input: AiImageListingInput): Result<AiImageListingSuggestion> {
        return aiRepository.generateListingSuggestionFromImage(input)
    }
}
