package com.example.unimarket.di

import com.example.unimarket.data.repository.InMemoryCartRepositoryImpl
import com.example.unimarket.data.repository.FirebaseChatRepositoryImpl
import com.example.unimarket.domain.repository.AuthRepository
import com.example.unimarket.domain.repository.CartRepository
import com.example.unimarket.domain.repository.ChatRepository
import com.example.unimarket.domain.repository.ImageRepository
import com.example.unimarket.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.example.unimarket.data.repository.FirebaseProductRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        firebaseProductRepositoryImpl: FirebaseProductRepositoryImpl
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindCartRepository(
        inMemoryCartRepositoryImpl: InMemoryCartRepositoryImpl
    ): CartRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        firebaseChatRepositoryImpl: FirebaseChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: com.example.unimarket.data.repository.AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindImageRepository(
        imageRepositoryImpl: com.example.unimarket.data.repository.ImageRepositoryImpl
    ): ImageRepository

    @Binds
    @Singleton
    abstract fun bindLocalDraftRepository(
        localDraftRepositoryImpl: com.example.unimarket.data.repository.LocalDraftRepositoryImpl
    ): com.example.unimarket.domain.repository.LocalDraftRepository
}
