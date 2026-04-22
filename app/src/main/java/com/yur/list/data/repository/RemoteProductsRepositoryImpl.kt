package com.yur.list.data.repository

import com.yur.list.data.remote.dto.toDomain
import com.yur.list.domain.model.Product
import com.yur.list.data.remote.api.ProductsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteProductsRepositoryImpl @Inject constructor(
    private val api: ProductsApi
) {
    data class Result(val products: List<Product>, val total: Int)

    suspend fun getProducts(skip: Int, limit: Int): Result {
        val response = api.getProducts(limit = limit, skip = skip)
        return Result(
            products = response.products.map { it.toDomain() },
            total = response.total
        )
    }
}
