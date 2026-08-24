package com.restaurant.staff.network

import com.squareup.moshi.JsonClass

/**
 * Mirror of the server's ApiResponse envelope. The server returns:
 *  { success, data, error: { code, message, fields? }, meta: { lang, fallback? } }
 */
@JsonClass(generateAdapter = true)
data class ApiEnvelopeDto<T>(
    val success: Boolean? = null,
    val data: T? = null,
    val error: ApiErrorDto? = null,
    val meta: ApiMetaDto? = null
)

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    val code: String? = null,
    val message: String? = null,
    val fields: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class ApiMetaDto(
    val lang: String? = null,
    val fallback: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class HealthDto(
    val status: String? = null,
    val version: String? = null,
    val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class ServerInfoDto(
    val server: String? = null,
    val port: Int? = null,
    val protocol: String? = null,
    val version: String? = null
)