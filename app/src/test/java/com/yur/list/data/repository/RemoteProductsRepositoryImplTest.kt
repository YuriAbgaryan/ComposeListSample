package com.yur.list.data.repository

import com.yur.list.data.remote.api.ProductsApi
import com.yur.list.data.remote.dto.ProductDto
import com.yur.list.data.remote.dto.ProductsResponseDto
import com.yur.list.domain.model.Product
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteProductsRepositoryImplTest {

    private val api: ProductsApi = mockk()
    private lateinit var repository: RemoteProductsRepositoryImpl

    private val dtoList = listOf(
        ProductDto(id = 1, title = "Phone", description = "Nice phone", thumbnail = "url1"),
        ProductDto(id = 2, title = "Laptop", description = "Fast laptop", thumbnail = "url2")
    )

    @Before
    fun setUp() {
        repository = RemoteProductsRepositoryImpl(api)
    }

    @Test
    fun `getProducts maps DTOs to domain models correctly`() = runTest {
        coEvery { api.getProducts(limit = 10, skip = 0) } returns
                ProductsResponseDto(products = dtoList, total = 100)

        val result = repository.getProducts(skip = 0, limit = 10)

        assertEquals(
            listOf(
                Product(1, "Phone", "Nice phone", "url1"),
                Product(2, "Laptop", "Fast laptop", "url2")
            ),
            result.products
        )
        assertEquals(100, result.total)
    }

    @Test
    fun `getProducts passes correct skip and limit to API`() = runTest {
        coEvery { api.getProducts(limit = 5, skip = 20) } returns
                ProductsResponseDto(products = emptyList(), total = 50)

        repository.getProducts(skip = 20, limit = 5)

        coVerify(exactly = 1) { api.getProducts(limit = 5, skip = 20) }
    }

    @Test
    fun `getProducts returns empty list when API returns nothing`() = runTest {
        coEvery { api.getProducts(any(), any()) } returns
                ProductsResponseDto(products = emptyList(), total = 0)

        val result = repository.getProducts(skip = 0, limit = 10)

        assertEquals(emptyList<Product>(), result.products)
        assertEquals(0, result.total)
    }

    @Test(expected = Exception::class)
    fun `getProducts propagates network exceptions`() = runTest {
        coEvery { api.getProducts(any(), any()) } throws Exception("Network error")
        repository.getProducts(skip = 0, limit = 10)
    }
}
