package com.restaurant.staff.repository

import com.restaurant.staff.network.ApiClientProvider
import com.restaurant.staff.network.CategoryViewDto
import com.restaurant.staff.network.FoodPageDto
import com.restaurant.staff.network.FoodViewDto
import com.restaurant.staff.network.StoreViewDto
import com.restaurant.staff.storage.SessionStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val api: ApiClientProvider,
    private val session: SessionStore
) {
    suspend fun list(lang: String): List<CategoryViewDto> {
        val bearer = "Bearer ${session.currentToken() ?: ""}"
        val r = api.categoryApi().list(bearer, lang)
        if (r.success != true) throw RuntimeException(r.error?.message ?: "Failed")
        return r.data.orEmpty()
    }
}

@Singleton
class FoodRepository @Inject constructor(
    private val api: ApiClientProvider,
    private val session: SessionStore
) {
    private suspend fun bearer() = "Bearer ${session.currentToken() ?: ""}"

    suspend fun list(
        lang: String,
        q: String? = null,
        categoryId: Long? = null,
        status: String? = null,
        featured: Boolean? = null,
        page: Int = 0,
        size: Int = 50
    ): FoodPageDto {
        val r = api.foodApi().list(bearer(), lang, q, categoryId, status, featured, page, size)
        if (r.success != true) throw RuntimeException(r.error?.message ?: "Failed")
        return r.data ?: FoodPageDto(emptyList(), 0, size, 0, 0)
    }

    suspend fun featured(lang: String, limit: Int = 10): List<FoodViewDto> {
        val r = api.foodApi().featured(bearer(), lang, limit)
        if (r.success != true) throw RuntimeException(r.error?.message ?: "Failed")
        return r.data.orEmpty()
    }

    suspend fun detail(lang: String, id: Long): FoodViewDto? {
        val r = api.foodApi().detail(bearer(), id, lang)
        if (r.success != true) throw RuntimeException(r.error?.message ?: "Failed")
        return r.data
    }
}

@Singleton
class StoreRepository @Inject constructor(
    private val api: ApiClientProvider,
    private val session: SessionStore
) {
    suspend fun get(lang: String): StoreViewDto? {
        val bearer = "Bearer ${session.currentToken() ?: ""}"
        val r = api.storeApi().get(bearer, lang)
        return r.data
    }
}