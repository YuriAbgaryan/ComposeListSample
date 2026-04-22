package com.yur.list.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yur.list.domain.model.Product
import com.yur.list.domain.usecase.GetProductsUseCase
import com.yur.list.presentation.products.mvi.ProductsEffect
import com.yur.list.presentation.products.mvi.ProductsIntent
import com.yur.list.presentation.products.mvi.ProductsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    val pagingFlow: Flow<PagingData<Product>> = getProductsUseCase()
        .cachedIn(viewModelScope)

    private val _state = MutableStateFlow(ProductsState())
    val state: StateFlow<ProductsState> = _state.asStateFlow()

    private val _effect = Channel<ProductsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: ProductsIntent) {
        when (intent) {
            is ProductsIntent.Refresh -> {
                _state.update { it.copy(scrollToTop = true) }
            }

            is ProductsIntent.ScrollToTopHandled -> {
                _state.update { it.copy(scrollToTop = false) }
            }
        }
    }

    fun onPagingError(message: String) {
        viewModelScope.launch {
            _effect.send(ProductsEffect.ShowError(message))
        }
    }
}
