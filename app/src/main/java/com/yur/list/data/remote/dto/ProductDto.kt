package com.yur.list.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductsResponseDto(
    val products: List<ProductDto> = emptyList(),
    val total: Int = 0,
    val skip: Int = 0,
    val limit: Int = 0
)

@Serializable
data class ProductDto(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val thumbnail: String = "",
    val images: List<String> = emptyList()
)
