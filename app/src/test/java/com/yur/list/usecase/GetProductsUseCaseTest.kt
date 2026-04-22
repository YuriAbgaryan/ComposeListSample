package com.yur.list.domain.usecase

import androidx.paging.PagingData
import com.yur.list.domain.model.Product
import com.yur.list.domain.repository.ProductsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class GetProductsUseCaseTest {

    private val repository: ProductsRepository = mockk()
    private lateinit var useCase: GetProductsUseCase

    @Before
    fun setUp() {
        useCase = GetProductsUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository`() {
        val expected: Flow<PagingData<Product>> = flowOf(PagingData.empty())
        every { repository.getPagedProducts() } returns expected

        val result = useCase()

        assertSame(expected, result)
        verify(exactly = 1) { repository.getPagedProducts() }
    }

    @Test
    fun `invoke calls repository only once per invocation`() {
        every { repository.getPagedProducts() } returns flowOf(PagingData.empty())

        useCase()

        verify(exactly = 1) { repository.getPagedProducts() }
    }
}
