package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequestDto(
    @param:Json(name = "contents") val contents: List<ContentDto>,
    @param:Json(name = "systemInstruction") val systemInstruction: ContentDto? = null,
    @param:Json(name = "generationConfig") val generationConfig: GenerationConfigDto? = null
)

@JsonClass(generateAdapter = true)
data class ContentDto(
    @param:Json(name = "parts") val parts: List<PartDto>,
    @param:Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class PartDto(
    @param:Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfigDto(
    @param:Json(name = "temperature") val temperature: Float? = null,
    @param:Json(name = "topP") val topP: Float? = null,
    @param:Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null,
    @param:Json(name = "responseMimeType") val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponseDto(
    @param:Json(name = "candidates") val candidates: List<CandidateDto>? = null,
    @param:Json(name = "error") val error: GeminiErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class CandidateDto(
    @param:Json(name = "content") val content: ContentDto? = null,
    @param:Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorDto(
    @param:Json(name = "code") val code: Int? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "status") val status: String? = null
)
