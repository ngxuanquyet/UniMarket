package com.example.unimarket.domain.repository

import com.example.unimarket.data.local.DraftProduct
import kotlinx.coroutines.flow.Flow

interface LocalDraftRepository {
    suspend fun saveDraft(draft: DraftProduct)
    fun getDrafts(userId: String): Flow<List<DraftProduct>>
    suspend fun getDraftById(draftId: String): DraftProduct?
    suspend fun deleteDraft(draftId: String)
}
