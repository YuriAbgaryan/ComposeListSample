package com.yur.list.di

import android.content.Context
import androidx.room.Room
import com.yur.list.data.local.ProductDao
import com.yur.list.data.local.ProductDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    const val DB_NAME = "products.db"

    @Provides
    @Singleton
    fun provideProductDatabase(@ApplicationContext context: Context): ProductDatabase =
        Room.databaseBuilder(
            context,
            ProductDatabase::class.java,
            DB_NAME
        ).build()

    @Provides
    @Singleton
    fun provideProductDao(database: ProductDatabase): ProductDao =
        database.productDao()
}
