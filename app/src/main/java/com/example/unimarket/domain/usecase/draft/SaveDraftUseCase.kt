package com.example.unimarket.domain.usecase.draft

import com.example.unimarket.data.local.DraftProduct
import com.example.unimarket.domain.repository.LocalDraftRepository
import javax.inject.Inject

class SaveDraftUseCase @Inject constructor(
    private val repository: LocalDraftRepository
) {
    suspend operator fun invoke(draft: DraftProduct) {
        repository.saveDraft(draft)
    }
}
