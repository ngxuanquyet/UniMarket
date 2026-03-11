package com.example.unimarket.domain.usecase.draft

import com.example.unimarket.data.local.DraftProduct
import com.example.unimarket.domain.repository.LocalDraftRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDraftsUseCase @Inject constructor(
    private val repository: LocalDraftRepository
) {
    operator fun invoke(userId: String): Flow<List<DraftProduct>> {
        return repository.getDrafts(userId)
    }
}
