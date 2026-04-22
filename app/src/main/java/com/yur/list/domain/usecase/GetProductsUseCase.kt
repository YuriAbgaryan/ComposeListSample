package com.yur.list.domain.usecase

import androidx.paging.PagingData
import com.yur.list.domain.model.Product
import com.yur.list.domain.repository.ProductsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val repository: ProductsRepository
) {
    operator fun invoke(): Flow<PagingData<Product>> = repository.getPagedProducts()
}
