package com.example.unimarket.data.repository

import android.content.Context
import android.net.Uri
import com.example.unimarket.data.local.DraftProduct
import com.example.unimarket.data.local.DraftProductDao
import com.example.unimarket.domain.repository.LocalDraftRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class LocalDraftRepositoryImpl @Inject constructor(
    private val dao: DraftProductDao,
    @ApplicationContext private val context: Context
) : LocalDraftRepository {

    override suspend fun saveDraft(draft: DraftProduct) {
        val persistedImageUrls = draft.imageUrls.map { urlString ->
            if (urlString.startsWith("content://")) {
                persistUriToInternalStorage(Uri.parse(urlString))?.toString() ?: urlString
            } else {
                urlString
            }
        }

        val draftToSave = draft.copy(imageUrls = persistedImageUrls, lastModified = System.currentTimeMillis())
        dao.insertDraft(draftToSave)
    }

    override fun getDrafts(userId: String): Flow<List<DraftProduct>> {
        return dao.getAllDrafts(userId)
    }

    override suspend fun getDraftById(draftId: String): DraftProduct? {
        return dao.getDraftById(draftId)
    }

    override suspend fun deleteDraft(draftId: String) {
        val draft = dao.getDraftById(draftId)
        draft?.imageUrls?.forEach { urlString ->
            if (urlString.startsWith("file://")) {
                try {
                    val file = File(Uri.parse(urlString).path!!)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    // Ignore deletion errors for cache files
                }
            }
        }
        dao.deleteDraftPath(draftId)
    }

    private suspend fun persistUriToInternalStorage(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        return@withContext inputStream?.use { input ->
            val draftsDir = File(context.cacheDir, "draft_images")
            if (!draftsDir.exists()) {
                draftsDir.mkdirs()
            }
            // Use current time to avoid collisions, but hashing uri might be better for deduplication
            val tempFile = File(draftsDir, "draft_img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
            Uri.fromFile(tempFile)
        }
    }
}
