# ProductsListApp

An Android app that fetches and displays a paginated product list from [DummyJSON](https://dummyjson.com/products), built as a reference implementation of **Clean Architecture + MVI** with an offline-first cache strategy.

---

## Features

- Paginated product list (10 items per page, infinite scroll)
- Offline-first — products are cached in Room and served without a network call on subsequent opens
- Pull-to-refresh — clears the Room cache and re-fetches from the network
- Back-to-top FAB — appears after scrolling past the first item
- Error handling at every level — full-screen on first load, inline retry footer on subsequent pages
- Dark mode support with Material You dynamic color (Android 12+)

---

## Architecture

The project follows **Clean Architecture** with three layers that only depend inward, combined with the **MVI** pattern in the presentation layer.

```
┌─────────────────────────────────────┐
│           Presentation              │  Jetpack Compose + MVI
│  Screen → Effect → ViewModel        │
├─────────────────────────────────────┤
│              Domain                 │  Pure Kotlin, no Android deps
│  Model · Repository (interface)     │
│  UseCases                           │
├─────────────────────────────────────┤
│               Data                  │  Implements domain contracts
│  Room (local) · Retrofit (remote)   │
│  PagingSource · Repositories        │
└─────────────────────────────────────┘
```

### MVI Flow

```
User action → ViewModel.function() → Effect → Screen reacts
                     ↓
              UseCase → Repository
              (cache or network)
```

`ProductsEffect` is delivered through a `Channel` so each event fires exactly once — no snackbar re-showing on recomposition.

### Offline-first Cache Strategy

Every page load goes through `ProductsPagingSource`:

1. **Room hit + TTL valid** → return cached rows, no network call
2. **Room miss / expired (5 min TTL)** → fetch from Retrofit, upsert into Room, return result
3. **Network error on miss** → surface as `LoadResult.Error` for Paging 3 retry UI

Refresh clears the entire Room cache first, then triggers `pagingItems.refresh()` so all pages are re-fetched from the network.

---

## Tech Stack

| Layer | Library | Version |
|---|---|---|
| UI | Jetpack Compose BOM | 2024.11.00 |
| UI | Material 3 | via BOM |
| DI | Hilt | 2.52 |
| Networking | Retrofit | 2.11.0 |
| Networking | OkHttp | 4.12.0 |
| Serialization | kotlinx.serialization | 1.7.3 |
| Pagination | Paging 3 | 3.3.4 |
| Local cache | Room | 2.6.1 |
| Image loading | Coil | 2.7.0 |
| Coroutines | kotlinx.coroutines | 1.9.0 |
| Testing | MockK | 1.13.12 |
| Testing | Robolectric | 4.13 |
| Testing | Turbine | 1.2.0 |

---

## Project Structure

```
app/src/main/java/com/example/productsapp/
│
├── data/
│   ├── local/
│   │   ├── ProductDao.kt            # Room DAO — page-keyed queries with TTL
│   │   ├── ProductDatabase.kt       # Room database definition
│   │   └── ProductEntity.kt         # Room entity + domain mappers
│   ├── remote/
│   │   ├── api/
│   │   │   ├── ProductsApi.kt       # Retrofit interface
│   │   │   └── ProductsPagingSource.kt  # Cache-first PagingSource
│   │   └── dto/
│   │       ├── ProductDto.kt        # Network response models
│   │       └── ProductMapper.kt     # DTO → domain mapper
│   └── repository/
│       ├── LocalProductsRepositoryImpl.kt   # Offline-first, owns Room cache
│       └── RemoteProductsRepositoryImpl.kt  # Pure network, no cache awareness
│
├── di/
│   ├── DatabaseModule.kt    # Hilt — provides Room database and DAO
│   ├── NetworkModule.kt     # Hilt — provides Retrofit, OkHttp, Json
│   └── RepositoryModule.kt  # Hilt — binds interfaces to implementations
│
├── domain/
│   ├── model/
│   │   └── Product.kt               # Domain model
│   ├── repository/
│   │   └── ProductsRepository.kt    # Single repository interface
│   └── usecase/
│       ├── GetProductsUseCase.kt    # Returns Flow<PagingData<Product>>
│       └── RefreshProductsUseCase.kt  # Clears cache, triggers network refresh
│
└── presentation/
    ├── products/
    │   ├── mvi/
    │   │   └── ProductsContract.kt  # ProductsEffect sealed interface
    │   ├── components/
    │   │   ├── PagingFooter.kt      # Inline loading/error at bottom of list
    │   │   ├── ProductItem.kt       # Single product row card
    │   │   └── StateViews.kt        # Full-screen LoadingView and ErrorView
    │   ├── ProductsScreen.kt        # Root composable
    │   └── ProductsViewModel.kt     # Exposes pagingFlow and refresh()
    └── theme/
        ├── Color.kt    # Fallback color values
        ├── Theme.kt    # ProductsAppTheme with dynamic color support
        └── Type.kt     # Typography definitions
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34
- Minimum device/emulator: API 24

### Run

1. Clone the repository
2. Open the project in Android Studio
3. Let Gradle sync
4. Run on an emulator or device

### Tests

```bash
./gradlew test
```

Tests run on the JVM via Robolectric — no emulator needed.

| Test file | What it covers |
|---|---|
| `ProductDaoTest` | Room DAO — insert, query, TTL expiry, clearAll, clearPage |
| `LocalProductsRepositoryImplTest` | Cache read/write, TTL, clearCache, offline-first paging |
| `RemoteProductsRepositoryImplTest` | DTO→domain mapping, param forwarding, error propagation |
| `ProductsPagingSourceTest` | Cache hit/miss, nextKey/prevKey math, error handling |
| `GetProductsUseCaseTest` | Delegates to repository, called once per invocation |

---

## API

Data is fetched from the public [DummyJSON Products API](https://dummyjson.com/products).

```
GET https://dummyjson.com/products?limit=10&skip=0
```

Each page uses `limit` (page size) and `skip` (offset) query parameters. The `skip` value is also stored on each Room row as the page key, enabling per-page cache invalidation.

---

## License

```
MIT License

Copyright (c) 2024

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```
