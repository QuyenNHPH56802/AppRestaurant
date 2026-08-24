package com.restaurant.staff.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory for OkHttp + Retrofit. baseUrl is dynamic; we recreate the Retrofit
 * instance whenever the user changes the server (Pairing flow). Hilt would
 * ordinarily want a single instance; we use a small holder to swap in/out.
 */
object NetworkClient {

    fun moshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun okHttp(logging: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
        if (logging) {
            val log = HttpLoggingInterceptor()
            log.level = HttpLoggingInterceptor.Level.BASIC
            builder.addInterceptor(log)
        }
        return builder.build()
    }

    fun retrofit(client: OkHttpClient, baseUrl: String, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
}