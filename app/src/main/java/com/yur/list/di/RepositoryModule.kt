package com.yur.list.di

import com.yur.list.data.repository.LocalProductsRepositoryImpl
import com.yur.list.domain.repository.ProductsRepository
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
    abstract fun bindProductsRepository(
        impl: LocalProductsRepositoryImpl
    ): ProductsRepository
}
