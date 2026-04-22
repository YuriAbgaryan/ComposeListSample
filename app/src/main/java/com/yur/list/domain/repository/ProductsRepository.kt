package com.yur.list.domain.repository

import androidx.paging.PagingData
import com.yur.list.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductsRepository {
    fun getPagedProducts(): Flow<PagingData<Product>>
}
