package com.yur.list.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.yur.list.data.local.ProductDao
import com.yur.list.data.local.toDomain
import com.yur.list.data.local.toEntity
import com.yur.list.data.remote.api.ProductsApi
import com.yur.list.data.remote.api.ProductsPagingSource
import com.yur.list.domain.model.Product
import com.yur.list.domain.repository.ProductsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first implementation of [ProductsRepository] backed by Room.
 *
 * All cache logic lives here — callers and the domain layer know nothing about it.
 *
 * Strategy per page load (delegated to [ProductsPagingSource]):
 *   1. Room hit + TTL valid  →  return cached rows immediately, no network call.
 *   2. Room miss / expired   →  fetch from network, upsert into Room, return result.
 *   3. Network error on miss →  propagate as [LoadResult.Error] for Paging 3 retry UI.
 */
@Singleton
class LocalProductsRepositoryImpl @Inject constructor(
    private val api: ProductsApi,
    private val dao: ProductDao
) : ProductsRepository {

    internal var ttlMs: Long = TTL_MS

    suspend fun clearCache() = dao.clearAll()

    internal suspend fun getCached(skip: Int): List<Product>? {
        val rows = dao.getProducts(
            skip = skip,
            now = System.currentTimeMillis(),
            ttlMs = this.ttlMs
        )
        return if (rows.isEmpty()) null else rows.map { it.toDomain() }
    }

    internal suspend fun putCache(skip: Int, products: List<Product>) {
        val now = System.currentTimeMillis()
        dao.insertProducts(products.map { it.toEntity(skip = skip, cacheTimeStamp = now) })
    }

    override fun getPagedProducts(): Flow<PagingData<Product>> =
        Pager(
            config = PagingConfig(
                pageSize = ProductsApi.PAGE_SIZE,
                prefetchDistance = 2,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                ProductsPagingSource(
                    api = api,
                    getCached = ::getCached,
                    putCache = ::putCache
                )
            }
        ).flow

    companion object {
        const val TTL_MS = 5 * 60 * 1000L
    }
}
