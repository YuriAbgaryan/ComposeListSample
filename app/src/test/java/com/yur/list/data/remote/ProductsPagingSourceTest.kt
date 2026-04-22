package com.yur.list.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult
import com.yur.list.data.remote.api.ProductsApi
import com.yur.list.data.remote.api.ProductsPagingSource
import com.yur.list.data.remote.dto.ProductDto
import com.yur.list.data.remote.dto.ProductsResponseDto
import com.yur.list.domain.model.Product
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ProductsPagingSourceTest {

    private val api: ProductsApi = mockk()

    private val cachedPages = mutableMapOf<Int, List<Product>>()
    private val savedPages = mutableMapOf<Int, List<Product>>()

    private lateinit var pagingSource: ProductsPagingSource

    private val products = (1..10).map {
        Product(it, "Product $it", "Desc $it", "url$it")
    }
    private val productDtos = (1..10).map {
        ProductDto(
            id = it, title = "Product $it", description = "Desc $it",
            thumbnail = "url$it"
        )
    }

    private fun loadParams(key: Int? = null, loadSize: Int = 10) =
        PagingSource.LoadParams.Refresh(key = key, loadSize = loadSize, placeholdersEnabled = false)

    @Before
    fun setUp() {
        cachedPages.clear()
        savedPages.clear()
        pagingSource = ProductsPagingSource(
            api = api,
            getCached = { skip -> cachedPages[skip] },
            putCache = { skip, list -> savedPages[skip] = list }
        )
    }

    // ── cache hit ────────────────────────────────────────────────────────────

    @Test
    fun `load returns cached data without calling API`() = runTest {
        cachedPages[0] = products

        val result = pagingSource.load(loadParams()) as LoadResult.Page

        assertEquals(products, result.data)
        coVerify(exactly = 0) { api.getProducts(any(), any()) }
    }

    @Test
    fun `load on cache hit sets prevKey null for first page`() = runTest {
        cachedPages[0] = products
        val result = pagingSource.load(loadParams()) as LoadResult.Page
        assertNull(result.prevKey)
    }

    @Test
    fun `load on cache hit sets nextKey to skip + pageSize`() = runTest {
        cachedPages[0] = products
        val result = pagingSource.load(loadParams()) as LoadResult.Page
        assertEquals(10, result.nextKey)
    }

    // ── cache miss → network ─────────────────────────────────────────────────

    @Test
    fun `load fetches from API on cache miss`() = runTest {
        coEvery { api.getProducts(limit = 10, skip = 0) } returns
                ProductsResponseDto(products = productDtos, total = 100)

        val result = pagingSource.load(loadParams()) as LoadResult.Page

        assertEquals(products, result.data)
        coVerify(exactly = 1) { api.getProducts(limit = 10, skip = 0) }
    }

    @Test
    fun `load writes network result to cache`() = runTest {
        coEvery { api.getProducts(any(), any()) } returns
                ProductsResponseDto(products = productDtos, total = 100)

        pagingSource.load(loadParams())

        assertEquals(products, savedPages[0])
    }

    @Test
    fun `load sets nextKey null when all products loaded`() = runTest {
        coEvery { api.getProducts(limit = 10, skip = 0) } returns
                ProductsResponseDto(products = productDtos, total = 10)

        val result = pagingSource.load(loadParams()) as LoadResult.Page

        assertNull(result.nextKey)
    }

    @Test
    fun `load sets correct nextKey for middle page`() = runTest {
        coEvery { api.getProducts(limit = 10, skip = 0) } returns
                ProductsResponseDto(products = productDtos, total = 50)

        val result = pagingSource.load(loadParams()) as LoadResult.Page

        assertEquals(10, result.nextKey)
    }

    @Test
    fun `load sets prevKey for non-first page`() = runTest {
        coEvery { api.getProducts(limit = 10, skip = 10) } returns
                ProductsResponseDto(products = productDtos, total = 50)

        val result = pagingSource.load(loadParams(key = 10)) as LoadResult.Page

        assertEquals(0, result.prevKey)
    }

    @Test
    fun `load returns prevKey null for first page`() = runTest {
        coEvery { api.getProducts(limit = 10, skip = 0) } returns
                ProductsResponseDto(products = productDtos, total = 50)

        val result = pagingSource.load(loadParams(key = null)) as LoadResult.Page

        assertNull(result.prevKey)
    }

    // ── error handling ───────────────────────────────────────────────────────

    @Test
    fun `load returns LoadResult Error on IOException`() = runTest {
        coEvery { api.getProducts(any(), any()) } throws IOException("No connection")

        val result = pagingSource.load(loadParams())

        assertTrue(result is LoadResult.Error)
        assertTrue((result as LoadResult.Error).throwable is IOException)
    }

    @Test
    fun `load returns LoadResult Error on generic exception`() = runTest {
        coEvery { api.getProducts(any(), any()) } throws RuntimeException("500")

        val result = pagingSource.load(loadParams())

        assertTrue(result is LoadResult.Error)
    }
}
