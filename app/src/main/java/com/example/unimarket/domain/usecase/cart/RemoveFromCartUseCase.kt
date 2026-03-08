package com.example.unimarket.domain.usecase.cart

import com.example.unimarket.domain.repository.CartRepository
import javax.inject.Inject

class RemoveFromCartUseCase @Inject constructor(
    private val cartRepository: CartRepository
) {
    suspend operator fun invoke(cartItemId: String) {
        cartRepository.removeFromCart(cartItemId)
    }
}
