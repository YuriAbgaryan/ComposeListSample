package com.yur.list.data.remote.api

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yur.list.data.repository.RemoteProductsRepositoryImpl
import com.yur.list.domain.model.Product
import retrofit2.HttpException
import java.io.IOException

class ProductsPagingSource(
    private val api: ProductsApi,
    private val getCached: suspend (skip: Int) -> List<Product>?,
    private val putCache: suspend (skip: Int, products: List<Product>) -> Unit
) : PagingSource<Int, Product>() {

    private val remote = RemoteProductsRepositoryImpl(api)

    override fun getRefreshKey(state: PagingState<Int, Product>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(ProductsApi.PAGE_SIZE)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(ProductsApi.PAGE_SIZE)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Product> {
        val skip = params.key ?: 0

        getCached(skip)?.let { cached ->
            return pageResult(cached, skip, params.loadSize, Int.MAX_VALUE)
        }

        return try {
            val result = remote.getProducts(skip = skip, limit = params.loadSize)
            putCache(skip, result.products)
            pageResult(result.products, skip, params.loadSize, result.total)
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    private fun pageResult(
        products: List<Product>,
        skip: Int,
        loadSize: Int,
        total: Int
    ): LoadResult.Page<Int, Product> {
        val nextSkip = skip + products.size
        return LoadResult.Page(
            data = products,
            prevKey = if (skip == 0) null else skip - loadSize,
            nextKey = if (products.isEmpty() || nextSkip >= total) null else nextSkip
        )
    }
}
