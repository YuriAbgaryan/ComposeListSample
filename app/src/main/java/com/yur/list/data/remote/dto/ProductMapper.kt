package com.yur.list.data.remote.dto

import com.yur.list.domain.model.Product

fun ProductDto.toDomain(): Product = Product(
    id = id,
    title = title,
    description = description,
    thumbnail = thumbnail
)
