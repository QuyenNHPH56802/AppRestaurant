package com.restaurant.staff.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface CategoryApi {
    @GET("api/categories")
    suspend fun list(@Header("Authorization") bearer: String,
                     @Query("lang") lang: String = "vi"): ApiEnvelopeDto<List<CategoryViewDto>>
}

interface FoodApi {
    @GET("api/foods")
    suspend fun list(
        @Header("Authorization") bearer: String,
        @Query("lang") lang: String = "vi",
        @Query("q") q: String? = null,
        @Query("categoryId") categoryId: Long? = null,
        @Query("status") status: String? = null,
        @Query("featured") featured: Boolean? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): ApiEnvelopeDto<FoodPageDto>

    @GET("api/foods/featured")
    suspend fun featured(
        @Header("Authorization") bearer: String,
        @Query("lang") lang: String = "vi",
        @Query("limit") limit: Int = 10
    ): ApiEnvelopeDto<List<FoodViewDto>>

    @GET("api/foods/{id}")
    suspend fun detail(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long,
        @Query("lang") lang: String = "vi"
    ): ApiEnvelopeDto<FoodViewDto>
}

interface StoreApi {
    @GET("api/store")
    suspend fun get(@Header("Authorization") bearer: String,
                    @Query("lang") lang: String = "vi"): ApiEnvelopeDto<StoreViewDto>
}

@JsonClass(generateAdapter = true)
data class CategoryViewDto(
    val id: Long? = null,
    val sortOrder: Int? = null,
    val status: String? = null,
    val imageUrl: String? = null,
    val name: String? = null,
    val description: String? = null,
    val lang: String? = null,
    val fallback: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class FoodViewDto(
    val id: Long? = null,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val price: String? = null,
    val imageUrl: String? = null,
    val status: String? = null,
    val featured: Boolean? = null,
    val sortOrder: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val ingredients: String? = null,
    val portion: String? = null,
    val lang: String? = null,
    val fallback: List<String>? = null,
    val images: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class FoodPageDto(
    val items: List<FoodViewDto>? = null,
    val page: Int? = null,
    val size: Int? = null,
    val total: Long? = null,
    val totalPages: Int? = null
)

@JsonClass(generateAdapter = true)
data class StoreViewDto(
    val id: Long? = null,
    val logoUrl: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val openingHours: String? = null,
    val name: String? = null,
    val description: String? = null,
    val lang: String? = null,
    val fallback: List<String>? = null
)