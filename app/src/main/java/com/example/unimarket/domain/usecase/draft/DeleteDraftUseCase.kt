package com.example.unimarket.domain.usecase.draft

import com.example.unimarket.domain.repository.LocalDraftRepository
import javax.inject.Inject

class DeleteDraftUseCase @Inject constructor(
    private val repository: LocalDraftRepository
) {
    suspend operator fun invoke(draftId: String) {
        repository.deleteDraft(draftId)
    }
}
