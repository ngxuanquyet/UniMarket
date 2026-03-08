package com.example.unimarket.domain.usecase.cart

import com.example.unimarket.domain.repository.CartRepository
import javax.inject.Inject

class UpdateQuantityUseCase @Inject constructor(
    private val cartRepository: CartRepository
) {
    suspend operator fun invoke(cartItemId: String, quantity: Int) {
        cartRepository.updateQuantity(cartItemId, quantity)
    }
}
