package com.example.unimarket.data.repository

import android.util.Log
import com.example.unimarket.data.api.GeminiApiService
import com.example.unimarket.data.api.model.GeminiContent
import com.example.unimarket.data.api.model.GeminiGenerateContentRequest
import com.example.unimarket.data.api.model.GeminiGenerationConfig
import com.example.unimarket.data.api.model.GeminiPart
import com.example.unimarket.data.api.model.GeminiThinkingConfig
import com.example.unimarket.domain.model.AiListingInput
import com.example.unimarket.domain.model.AiListingSuggestion
import com.example.unimarket.domain.repository.AiRepository
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GeminiAiRepositoryImpl @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val remoteConfig: FirebaseRemoteConfig,
    private val gson: Gson
) : AiRepository {

    override suspend fun generateListingSuggestion(input: AiListingInput): Result<AiListingSuggestion> {
        return try {
            remoteConfig.fetchAndActivate().await()
            val apiKey = remoteConfig.getString("API_KEY")
            if (apiKey.isBlank()) {
                return Result.failure(Exception("Gemini API key is missing from Remote Config"))
            }

            val request = GeminiGenerateContentRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(
                            text = "You write concise marketplace listings for a student marketplace app. " +
                                "Only use facts provided by the user. Do not invent missing details. " +
                                "Respond with valid JSON only using this shape: " +
                                "{\"title\":\"...\",\"description\":\"...\",\"specifications\":{\"key\":\"value\"}}. " +
                                "The specifications field must be a flat JSON object mapping string keys to string values."
                        )
                    )
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(text = buildPrompt(input))
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.5,
                    topP = 0.9,
                    topK = 40,
                    maxOutputTokens = 512,
                    responseMimeType = "application/json",
                    thinkingConfig = GeminiThinkingConfig(thinkingBudget = 0)
                )
            )

            val response = geminiApiService.generateContent(apiKey = apiKey, request = request)
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                Log.e("GeminiAiRepository", "Gemini request failed: ${response.code()} $errorBody")
                return Result.failure(Exception("Gemini request failed with code ${response.code()}"))
            }

            Log.d(
                "GeminiAiRepository",
                "Gemini response success: code=${response.code()}, body=${gson.toJson(response.body())}"
            )

            val rawJson = response.body()
                ?.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.joinToString(separator = "") { it.text.orEmpty() }
                .orEmpty()
                .trim()

            if (rawJson.isBlank()) {
                return Result.failure(Exception("Gemini returned an empty response"))
            }

            Log.d("GeminiAiRepository", "Gemini raw generated JSON: $rawJson")

            val parsed = gson.fromJson(rawJson, AiListingSuggestion::class.java)
            if (parsed == null || parsed.description.isBlank()) {
                return Result.failure(Exception("Failed to parse Gemini response"))
            }

            val finalSpecifications = buildFinalSpecifications(input, parsed.specifications)
            val finalDescription = buildFinalDescription(
                input = input,
                aiDescription = parsed.description,
                specifications = finalSpecifications
            )

            Result.success(
                parsed.copy(
                    title = parsed.title.trim(),
                    description = finalDescription,
                    specifications = finalSpecifications
                )
            )
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun buildPrompt(input: AiListingInput): String {
        val specificationsText = input.specifications.entries
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .joinToString(separator = "\n") { "- ${it.key}: ${it.value}" }
            .ifBlank { "- None" }

        val deliveryMethodsText = input.deliveryMethodsAvailable
            .joinToString(separator = ", ") { it.title }
            .ifBlank { "None provided" }

        return """
            Create a high-quality product listing for a second-hand marketplace app targeting students.

            Keep the same language as the user's input when possible.
            If the language is unclear, default to Vietnamese.

            Title:
            Make the title short, clear, and easy to search.
            Prioritize brand, model, capacity, and key specs.
            Keep only the most important identifying information.

            Description:
            Write a natural, trustworthy, and informative description.
            Length from 8 to 15 short lines or short paragraphs.
            Each line should contain meaningful information and be easy to read on mobile.
            Focus on specific and concrete details.
            Highlight key specs and standout features.
            Describe actual usage condition if available such as appearance, battery, and performance.
            Mention why the product is suitable or valuable for students.
            Include accessories or extras if mentioned.
            Use a tone that feels honest and close to how a real seller would write.
            Break content into short lines to improve readability.
            Never output placeholders or vague text such as "No description provided." or "No specs available.".

            Additional details:
            Include condition, quantity, negotiable status, and delivery method when they are provided.

            Specifications:
            Create a flat JSON object named specifications.
            Ensure specifications is a JSON map from key to value, where both key and value are strings.
            Extract only information that appears in the input.
            Normalize common shorthand formats.
            24/512 becomes RAM 24GB and Storage 512GB.
            16GB/1TB becomes RAM 16GB and Storage 1TB.
            M4 becomes Chip M4.
            Use short and clear keys such as Brand, Model, Chip, RAM, Storage, Color, Size, Material, Condition, Battery, Dimensions.
            Keep values clean and consistent.

            Output format:
            Return title.
            Return description.
            Return specifications as JSON.

            Ensure the final listing is detailed, clear, and helpful for a student buyer.

            User input:
            Title: ${input.title.ifBlank { "(empty)" }}
            Description: ${input.description.ifBlank { "(empty)" }}
            Category: ${input.category.ifBlank { "(empty)" }}
            Condition: ${input.condition.ifBlank { "(empty)" }}
            Price: ${input.price.ifBlank { "(empty)" }}
            Quantity: ${input.quantity.ifBlank { "(empty)" }}
            Negotiable: ${if (input.isNegotiable) "Yes" else "No"}
            Delivery methods: $deliveryMethodsText
            Specifications:
            $specificationsText
        """.trimIndent()
    }

    private fun buildFinalSpecifications(
        input: AiListingInput,
        aiSpecifications: Map<String, String>
    ): Map<String, String> {
        val merged = linkedMapOf<String, String>()

        normalizeSpecifications(input.specifications).forEach { (key, value) ->
            merged[key] = value
        }
        extractSpecificationsFromText("${input.title}\n${input.description}").forEach { (key, value) ->
            merged.putIfAbsent(key, value)
        }
        normalizeSpecifications(aiSpecifications).forEach { (key, value) ->
            merged[key] = value
        }

        return merged
    }

    private fun normalizeSpecifications(specifications: Map<String, String>): Map<String, String> {
        return specifications
            .mapNotNull { (key, value) ->
                val normalizedKey = key.trim()
                val normalizedValue = value.trim()
                if (normalizedKey.isBlank() || normalizedValue.isBlank()) {
                    null
                } else {
                    normalizedKey to normalizedValue
                }
            }
            .toMap()
    }

    private fun extractSpecificationsFromText(text: String): Map<String, String> {
        val extracted = linkedMapOf<String, String>()
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return extracted

        Regex("""\bM\d(?:\s?(?:Pro|Max|Ultra))?\b""", RegexOption.IGNORE_CASE)
            .find(normalizedText)
            ?.value
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { extracted["Chip"] = it.uppercase() }

        Regex("""\b(\d{1,3})\s*/\s*(\d{2,4})(TB|GB)?\b""", RegexOption.IGNORE_CASE)
            .find(normalizedText)
            ?.let { match ->
                val ram = match.groupValues[1]
                val storage = match.groupValues[2]
                val unit = match.groupValues[3].ifBlank { "GB" }.uppercase()
                extracted.putIfAbsent("RAM", "${ram}GB")
                extracted.putIfAbsent("Storage", "$storage$unit")
            }

        Regex("""\b(\d{1,3})\s?GB\s?(?:RAM|Memory)\b""", RegexOption.IGNORE_CASE)
            .find(normalizedText)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { extracted["RAM"] = "${it}GB" }

        Regex("""\b(\d{2,4})\s?(GB|TB)\s?(?:SSD|HDD|Storage|ROM|Disk|Bộ nhớ|Lưu trữ)\b""", RegexOption.IGNORE_CASE)
            .find(normalizedText)
            ?.let { match ->
                extracted["Storage"] = "${match.groupValues[1]}${match.groupValues[2].uppercase()}"
            }

        return extracted
    }

    private fun buildFinalDescription(
        input: AiListingInput,
        aiDescription: String,
        specifications: Map<String, String>
    ): String {
        val normalizedDescription = aiDescription.trim()
        val isPlaceholderDescription = normalizedDescription.equals("No description provided.", ignoreCase = true) ||
            normalizedDescription.equals("No description available.", ignoreCase = true)

        if (normalizedDescription.isNotBlank() && !isPlaceholderDescription) {
            return normalizedDescription
        }

        val lines = mutableListOf<String>()

        if (input.title.isNotBlank()) {
            lines += input.title.trim()
        }

        if (specifications.isNotEmpty()) {
            val specificationHighlights = specifications.entries
                .take(4)
                .joinToString(separator = ", ") { "${it.key} ${it.value}" }
            lines += "Thông tin nổi bật: $specificationHighlights."
        }

        if (input.condition.isNotBlank()) {
            lines += "Tình trạng: ${input.condition}."
        }

        if (input.quantity.isNotBlank()) {
            lines += "Số lượng: ${input.quantity}."
        }

        if (input.deliveryMethodsAvailable.isNotEmpty()) {
            lines += "Hình thức giao nhận: ${input.deliveryMethodsAvailable.joinToString { it.title }}."
        }

        return lines.joinToString(separator = "\n").ifBlank {
            input.title.trim().ifBlank { "Sản phẩm đã qua sử dụng." }
        }
    }
}
