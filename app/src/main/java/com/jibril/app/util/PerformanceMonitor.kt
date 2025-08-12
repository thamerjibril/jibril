package com.jibril.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jibril.app.BuildConfig
import java.text.DecimalFormat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceMonitor @Inject constructor(
    private val context: Context
) : DefaultLifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false
    private val performanceMetrics = mutableMapOf<String, Any>()
    private val decimalFormat = DecimalFormat("#.##")

    // Performance tracking variables
    private var appStartTime = 0L
    private var frameDropCount = 0
    private var networkRequestCount = 0
    private var cacheHitCount = 0
    private var cacheMissCount = 0

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MONITORING_INTERVAL = 5000L // 5 seconds
    }

    init {
        if (BuildConfig.DEBUG) {
            appStartTime = System.currentTimeMillis()
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        if (BuildConfig.DEBUG) {
            startMonitoring()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopMonitoring()
    }

    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        schedulePerformanceCheck()
        Log.d(TAG, "Performance monitoring started")
    }

    fun stopMonitoring() {
        isMonitoring = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Performance monitoring stopped")
    }

    private fun schedulePerformanceCheck() {
        if (!isMonitoring) return

        handler.postDelayed({
            collectPerformanceMetrics()
            schedulePerformanceCheck()
        }, MONITORING_INTERVAL)
    }

    private fun collectPerformanceMetrics() {
        try {
            // Memory metrics
            val memoryInfo = getMemoryInfo()
            performanceMetrics["memory_used_mb"] = memoryInfo.usedMemoryMB
            performanceMetrics["memory_available_mb"] = memoryInfo.availableMemoryMB
            performanceMetrics["memory_total_mb"] = memoryInfo.totalMemoryMB

            // CPU metrics
            performanceMetrics["cpu_usage_percent"] = getCpuUsage()

            // App-specific metrics
            performanceMetrics["network_requests"] = networkRequestCount
            performanceMetrics["cache_hit_rate"] = getCacheHitRate()
            performanceMetrics["app_uptime_minutes"] = getAppUptimeMinutes()

            logPerformanceMetrics()

        } catch (e: Exception) {
            Log.e(TAG, "Error collecting performance metrics", e)
        }
    }

    private fun getMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()

        return MemoryInfo(
            usedMemoryMB = usedMemory / (1024 * 1024),
            availableMemoryMB = memInfo.availMem / (1024 * 1024),
            totalMemoryMB = memInfo.totalMem / (1024 * 1024)
        )
    }

    private fun getCpuUsage(): Double {
        return try {
            val cpuInfo = Debug.threadCpuTimeNanos()
            // This is a simplified CPU usage calculation
            // In production, you might want to use more sophisticated methods
            (cpuInfo / 1000000.0) % 100.0
        } catch (e: Exception) {
            0.0
        }
    }

    private fun getCacheHitRate(): Double {
        val totalRequests = cacheHitCount + cacheMissCount
        return if (totalRequests > 0) {
            (cacheHitCount.toDouble() / totalRequests) * 100
        } else 0.0
    }

    private fun getAppUptimeMinutes(): Double {
        return (System.currentTimeMillis() - appStartTime) / (1000.0 * 60.0)
    }

    private fun logPerformanceMetrics() {
        if (!BuildConfig.DEBUG) return

        val memoryUsed = performanceMetrics["memory_used_mb"] as Long
        val cpuUsage = performanceMetrics["cpu_usage_percent"] as Double
        val cacheHitRate = performanceMetrics["cache_hit_rate"] as Double
        val uptime = performanceMetrics["app_uptime_minutes"] as Double

        Log.d(TAG, "=== Performance Metrics ===")
        Log.d(TAG, "Memory Used: ${memoryUsed}MB")
        Log.d(TAG, "CPU Usage: ${decimalFormat.format(cpuUsage)}%")
        Log.d(TAG, "Cache Hit Rate: ${decimalFormat.format(cacheHitRate)}%")
        Log.d(TAG, "App Uptime: ${decimalFormat.format(uptime)} minutes")
        Log.d(TAG, "Network Requests: $networkRequestCount")
        Log.d(TAG, "===========================")
    }

    // Methods to track specific events
    fun trackNetworkRequest() {
        networkRequestCount++
    }

    fun trackCacheHit() {
        cacheHitCount++
    }

    fun trackCacheMiss() {
        cacheMissCount++
    }

    fun trackFrameDrop() {
        frameDropCount++
    }

    fun trackCustomMetric(key: String, value: Any) {
        performanceMetrics[key] = value
    }

    fun getPerformanceReport(): PerformanceReport {
        return PerformanceReport(
            memoryUsageMB = performanceMetrics["memory_used_mb"] as? Long ?: 0L,
            cpuUsagePercent = performanceMetrics["cpu_usage_percent"] as? Double ?: 0.0,
            networkRequestCount = networkRequestCount,
            cacheHitRate = getCacheHitRate(),
            frameDropCount = frameDropCount,
            appUptimeMinutes = getAppUptimeMinutes(),
            customMetrics = performanceMetrics.toMap()
        )
    }

    // Check for performance issues
    fun checkPerformanceIssues(): List<PerformanceIssue> {
        val issues = mutableListOf<PerformanceIssue>()

        val memoryUsed = performanceMetrics["memory_used_mb"] as? Long ?: 0L
        if (memoryUsed > 200) { // 200MB threshold
            issues.add(PerformanceIssue.HighMemoryUsage(memoryUsed))
        }

        val cacheHitRate = getCacheHitRate()
        if (cacheHitRate < 50.0) { // 50% threshold
            issues.add(PerformanceIssue.LowCacheHitRate(cacheHitRate))
        }

        if (frameDropCount > 10) {
            issues.add(PerformanceIssue.FrequentFrameDrops(frameDropCount))
        }

        return issues
    }

    private data class MemoryInfo(
        val usedMemoryMB: Long,
        val availableMemoryMB: Long,
        val totalMemoryMB: Long
    )
}

data class PerformanceReport(
    val memoryUsageMB: Long,
    val cpuUsagePercent: Double,
    val networkRequestCount: Int,
    val cacheHitRate: Double,
    val frameDropCount: Int,
    val appUptimeMinutes: Double,
    val customMetrics: Map<String, Any>
)

sealed class PerformanceIssue(val description: String) {
    data class HighMemoryUsage(val memoryMB: Long) : PerformanceIssue("High memory usage: ${memoryMB}MB")
    data class LowCacheHitRate(val hitRate: Double) : PerformanceIssue("Low cache hit rate: ${"%.1f".format(hitRate)}%")
    data class FrequentFrameDrops(val dropCount: Int) : PerformanceIssue("Frequent frame drops: $dropCount")
}