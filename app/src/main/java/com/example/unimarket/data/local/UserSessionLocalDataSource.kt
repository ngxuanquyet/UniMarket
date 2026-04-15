package com.example.unimarket.data.local

import android.content.Context
import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.model.SellerPaymentMethodType
import com.example.unimarket.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class UserSessionLocalDataSource @Inject constructor(
    @ApplicationContext context: Context
) {

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _cachedUser = MutableStateFlow(readUserFromPreferences())
    val cachedUser: StateFlow<UserProfile?> = _cachedUser.asStateFlow()

    fun getCachedUser(): UserProfile? = _cachedUser.value

    fun saveUser(userProfile: UserProfile) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, userProfile.id)
            .putString(KEY_DISPLAY_NAME, userProfile.displayName)
            .putString(KEY_EMAIL, userProfile.email)
            .putString(KEY_AVATAR_URL, userProfile.avatarUrl)
            .putString(KEY_UNIVERSITY, userProfile.university)
            .putBoolean(KEY_IS_LOCK, userProfile.isLock)
            .putString(KEY_STUDENT_ID, userProfile.studentId)
            .putInt(KEY_BOUGHT_COUNT, userProfile.boughtCount)
            .putInt(KEY_SOLD_COUNT, userProfile.soldCount)
            .putFloat(KEY_AVERAGE_RATING, userProfile.averageRating.toFloat())
            .putInt(KEY_RATING_COUNT, userProfile.ratingCount)
            .putString(KEY_WALLET_BALANCE, userProfile.walletBalance.toString())
            .putString(KEY_PAYMENT_METHODS, paymentMethodsToJson(userProfile.paymentMethods))
            .apply()

        _cachedUser.value = userProfile
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
        _cachedUser.value = null
    }

    private fun readUserFromPreferences(): UserProfile? {
        val userId = sharedPreferences.getString(KEY_USER_ID, null).orEmpty()
        if (userId.isBlank()) return null

        return UserProfile(
            id = userId,
            displayName = sharedPreferences.getString(KEY_DISPLAY_NAME, null).orEmpty(),
            email = sharedPreferences.getString(KEY_EMAIL, null).orEmpty(),
            avatarUrl = sharedPreferences.getString(KEY_AVATAR_URL, null).orEmpty(),
            university = sharedPreferences.getString(KEY_UNIVERSITY, null).orEmpty(),
            isLock = sharedPreferences.getBoolean(KEY_IS_LOCK, false),
            studentId = sharedPreferences.getString(KEY_STUDENT_ID, null).orEmpty(),
            boughtCount = sharedPreferences.getInt(KEY_BOUGHT_COUNT, 0),
            soldCount = sharedPreferences.getInt(KEY_SOLD_COUNT, 0),
            averageRating = sharedPreferences.getFloat(KEY_AVERAGE_RATING, 0f).toDouble(),
            ratingCount = sharedPreferences.getInt(KEY_RATING_COUNT, 0),
            walletBalance = sharedPreferences.getString(KEY_WALLET_BALANCE, null)
                ?.toDoubleOrNull()
                ?: 0.0,
            paymentMethods = paymentMethodsFromJson(
                sharedPreferences.getString(KEY_PAYMENT_METHODS, null).orEmpty()
            )
        )
    }

    private fun paymentMethodsToJson(methods: List<SellerPaymentMethod>): String {
        val array = JSONArray()
        methods.forEach { method ->
            array.put(
                JSONObject()
                    .put("id", method.id)
                    .put("type", method.type.storageValue)
                    .put("label", method.label)
                    .put("accountName", method.accountName)
                    .put("accountNumber", method.accountNumber)
                    .put("bankCode", method.bankCode)
                    .put("bankName", method.bankName)
                    .put("phoneNumber", method.phoneNumber)
                    .put("note", method.note)
                    .put("isDefault", method.isDefault)
            )
        }
        return array.toString()
    }

    private fun paymentMethodsFromJson(raw: String): List<SellerPaymentMethod> {
        if (raw.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                SellerPaymentMethod(
                    id = item.optString("id"),
                    type = SellerPaymentMethodType.fromRaw(item.optString("type")),
                    label = item.optString("label"),
                    accountName = item.optString("accountName"),
                    accountNumber = item.optString("accountNumber"),
                    bankCode = item.optString("bankCode"),
                    bankName = item.optString("bankName"),
                    phoneNumber = item.optString("phoneNumber"),
                    note = item.optString("note"),
                    isDefault = item.optBoolean("isDefault")
                )
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREF_NAME = "user_session"
        const val KEY_USER_ID = "user_id"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_EMAIL = "email"
        const val KEY_AVATAR_URL = "avatar_url"
        const val KEY_UNIVERSITY = "university"
        const val KEY_IS_LOCK = "is_lock"
        const val KEY_STUDENT_ID = "student_id"
        const val KEY_BOUGHT_COUNT = "bought_count"
        const val KEY_SOLD_COUNT = "sold_count"
        const val KEY_AVERAGE_RATING = "average_rating"
        const val KEY_RATING_COUNT = "rating_count"
        const val KEY_WALLET_BALANCE = "wallet_balance"
        const val KEY_PAYMENT_METHODS = "payment_methods"
    }
}
