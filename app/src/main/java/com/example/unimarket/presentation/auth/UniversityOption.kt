package com.example.unimarket.presentation.auth

import java.text.Normalizer
import org.json.JSONArray
import org.json.JSONObject

data class UniversityOption(
    val id: String,
    val name: String,
    val shortName: String = "",
    val searchKeywords: List<String> = emptyList(),
    val isActive: Boolean = true,
    val sortOrder: Int = Int.MAX_VALUE
) {
    val searchableTokens: List<String>
        get() = buildList {
            add(name)
            if (shortName.isNotBlank()) add(shortName)
            addAll(searchKeywords)
        }.map { it.normalizeSearchText() }
}

fun parseUniversityOptions(raw: String): List<UniversityOption> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
        val trimmed = raw.trim()
        val items = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> JSONObject(trimmed).optJSONArray("universities") ?: JSONArray()
            else -> JSONArray()
        }
        List(items.length()) { index ->
            val item = items.optJSONObject(index) ?: JSONObject()
            UniversityOption(
                id = item.optString("id").trim(),
                name = item.optString("name").trim(),
                shortName = item.optString("shortName").trim(),
                searchKeywords = item.optJSONArray("searchKeywords").toStringList(),
                isActive = item.optBoolean("isActive", true),
                sortOrder = item.optInt("sortOrder", Int.MAX_VALUE)
            )
        }
            .filter { it.id.isNotBlank() && it.name.isNotBlank() && it.isActive }
            .sortedWith(compareBy<UniversityOption> { it.sortOrder }.thenBy { it.name })
    }.getOrDefault(emptyList())
}

fun filterUniversityOptions(
    options: List<UniversityOption>,
    query: String,
    limit: Int = 6
): List<UniversityOption> {
    if (options.isEmpty()) return emptyList()
    val normalizedQuery = query.normalizeSearchText()
    return if (normalizedQuery.isBlank()) {
        options.take(limit)
    } else {
        options.filter { option ->
            option.searchableTokens.any { token -> token.contains(normalizedQuery) }
        }.take(limit)
    }
}

fun resolveUniversitySelection(
    options: List<UniversityOption>,
    input: String
): UniversityOption? {
    val normalizedInput = input.normalizeSearchText()
    if (normalizedInput.isBlank()) return null
    return options.firstOrNull { option ->
        option.name.normalizeSearchText() == normalizedInput ||
            option.shortName.normalizeSearchText() == normalizedInput
    }
}

private fun String.normalizeSearchText(): String {
    val noDiacritics = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
    return noDiacritics.lowercase().trim()
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index).trim() }
        .filter { it.isNotBlank() }
}
