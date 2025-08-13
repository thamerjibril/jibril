package com.jibril.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    
    @GET("data")
    suspend fun getData(): List<String>
    
    @GET("data/details")
    suspend fun getDataDetails(@Query("id") id: String): String
    
    // Performance optimization: Paginated data loading
    @GET("data/paginated")
    suspend fun getDataPaginated(
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20
    ): List<String>
}