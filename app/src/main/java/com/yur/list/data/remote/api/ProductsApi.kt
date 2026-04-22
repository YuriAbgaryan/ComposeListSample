package com.yur.list.data.remote.api

import com.yur.list.data.remote.dto.ProductsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ProductsApi {

    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("skip") skip: Int = 0
    ): ProductsResponseDto

    companion object {
        const val BASE_URL = "https://dummyjson.com/"
        const val PAGE_SIZE = 10
    }
}
