package com.jibril.app.data.cache

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    private val context: Context
) {

    // Performance optimization: Memory cache with LRU eviction
    private val memoryCache = LruCache<String, Any>(
        // Use 1/8th of available memory for cache
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    )

    // Performance optimization: Disk cache directory
    private val diskCacheDir by lazy {
        File(context.cacheDir, "app_cache").apply {
            if (!exists()) mkdirs()
        }
    }

    // Performance optimization: Cache expiration times
    private val cacheExpirationMap = mutableMapOf<String, Long>()
    private val defaultExpirationMs = 5 * 60 * 1000L // 5 minutes

    /**
     * Get data from cache (memory first, then disk)
     */
    suspend fun <T> get(key: String, clazz: Class<T>): T? = withContext(Dispatchers.IO) {
        // Check if cache entry is expired
        if (isCacheExpired(key)) {
            remove(key)
            return@withContext null
        }

        // Try memory cache first
        memoryCache.get(key)?.let { cached ->
            if (clazz.isInstance(cached)) {
                @Suppress("UNCHECKED_CAST")
                return@withContext cached as T
            }
        }

        // Try disk cache if not in memory
        getDiskCache<T>(key)?.let { diskCached ->
            // Store in memory cache for faster future access
            memoryCache.put(key, diskCached)
            return@withContext diskCached
        }

        null
    }

    /**
     * Store data in both memory and disk cache
     */
    suspend fun <T : Serializable> put(
        key: String, 
        data: T, 
        expirationMs: Long = defaultExpirationMs
    ) = withContext(Dispatchers.IO) {
        // Store in memory cache
        memoryCache.put(key, data)
        
        // Store in disk cache
        putDiskCache(key, data)
        
        // Set expiration time
        cacheExpirationMap[key] = System.currentTimeMillis() + expirationMs
    }

    /**
     * Remove item from both caches
     */
    suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        memoryCache.remove(key)
        removeDiskCache(key)
        cacheExpirationMap.remove(key)
    }

    /**
     * Clear all caches
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        clearDiskCache()
        cacheExpirationMap.clear()
    }

    /**
     * Clean expired cache entries
     */
    suspend fun cleanExpiredEntries() = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = cacheExpirationMap.entries
            .filter { currentTime > it.value }
            .map { it.key }
        
        expiredKeys.forEach { key ->
            remove(key)
        }
    }

    private fun isCacheExpired(key: String): Boolean {
        val expirationTime = cacheExpirationMap[key] ?: return false
        return System.currentTimeMillis() > expirationTime
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getDiskCache(key: String): T? {
        return try {
            val file = File(diskCacheDir, key.hashCode().toString())
            if (!file.exists()) return null
            
            ObjectInputStream(FileInputStream(file)).use { input ->
                input.readObject() as T
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun <T : Serializable> putDiskCache(key: String, data: T) {
        try {
            val file = File(diskCacheDir, key.hashCode().toString())
            ObjectOutputStream(FileOutputStream(file)).use { output ->
                output.writeObject(data)
            }
        } catch (e: Exception) {
            // Silently fail disk cache writes
        }
    }

    private fun removeDiskCache(key: String) {
        try {
            val file = File(diskCacheDir, key.hashCode().toString())
            file.delete()
        } catch (e: Exception) {
            // Silently fail disk cache removal
        }
    }

    private fun clearDiskCache() {
        try {
            diskCacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            // Silently fail disk cache clearing
        }
    }

    /**
     * Get cache statistics for performance monitoring
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            memoryHitCount = memoryCache.hitCount(),
            memoryMissCount = memoryCache.missCount(),
            memorySize = memoryCache.size(),
            memoryMaxSize = memoryCache.maxSize(),
            diskCacheFiles = diskCacheDir.listFiles()?.size ?: 0
        )
    }
}

data class CacheStats(
    val memoryHitCount: Long,
    val memoryMissCount: Long,
    val memorySize: Int,
    val memoryMaxSize: Int,
    val diskCacheFiles: Int
) {
    val hitRate: Float get() = if (memoryHitCount + memoryMissCount > 0) {
        memoryHitCount.toFloat() / (memoryHitCount + memoryMissCount)
    } else 0f
}