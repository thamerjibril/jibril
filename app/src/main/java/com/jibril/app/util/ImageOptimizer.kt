package com.jibril.app.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

object ImageOptimizer {

    /**
     * Performance optimized image loading with Glide
     */
    fun loadImage(
        context: Context,
        imageView: ImageView,
        url: String,
        placeholder: Int? = null,
        error: Int? = null
    ) {
        val requestBuilder = Glide.with(context)
            .load(url)
            .apply(getOptimizedRequestOptions())

        placeholder?.let { requestBuilder.placeholder(it) }
        error?.let { requestBuilder.error(it) }

        requestBuilder.into(imageView)
    }

    /**
     * Load image with custom transformations
     */
    fun loadImageWithTransform(
        context: Context,
        imageView: ImageView,
        url: String,
        cornerRadius: Int = 0,
        placeholder: Int? = null
    ) {
        val requestOptions = getOptimizedRequestOptions()
            .transform(CenterCrop(), RoundedCorners(cornerRadius))

        val requestBuilder = Glide.with(context)
            .load(url)
            .apply(requestOptions)

        placeholder?.let { requestBuilder.placeholder(it) }

        requestBuilder.into(imageView)
    }

    /**
     * Preload images for better performance
     */
    fun preloadImage(context: Context, url: String) {
        Glide.with(context)
            .load(url)
            .apply(getOptimizedRequestOptions())
            .preload()
    }

    /**
     * Load image with callback for custom handling
     */
    fun loadImageWithCallback(
        context: Context,
        url: String,
        width: Int,
        height: Int,
        onSuccess: (Drawable) -> Unit,
        onError: (() -> Unit)? = null
    ) {
        Glide.with(context)
            .load(url)
            .apply(getOptimizedRequestOptions())
            .into(object : CustomTarget<Drawable>(width, height) {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    onSuccess(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Handle cleanup if needed
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    onError?.invoke()
                }
            })
    }

    /**
     * Clear image cache when memory is low
     */
    fun clearMemoryCache(context: Context) {
        Glide.get(context).clearMemory()
    }

    /**
     * Clear disk cache (should be called from background thread)
     */
    fun clearDiskCache(context: Context) {
        Thread {
            Glide.get(context).clearDiskCache()
        }.start()
    }

    private fun getOptimizedRequestOptions(): RequestOptions {
        return RequestOptions()
            // Performance optimization: Enable disk caching
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            // Performance optimization: Skip memory cache for large images
            .skipMemoryCache(false)
            // Performance optimization: Optimize for display size
            .fitCenter()
            // Performance optimization: Enable hardware bitmaps for better memory usage
            .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)
    }

    /**
     * Get cache size information for performance monitoring
     */
    fun getCacheSize(context: Context): Long {
        return try {
            val cacheDir = Glide.getPhotoCacheDir(context)
            cacheDir?.let { dir ->
                dir.listFiles()?.sumOf { it.length() } ?: 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}