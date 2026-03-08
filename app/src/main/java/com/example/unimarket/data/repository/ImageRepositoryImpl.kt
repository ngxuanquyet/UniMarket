package com.example.unimarket.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.unimarket.data.api.ImageApiService
import com.example.unimarket.domain.repository.ImageRepository
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class ImageRepositoryImpl @Inject constructor(
    private val imageApiService: ImageApiService,
    private val remoteConfig: FirebaseRemoteConfig,
    @ApplicationContext private val context: Context
): ImageRepository {
    override suspend fun uploadImage(image: Uri): Result<String> {
        try {
            // First, ensure config is fetched
            remoteConfig.fetchAndActivate().await()
            val apiKey = remoteConfig.getString("KEY_UPLOAD_IMAGE")
            Log.d("RemoteConfig", "Fetched Key: $apiKey")

            if (apiKey.isEmpty()) {
                return Result.failure(Exception("ImgBB API Key is missing from Remote Config"))
            }

            // Convert Uri to File
            val file = getFileFromUri(context, image)
                ?: return Result.failure(Exception("Failed to process image Uri"))

            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

            val response = imageApiService.uploadImage(key = apiKey, image = imagePart)

            return if (response.isSuccessful && response.body() != null) {
                val imageUrl = response.body()?.data?.url
                if (imageUrl != null) {
                    Result.success(imageUrl)
                } else {
                    Result.failure(Exception("Image URL not found in the response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ImgBB_API", "Upload failed. Code: ${response.code()}, Error: $errorBody")
                Result.failure(Exception("Image upload failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.use { input ->
            val tempFile = File(context.cacheDir, "upload_image_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
            return tempFile
        }
        return null
    }
}