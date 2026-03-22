package com.example.unimarket.data.local

import android.content.Context
import com.example.unimarket.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            .putString(KEY_STUDENT_ID, userProfile.studentId)
            .putInt(KEY_BOUGHT_COUNT, userProfile.boughtCount)
            .putInt(KEY_SOLD_COUNT, userProfile.soldCount)
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
            studentId = sharedPreferences.getString(KEY_STUDENT_ID, null).orEmpty(),
            boughtCount = sharedPreferences.getInt(KEY_BOUGHT_COUNT, 0),
            soldCount = sharedPreferences.getInt(KEY_SOLD_COUNT, 0)
        )
    }

    private companion object {
        const val PREF_NAME = "user_session"
        const val KEY_USER_ID = "user_id"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_EMAIL = "email"
        const val KEY_AVATAR_URL = "avatar_url"
        const val KEY_STUDENT_ID = "student_id"
        const val KEY_BOUGHT_COUNT = "bought_count"
        const val KEY_SOLD_COUNT = "sold_count"
    }
}
