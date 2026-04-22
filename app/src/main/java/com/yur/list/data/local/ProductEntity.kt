package com.yur.list.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yur.list.domain.model.Product

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val thumbnail: String,
    val skip: Int,
    val cacheTimeStamp: Long = System.currentTimeMillis()
)

fun ProductEntity.toDomain() = Product(
    id = id,
    title = title,
    description = description,
    thumbnail = thumbnail
)

fun Product.toEntity(skip: Int, cacheTimeStamp: Long = System.currentTimeMillis()) = ProductEntity(
    id = id,
    title = title,
    description = description,
    thumbnail = thumbnail,
    skip = skip,
    cacheTimeStamp = cacheTimeStamp
)
