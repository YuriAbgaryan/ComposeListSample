package com.yur.list.presentation.products.mvi



data class ProductsState(
    val scrollToTop: Boolean = false
)

sealed interface ProductsIntent {
    data object Refresh : ProductsIntent
    data object ScrollToTopHandled : ProductsIntent
}

sealed interface ProductsEffect {
    data class ShowError(val message: String) : ProductsEffect
}
