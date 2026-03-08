package com.example.unimarket.domain.usecase

import android.net.Uri
import com.example.unimarket.domain.repository.ImageRepository
import javax.inject.Inject

class UploadImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    suspend operator fun invoke(imageUri: Uri): Result<String> {
        return imageRepository.uploadImage(imageUri)
    }
}