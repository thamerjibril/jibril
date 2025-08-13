package com.jibril.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.jibril.app.databinding.ActivityMainBinding
import com.jibril.app.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // Performance optimization: Lazy initialization of heavy components
    private val navController by lazy {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Performance optimization: Initialize UI components efficiently
        initializeUI()
        setupObservers()
        
        // Performance optimization: Defer heavy operations
        lifecycleScope.launch {
            initializeHeavyComponents()
        }
    }

    private fun initializeUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Performance optimization: Setup edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        // Performance optimization: Setup navigation lazily
        binding.bottomNavigation?.let { bottomNav ->
            bottomNav.setupWithNavController(navController)
        }
    }

    private fun setupObservers() {
        // Setup ViewModel observers
        viewModel.isLoading.observe(this) { isLoading ->
            // Handle loading states efficiently
            updateLoadingState(isLoading)
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        // Performance optimization: Efficient loading state updates
        binding.progressBar?.let { progressBar ->
            progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private suspend fun initializeHeavyComponents() {
        // Performance optimization: Initialize heavy components in background
        // This includes network setup, database initialization, etc.
        viewModel.initializeData()
    }

    override fun onResume() {
        super.onResume()
        // Performance optimization: Resume operations efficiently
        viewModel.refreshDataIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        // Performance optimization: Pause operations to save resources
        viewModel.pauseOperations()
    }
}