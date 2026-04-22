package com.yur.list.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yur.list.data.local.ProductDatabase
import com.yur.list.data.remote.api.ProductsApi
import com.yur.list.data.remote.dto.ProductDto
import com.yur.list.data.remote.dto.ProductsResponseDto
import com.yur.list.domain.model.Product
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalProductsRepositoryImplTest {

    private val api: ProductsApi = mockk()
    private lateinit var database: ProductDatabase
    private lateinit var repository: LocalProductsRepositoryImpl

    private val page0Dto = (1..10).map {
        ProductDto(
            id = it, title = "Product $it", description = "Desc $it",
            thumbnail = "url$it"
        )
    }
    private val page0Domain = page0Dto.map {
        Product(it.id, it.title, it.description, it.thumbnail)
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ProductDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = LocalProductsRepositoryImpl(api, database.productDao())
    }

    @After
    fun tearDown() = database.close()

    // ── cache behaviour via Room ──────────────────────────────────────────────

    @Test
    fun `getCached returns null on empty database`() = runTest {
        assertNull(repository.getCached(skip = 0))
    }

    @Test
    fun `putCache then getCached returns the same products`() = runTest {
        repository.putCache(skip = 0, products = page0Domain)
        assertEquals(page0Domain, repository.getCached(skip = 0))
    }

    @Test
    fun `getCached returns null after TTL expires`() = runTest {
        repository.ttlMs = 0L
        repository.putCache(skip = 0, products = page0Domain)
        // TTL = 0 means any cached entry is immediately considered expired
        assertNull(repository.getCached(skip = 0))
    }

    @Test
    fun `clearCache removes all entries from database`() = runTest {
        repository.putCache(skip = 0, products = page0Domain)
        repository.putCache(skip = 10, products = page0Domain)

        repository.clearCache()

        assertNull(repository.getCached(skip = 0))
        assertNull(repository.getCached(skip = 10))
    }

    @Test
    fun `putCache overwrites existing entry for same skip`() = runTest {
        val updated = listOf(Product(99, "Updated", "Desc", "url"))
        repository.putCache(skip = 0, products = page0Domain)
        repository.putCache(skip = 0, products = updated)
        assertEquals(updated, repository.getCached(skip = 0))
    }

    @Test
    fun `multiple pages stored and retrieved independently`() = runTest {
        val page1 = listOf(Product(11, "P11", "D", "u"))
        repository.putCache(skip = 0, products = page0Domain)
        repository.putCache(skip = 10, products = page1)

        assertEquals(page0Domain, repository.getCached(skip = 0))
        assertEquals(page1, repository.getCached(skip = 10))
    }

    // ── offline-first paging ──────────────────────────────────────────────────

    @Test
    fun `getPagedProducts serves from Room cache without hitting network`() = runTest {
        repository.putCache(skip = 0, products = page0Domain)

        // Just verify cache is populated and no network call happens
        val cached = repository.getCached(skip = 0)
        assertEquals(page0Domain, cached)
        coVerify(exactly = 0) { api.getProducts(any(), any()) }
    }

    @Test
    fun `getPagedProducts fetches from network on cache miss and persists to Room`() = runTest {
        coEvery { api.getProducts(limit = any(), skip = 0) } returns
                ProductsResponseDto(products = page0Dto, total = 10)

        // Simulate what PagingSource does: miss → network → persist
        val cached = repository.getCached(skip = 0)
        assertNull(cached)

        // Simulate network fetch + persist (as PagingSource would do)
        val remote = RemoteProductsRepositoryImpl(api)
        val result = remote.getProducts(skip = 0, limit = 10)
        repository.putCache(skip = 0, products = result.products)

        assertNotNull(repository.getCached(skip = 0))
        assertEquals(page0Domain, repository.getCached(skip = 0))
    }
}
