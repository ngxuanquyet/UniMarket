package com.example.unimarket.di

import com.example.unimarket.data.repository.FakeCartRepositoryImpl
import com.example.unimarket.data.repository.FakeProductRepositoryImpl
import com.example.unimarket.domain.repository.AuthRepository
import com.example.unimarket.domain.repository.CartRepository
import com.example.unimarket.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        fakeProductRepositoryImpl: FakeProductRepositoryImpl
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindCartRepository(
        fakeCartRepositoryImpl: FakeCartRepositoryImpl
    ): CartRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: com.example.unimarket.data.repository.AuthRepositoryImpl
    ): AuthRepository
}
