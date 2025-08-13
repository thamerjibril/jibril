// Cache management utilities

const CACHE_VERSION = 'v1';
const CACHE_NAME = `jibril-cache-${CACHE_VERSION}`;

// Cache strategies
export const CacheStrategy = {
  NETWORK_FIRST: 'network-first',
  CACHE_FIRST: 'cache-first',
  NETWORK_ONLY: 'network-only',
  CACHE_ONLY: 'cache-only',
  STALE_WHILE_REVALIDATE: 'stale-while-revalidate'
};

// Check if caches API is available
const isCacheAvailable = 'caches' in window;

// Cache API response
export const cacheApiResponse = async (url, response, ttl = 3600000) => {
  if (!isCacheAvailable) return;
  
  try {
    const cache = await caches.open(CACHE_NAME);
    const clonedResponse = response.clone();
    
    // Add cache metadata
    const headers = new Headers(clonedResponse.headers);
    headers.append('sw-cache-expire', new Date(Date.now() + ttl).toISOString());
    
    const responseWithMeta = new Response(clonedResponse.body, {
      status: clonedResponse.status,
      statusText: clonedResponse.statusText,
      headers
    });
    
    await cache.put(url, responseWithMeta);
  } catch (error) {
    console.error('Failed to cache response:', error);
  }
};

// Get cached response
export const getCachedResponse = async (url) => {
  if (!isCacheAvailable) return null;
  
  try {
    const cache = await caches.open(CACHE_NAME);
    const cachedResponse = await cache.match(url);
    
    if (!cachedResponse) return null;
    
    // Check if cache is expired
    const expireDate = cachedResponse.headers.get('sw-cache-expire');
    if (expireDate && new Date(expireDate) < new Date()) {
      await cache.delete(url);
      return null;
    }
    
    return cachedResponse;
  } catch (error) {
    console.error('Failed to get cached response:', error);
    return null;
  }
};

// Fetch with cache
export const fetchWithCache = async (url, options = {}) => {
  const {
    strategy = CacheStrategy.NETWORK_FIRST,
    ttl = 3600000,
    ...fetchOptions
  } = options;
  
  switch (strategy) {
    case CacheStrategy.NETWORK_FIRST:
      try {
        const response = await fetch(url, fetchOptions);
        if (response.ok) {
          await cacheApiResponse(url, response, ttl);
        }
        return response;
      } catch (error) {
        const cachedResponse = await getCachedResponse(url);
        if (cachedResponse) return cachedResponse;
        throw error;
      }
      
    case CacheStrategy.CACHE_FIRST:
      const cachedResponse = await getCachedResponse(url);
      if (cachedResponse) return cachedResponse;
      
      const response = await fetch(url, fetchOptions);
      if (response.ok) {
        await cacheApiResponse(url, response, ttl);
      }
      return response;
      
    case CacheStrategy.STALE_WHILE_REVALIDATE:
      const cached = await getCachedResponse(url);
      const fetchPromise = fetch(url, fetchOptions).then(res => {
        if (res.ok) {
          cacheApiResponse(url, res, ttl);
        }
        return res;
      });
      
      return cached || fetchPromise;
      
    case CacheStrategy.NETWORK_ONLY:
      return fetch(url, fetchOptions);
      
    case CacheStrategy.CACHE_ONLY:
      const cacheOnly = await getCachedResponse(url);
      if (!cacheOnly) {
        throw new Error('No cached response available');
      }
      return cacheOnly;
      
    default:
      return fetch(url, fetchOptions);
  }
};

// Clear old caches
export const clearOldCaches = async () => {
  if (!isCacheAvailable) return;
  
  const cacheNames = await caches.keys();
  const currentCaches = [CACHE_NAME];
  
  await Promise.all(
    cacheNames.map(cacheName => {
      if (!currentCaches.includes(cacheName)) {
        return caches.delete(cacheName);
      }
    })
  );
};

// Preload critical resources
export const preloadResources = async (urls) => {
  if (!isCacheAvailable) return;
  
  const cache = await caches.open(CACHE_NAME);
  await cache.addAll(urls);
};

// Memory cache for runtime data
class MemoryCache {
  constructor(maxSize = 100, ttl = 300000) {
    this.cache = new Map();
    this.maxSize = maxSize;
    this.ttl = ttl;
  }
  
  set(key, value) {
    // Implement LRU eviction if needed
    if (this.cache.size >= this.maxSize) {
      const firstKey = this.cache.keys().next().value;
      this.cache.delete(firstKey);
    }
    
    this.cache.set(key, {
      value,
      expires: Date.now() + this.ttl
    });
  }
  
  get(key) {
    const item = this.cache.get(key);
    
    if (!item) return null;
    
    if (Date.now() > item.expires) {
      this.cache.delete(key);
      return null;
    }
    
    return item.value;
  }
  
  clear() {
    this.cache.clear();
  }
}

export const memoryCache = new MemoryCache();