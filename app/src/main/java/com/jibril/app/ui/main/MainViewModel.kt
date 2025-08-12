package com.jibril.app.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jibril.app.data.repository.DataRepository
import com.jibril.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataRepository
) : ViewModel() {

    // Performance optimization: Use private mutable and expose immutable
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _data = MutableLiveData<Resource<List<String>>>()
    val data: LiveData<Resource<List<String>>> = _data

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Performance optimization: Track ongoing operations to prevent duplicates
    private var dataLoadingJob: Job? = null
    private var lastRefreshTime = 0L
    private val refreshThrottleMs = 30000L // 30 seconds

    init {
        // Performance optimization: Don't load data immediately in init
        // Let the UI trigger data loading when ready
    }

    suspend fun initializeData() {
        // Performance optimization: Initialize data in background
        withContext(Dispatchers.IO) {
            loadDataIfNeeded()
        }
    }

    private suspend fun loadDataIfNeeded() {
        // Performance optimization: Check if data is already cached
        val cachedData = repository.getCachedData()
        if (cachedData.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                _data.value = Resource.Success(cachedData)
            }
        } else {
            loadData()
        }
    }

    fun loadData() {
        // Performance optimization: Cancel previous job if running
        dataLoadingJob?.cancel()
        
        dataLoadingJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Performance optimization: Load from cache first, then network
                val result = repository.getData()
                _data.value = result

            } catch (exception: Exception) {
                _error.value = exception.message
                _data.value = Resource.Error(exception.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshDataIfNeeded() {
        // Performance optimization: Throttle refresh to prevent excessive network calls
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRefreshTime > refreshThrottleMs) {
            lastRefreshTime = currentTime
            refreshData()
        }
    }

    private fun refreshData() {
        // Performance optimization: Only refresh if not already loading
        if (_isLoading.value == true) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.refreshData()
                _data.value = result
            } catch (exception: Exception) {
                _error.value = exception.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun pauseOperations() {
        // Performance optimization: Cancel ongoing operations when activity pauses
        dataLoadingJob?.cancel()
        repository.pauseOperations()
    }

    fun retryLastOperation() {
        if (_error.value != null) {
            loadData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Performance optimization: Clean up resources
        dataLoadingJob?.cancel()
        repository.cleanup()
    }
}