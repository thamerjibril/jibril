package com.jibril.app.data.repository

import com.jibril.app.data.cache.CacheManager
import com.jibril.app.data.remote.ApiService
import com.jibril.app.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepository @Inject constructor(
    private val apiService: ApiService,
    private val cacheManager: CacheManager
) {

    // Performance optimization: Track ongoing operations
    private var networkJob: Job? = null
    private val cacheKey = "main_data"
    
    // Performance optimization: Cache-first strategy with network fallback
    suspend fun getData(): Resource<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Try cache first
            getCachedData().takeIf { it.isNotEmpty() }?.let { cachedData ->
                return@withContext Resource.Success(cachedData)
            }

            // Fetch from network if cache is empty
            fetchFromNetwork()
        } catch (exception: Exception) {
            Resource.Error(exception.message ?: "Unknown error")
        }
    }

    suspend fun getCachedData(): List<String> = withContext(Dispatchers.IO) {
        cacheManager.get(cacheKey, List::class.java) as? List<String> ?: emptyList()
    }

    suspend fun refreshData(): Resource<List<String>> = withContext(Dispatchers.IO) {
        // Force network refresh
        fetchFromNetwork()
    }

    private suspend fun fetchFromNetwork(): Resource<List<String>> {
        return try {
            // Simulate network call - replace with actual API call
            val networkData = apiService.getData()
            
            // Cache the fresh data
            cacheManager.put(cacheKey, ArrayList(networkData))
            
            Resource.Success(networkData)
        } catch (exception: Exception) {
            // Return cached data if network fails
            val cachedData = getCachedData()
            if (cachedData.isNotEmpty()) {
                Resource.Success(cachedData)
            } else {
                Resource.Error(exception.message ?: "Network error")
            }
        }
    }

    fun pauseOperations() {
        // Cancel ongoing network operations to save battery/data
        networkJob?.cancel()
    }

    suspend fun cleanup() = withContext(Dispatchers.IO) {
        // Clean up expired cache entries
        cacheManager.cleanExpiredEntries()
        networkJob?.cancel()
    }

    // Performance monitoring: Get cache statistics
    fun getCacheStats() = cacheManager.getCacheStats()
}