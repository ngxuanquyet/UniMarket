package com.example.unimarket.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftProduct)

    @Query("SELECT * FROM drafts WHERE userId = :userId ORDER BY lastModified DESC")
    fun getAllDrafts(userId: String): Flow<List<DraftProduct>>

    @Query("SELECT * FROM drafts WHERE id = :draftId")
    suspend fun getDraftById(draftId: String): DraftProduct?

    @Query("DELETE FROM drafts WHERE id = :draftId")
    suspend fun deleteDraftPath(draftId: String)
}
