package com.example.unimarket.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.usecase.auth.GetCurrentUserUseCase
import com.example.unimarket.domain.usecase.auth.GetUserUniversityByIdUseCase
import com.example.unimarket.domain.usecase.explore.GetAllProductsUseCase
import com.example.unimarket.domain.usecase.image.GetUserAvatarUrl
import com.example.unimarket.domain.usecase.product.GetCategoriesUseCase
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserUniversityByIdUseCase: GetUserUniversityByIdUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getUserAvatarUrl: GetUserAvatarUrl
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true))
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private val currentUserId = (getCurrentUserUseCase() as? FirebaseUser)?.uid.orEmpty()
    private val sellerAvatarCache = mutableMapOf<String, String>()
    private val loadingSellerAvatarIds = mutableSetOf<String>()
    private val sellerUniversityCache = mutableMapOf<String, String>()
    private val loadingSellerUniversityIds = mutableSetOf<String>()

    init {
        loadData()
    }

    fun refresh() {
        loadData()
    }

    private fun loadData() {
        loadJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        loadJob = viewModelScope.launch {
            try {
                val products = getAllProductsUseCase().first()
                val categories = getCategoriesUseCase().first()
                val currentState = _uiState.value

                _uiState.value = updateFilteredProducts(
                    currentState.copy(
                        products = products,
                        categories = categories,
                        isLoading = false
                    )
                )
                preloadSellerUniversities(products)
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load explore data"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(currentState.copy(searchQuery = query))
    }

    fun updateSelectedCategory(categoryName: String) {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(currentState.copy(selectedCategory = categoryName))
    }

    fun updateSelectedPriceFilter(priceFilter: ExplorePriceFilter) {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(currentState.copy(selectedPriceFilter = priceFilter))
    }

    fun updateSelectedPriceSort(priceSort: ExplorePriceSort) {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(currentState.copy(selectedPriceSort = priceSort))
    }

    fun updateLocationFilter(location: String) {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(
            currentState.copy(selectedLocationFilter = location)
        )
    }

    fun updateUniversityFilter(university: String) {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(
            currentState.copy(selectedUniversityFilter = university)
        )
    }

    fun clearFilters() {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(
            currentState.copy(
                selectedPriceFilter = ExplorePriceFilter.ALL,
                selectedPriceSort = ExplorePriceSort.RECOMMENDED,
                selectedLocationFilter = "",
                selectedUniversityFilter = ""
            )
        )
    }

    fun resetExploreState() {
        val currentState = _uiState.value
        _uiState.value = updateFilteredProducts(
            currentState.copy(
                searchQuery = "",
                selectedCategory = "All Items",
                selectedPriceFilter = ExplorePriceFilter.ALL,
                selectedPriceSort = ExplorePriceSort.RECOMMENDED,
                selectedLocationFilter = "",
                selectedUniversityFilter = ""
            )
        )
    }

    private fun updateFilteredProducts(state: ExploreUiState): ExploreUiState {
        val filteredProducts = filterProducts(
            products = state.products,
            query = state.searchQuery,
            categoryName = state.selectedCategory,
            categories = state.categories,
            priceFilter = state.selectedPriceFilter,
            priceSort = state.selectedPriceSort,
            locationFilter = state.selectedLocationFilter,
            universityFilter = state.selectedUniversityFilter
        )
        val matchedSellers = buildSellerPreviews(
            filteredProducts = filteredProducts,
            query = state.searchQuery
        )
        matchedSellers.forEach { seller ->
            preloadSellerAvatar(seller.sellerId, seller.sellerName)
        }
        return state.copy(
            filteredProducts = filteredProducts,
            matchedSellers = matchedSellers
        )
    }

    private fun filterProducts(
        products: List<Product>,
        query: String,
        categoryName: String,
        categories: List<Category>,
        priceFilter: ExplorePriceFilter,
        priceSort: ExplorePriceSort,
        locationFilter: String,
        universityFilter: String
    ): List<Product> {
        val normalizedQuery = query.trim()
        val normalizedLocationFilter = locationFilter.trim()
        val normalizedUniversityFilter = universityFilter.trim()
        val selectedCategory = categories.firstOrNull { category ->
            category.name.equals(categoryName, ignoreCase = true) ||
                category.id.equals(categoryName, ignoreCase = true)
        }

        val filteredProducts = products.filter { product ->
            val inStock = product.quantityAvailable > 0
            val isOwnProduct = currentUserId.isNotBlank() && product.userId == currentUserId
            val isApproved =
                product.moderationStatus.equals("APPROVED", ignoreCase = true)
            val matchesQuery = normalizedQuery.isBlank() ||
                product.name.contains(normalizedQuery, ignoreCase = true) ||
                product.description.contains(normalizedQuery, ignoreCase = true) ||
                product.sellerName.contains(normalizedQuery, ignoreCase = true)
            val matchesCategory = isAllCategory(categoryName) ||
                product.categoryId.equals(categoryName, ignoreCase = true) ||
                selectedCategory?.let { category ->
                    product.categoryId.equals(category.id, ignoreCase = true) ||
                        product.categoryId.equals(category.name, ignoreCase = true)
                } == true
            val matchesPrice = priceFilter.matches(product.price)
            val matchesLocation = normalizedLocationFilter.isBlank() || matchesLocationFilter(
                product = product,
                filter = normalizedLocationFilter
            )
            val matchesUniversity = normalizedUniversityFilter.isBlank() || matchesUniversityFilter(
                product = product,
                filter = normalizedUniversityFilter
            )

            inStock &&
                isApproved &&
                !isOwnProduct &&
                matchesQuery &&
                matchesCategory &&
                matchesPrice &&
                matchesLocation &&
                matchesUniversity
        }

        return when (priceSort) {
            ExplorePriceSort.RECOMMENDED -> filteredProducts
            ExplorePriceSort.PRICE_LOW_TO_HIGH -> filteredProducts.sortedBy { it.price }
            ExplorePriceSort.PRICE_HIGH_TO_LOW -> filteredProducts.sortedByDescending { it.price }
        }
    }

    private fun matchesLocationFilter(product: Product, filter: String): Boolean {
        return product.location.contains(filter, ignoreCase = true) ||
            product.sellerPickupAddress?.addressLine.orEmpty().contains(filter, ignoreCase = true)
    }

    private fun matchesUniversityFilter(product: Product, filter: String): Boolean {
        val sellerId = product.userId
        if (sellerId.isBlank()) return false
        val sellerUniversity = sellerUniversityCache[sellerId]
        if (sellerUniversity == null) {
            preloadSellerUniversity(sellerId)
            return false
        }
        return sellerUniversity.contains(filter, ignoreCase = true)
    }

    private fun preloadSellerUniversities(products: List<Product>) {
        products.asSequence()
            .map { it.userId }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach(::preloadSellerUniversity)
    }

    private fun preloadSellerUniversity(sellerId: String) {
        if (sellerId.isBlank() || sellerId in loadingSellerUniversityIds || sellerUniversityCache.containsKey(sellerId)) {
            return
        }

        loadingSellerUniversityIds += sellerId
        viewModelScope.launch {
            val university = getUserUniversityByIdUseCase(sellerId)
                .getOrNull()
                .orEmpty()
                .trim()
            sellerUniversityCache[sellerId] = university
            loadingSellerUniversityIds -= sellerId
            _uiState.value = updateFilteredProducts(_uiState.value)
        }
    }

    private fun buildSellerPreviews(
        filteredProducts: List<Product>,
        query: String
    ): List<ExploreSellerPreview> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()

        return filteredProducts
            .filter { it.sellerName.contains(normalizedQuery, ignoreCase = true) }
            .groupBy { it.userId }
            .mapNotNull { (sellerId, sellerProducts) ->
                val firstProduct = sellerProducts.firstOrNull() ?: return@mapNotNull null
                ExploreSellerPreview(
                    sellerId = sellerId,
                    sellerName = firstProduct.sellerName,
                    avatarUrl = sellerAvatarCache[sellerId].orEmpty().ifBlank {
                        buildAvatarFallbackUrl(firstProduct.sellerName)
                    },
                    previewProducts = sellerProducts
                        .sortedByDescending { it.postedAt }
                        .take(4),
                    totalListings = sellerProducts.size
                )
            }
            .sortedBy { it.sellerName.lowercase() }
    }

    private fun preloadSellerAvatar(sellerId: String, sellerName: String) {
        if (sellerId.isBlank() || sellerAvatarCache.containsKey(sellerId) || sellerId in loadingSellerAvatarIds) {
            return
        }

        loadingSellerAvatarIds += sellerId
        viewModelScope.launch {
            val avatarUrl = getUserAvatarUrl(sellerId)
                .getOrNull()
                .orEmpty()
                .ifBlank { buildAvatarFallbackUrl(sellerName) }
            sellerAvatarCache[sellerId] = avatarUrl
            loadingSellerAvatarIds -= sellerId
            _uiState.value = updateFilteredProducts(_uiState.value)
        }
    }

    private fun buildAvatarFallbackUrl(name: String): String {
        val encodedName = URLEncoder.encode(
            name.ifBlank { "Student Seller" },
            StandardCharsets.UTF_8.toString()
        )
        return "https://ui-avatars.com/api/?name=$encodedName&background=random"
    }

    private fun isAllCategory(categoryName: String): Boolean {
        return categoryName.equals("All Items", ignoreCase = true) ||
            categoryName.equals("All", ignoreCase = true)
    }
}
