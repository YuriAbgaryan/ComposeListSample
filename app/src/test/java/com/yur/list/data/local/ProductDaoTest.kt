package com.yur.list.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductDaoTest {

    private lateinit var database: ProductDatabase
    private lateinit var dao: ProductDao

    private val now = System.currentTimeMillis()
    private val ttl = 5 * 60 * 1000L

    private fun makeEntities(skip: Int, count: Int = 5, cachedAt: Long = now) =
        (1..count).map { i ->
            ProductEntity(
                id = skip + i,
                title = "Product ${skip + i}",
                description = "Desc ${skip + i}",
                thumbnail = "url${skip + i}",
                skip = skip,
                cacheTimeStamp = cachedAt
            )
        }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ProductDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.productDao()
    }

    @After
    fun tearDown() = database.close()

    // ── insert & query ───────────────────────────────────────────────────────

    @Test
    fun `getProducts returns empty list when nothing inserted`() = runTest {
        val result = dao.getProducts(skip = 0, now = now, ttlMs = ttl)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getProducts returns rows for correct skip within TTL`() = runTest {
        val entities = makeEntities(skip = 0)
        dao.insertProducts(entities)

        val result = dao.getProducts(skip = 0, now = now, ttlMs = ttl)

        assertEquals(entities.size, result.size)
        assertEquals(entities.map { it.id }, result.map { it.id })
    }

    @Test
    fun `getProducts returns empty list for different skip`() = runTest {
        dao.insertProducts(makeEntities(skip = 0))

        val result = dao.getProducts(skip = 10, now = now, ttlMs = ttl)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getProducts returns empty list when TTL expired`() = runTest {
        val expiredAt = now - (ttl + 1000) // 1 second past TTL
        dao.insertProducts(makeEntities(skip = 0, cachedAt = expiredAt))

        val result = dao.getProducts(skip = 0, now = now, ttlMs = ttl)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `insertProducts replaces existing row with same id`() = runTest {
        val original = makeEntities(skip = 0)
        dao.insertProducts(original)

        val updated = original.map { it.copy(title = "Updated ${it.id}") }
        dao.insertProducts(updated)

        val result = dao.getProducts(skip = 0, now = now, ttlMs = ttl)
        assertEquals(updated.map { it.title }, result.map { it.title })
    }

    @Test
    fun `multiple pages stored and retrieved independently`() = runTest {
        dao.insertProducts(makeEntities(skip = 0))
        dao.insertProducts(makeEntities(skip = 10))

        val page0 = dao.getProducts(skip = 0, now = now, ttlMs = ttl)
        val page1 = dao.getProducts(skip = 10, now = now, ttlMs = ttl)

        assertEquals(5, page0.size)
        assertEquals(5, page1.size)
        assertTrue(page0.all { it.skip == 0 })
        assertTrue(page1.all { it.skip == 10 })
    }

    // ── clearAll ─────────────────────────────────────────────────────────────

    @Test
    fun `clearAll removes all rows`() = runTest {
        dao.insertProducts(makeEntities(skip = 0))
        dao.insertProducts(makeEntities(skip = 10))

        dao.clearAll()

        assertTrue(dao.getProducts(skip = 0, now = now, ttlMs = ttl).isEmpty())
        assertTrue(dao.getProducts(skip = 10, now = now, ttlMs = ttl).isEmpty())
    }

    // ── clearPage ─────────────────────────────────────────────────────────────

    @Test
    fun `clearPage removes only rows for given skip`() = runTest {
        dao.insertProducts(makeEntities(skip = 0))
        dao.insertProducts(makeEntities(skip = 10))

        dao.clearPage(skip = 0)

        assertTrue(dao.getProducts(skip = 0, now = now, ttlMs = ttl).isEmpty())
        assertEquals(5, dao.getProducts(skip = 10, now = now, ttlMs = ttl).size)
    }
}
