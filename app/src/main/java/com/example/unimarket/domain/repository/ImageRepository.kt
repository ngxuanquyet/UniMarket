package com.example.unimarket.domain.repository

import android.net.Uri

interface ImageRepository {
    suspend fun uploadImage(image: Uri) : Result<String>
}