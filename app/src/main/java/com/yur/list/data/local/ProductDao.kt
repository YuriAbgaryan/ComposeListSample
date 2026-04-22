package com.yur.list.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductDao {

    /**
     * Returns cached products for a given page [skip] offset,
     * but only if they were cached within [ttlMs] milliseconds ago.
     * Returns empty list (not null) when the page is absent or expired.
     */
    @Query(
        """
        SELECT * FROM products
        WHERE skip = :skip
          AND (:now - cacheTimeStamp) < :ttlMs
        ORDER BY id ASC
    """
    )
    suspend fun getProducts(skip: Int, now: Long, ttlMs: Long): List<ProductEntity>

    /** Upserts a full page — replaces stale entries for the same product id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun clearAll()

    /** Removes all rows for a specific page. */
    @Query("DELETE FROM products WHERE skip = :skip")
    suspend fun clearPage(skip: Int)
}
