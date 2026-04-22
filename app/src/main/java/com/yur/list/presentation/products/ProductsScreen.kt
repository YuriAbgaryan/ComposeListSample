package com.yur.list.presentation.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.yur.list.R
import com.yur.list.domain.model.Product
import com.yur.list.presentation.products.components.ErrorView
import com.yur.list.presentation.products.components.LoadingView
import com.yur.list.presentation.products.components.PagingFooter
import com.yur.list.presentation.products.components.ProductItem
import com.yur.list.presentation.products.mvi.ProductsEffect
import com.yur.list.presentation.products.mvi.ProductsIntent
import com.yur.list.presentation.theme.Dimens
import kotlinx.coroutines.launch

@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagingItems: LazyPagingItems<Product> = viewModel.pagingFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showBackToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProductsEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(state.scrollToTop) {
        if (state.scrollToTop) {
            listState.animateScrollToItem(0)
            viewModel.handleIntent(ProductsIntent.ScrollToTopHandled)
        }
    }

    LaunchedEffect(pagingItems.loadState.append) {
        val appendError = pagingItems.loadState.append as? LoadState.Error
        appendError?.let { viewModel.onPagingError(it.error.message ?: "Failed to load more") }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showBackToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Dimens.Dimen_48)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.back_to_top)
                    )
                }
            }
        }
    ) { innerPadding ->

        if (pagingItems.loadState.refresh is LoadState.Loading && pagingItems.itemCount == 0) {
            LoadingView(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        if (pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0) {
            val error = (pagingItems.loadState.refresh as LoadState.Error).error
            ErrorView(
                message = error.message ?: stringResource(R.string.unknown_error),
                onRetry = { pagingItems.retry() },
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Dimens.SpacingL,
                    vertical = Dimens.SpacingM
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.Dimen_10)
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id }
                ) { index ->
                    pagingItems[index]?.let { product ->
                        ProductItem(product = product)
                    }
                }

                item {
                    PagingFooter(
                        loadState = pagingItems.loadState.append,
                        onRetry = { pagingItems.retry() }
                    )
                }
            }
        }
    }
}
