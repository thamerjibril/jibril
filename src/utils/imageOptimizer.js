// Image optimization utilities

export const getOptimizedImageUrl = (url, options = {}) => {
  const {
    width,
    height,
    quality = 80,
    format = 'webp'
  } = options;
  
  // In production, this would integrate with an image CDN
  // For now, return the original URL with query params
  const params = new URLSearchParams();
  if (width) params.append('w', width);
  if (height) params.append('h', height);
  params.append('q', quality);
  params.append('fm', format);
  
  return `${url}?${params.toString()}`;
};

// Lazy load images with Intersection Observer
export const lazyLoadImages = () => {
  const images = document.querySelectorAll('img[data-src]');
  
  const imageObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const img = entry.target;
        
        // Load the image
        img.src = img.dataset.src;
        
        // Add srcset if available
        if (img.dataset.srcset) {
          img.srcset = img.dataset.srcset;
        }
        
        // Clean up
        img.removeAttribute('data-src');
        img.removeAttribute('data-srcset');
        img.classList.add('loaded');
        
        observer.unobserve(img);
      }
    });
  }, {
    rootMargin: '50px 0px',
    threshold: 0.01
  });
  
  images.forEach(img => imageObserver.observe(img));
  
  return imageObserver;
};

// Generate responsive image srcset
export const generateSrcSet = (baseUrl, sizes = [320, 640, 960, 1280, 1920]) => {
  return sizes
    .map(size => `${getOptimizedImageUrl(baseUrl, { width: size })} ${size}w`)
    .join(', ');
};

// Preload critical images
export const preloadImage = (url) => {
  const link = document.createElement('link');
  link.rel = 'preload';
  link.as = 'image';
  link.href = url;
  document.head.appendChild(link);
};

// Convert images to WebP format with fallback
export const createPictureElement = (src, alt, sizes) => {
  const sources = [
    {
      srcset: generateSrcSet(src),
      type: 'image/webp'
    },
    {
      srcset: src,
      type: 'image/jpeg'
    }
  ];
  
  return {
    sources,
    img: {
      src,
      alt,
      sizes: sizes || '100vw',
      loading: 'lazy'
    }
  };
};