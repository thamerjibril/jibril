import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import viteCompression from 'vite-plugin-compression';
import { VitePWA } from 'vite-plugin-pwa';
import { visualizer } from 'rollup-plugin-visualizer';

export default defineConfig({
  plugins: [
    react(),
    
    // Gzip and Brotli compression
    viteCompression({
      algorithm: 'gzip',
      ext: '.gz',
    }),
    viteCompression({
      algorithm: 'brotliCompress',
      ext: '.br',
    }),
    
    // PWA support for offline functionality
    VitePWA({
      registerType: 'autoUpdate',
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,webp,woff2}'],
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/fonts\.googleapis\.com\/.*/i,
            handler: 'CacheFirst',
            options: {
              cacheName: 'google-fonts-cache',
              expiration: {
                maxEntries: 10,
                maxAgeSeconds: 60 * 60 * 24 * 365, // 1 year
              },
              cacheableResponse: {
                statuses: [0, 200],
              },
            },
          },
        ],
      },
      manifest: {
        name: 'Jibril App',
        short_name: 'Jibril',
        theme_color: '#ffffff',
        background_color: '#ffffff',
        display: 'standalone',
        scope: '/',
        start_url: '/',
      },
    }),
    
    // Bundle visualizer
    visualizer({
      open: false,
      gzipSize: true,
      brotliSize: true,
    }),
  ],
  
  build: {
    // Enable minification
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
      },
    },
    
    // Split vendor chunks for better caching
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          // Add more vendor chunks as needed
        },
      },
    },
    
    // Optimize chunk size
    chunkSizeWarningLimit: 500,
    
    // Enable source maps for production debugging
    sourcemap: false,
    
    // Asset inlining threshold
    assetsInlineLimit: 4096,
    
    // CSS code splitting
    cssCodeSplit: true,
    
    // Target modern browsers for smaller bundles
    target: 'es2020',
  },
  
  // Optimize deps pre-bundling
  optimizeDeps: {
    include: ['react', 'react-dom'],
  },
  
  // Server configuration
  server: {
    // Enable HTTPS in development
    https: false,
    // Enable CORS
    cors: true,
  },
});