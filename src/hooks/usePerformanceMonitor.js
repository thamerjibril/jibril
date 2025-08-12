import { useState, useEffect } from 'react';

export const usePerformanceMonitor = () => {
  const [metrics, setMetrics] = useState({
    fcp: 0,
    lcp: 0,
    tti: 0,
    cls: 0,
    fid: 0
  });
  
  useEffect(() => {
    // First Contentful Paint
    const paintObserver = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (entry.name === 'first-contentful-paint') {
          setMetrics(prev => ({ ...prev, fcp: Math.round(entry.startTime) }));
        }
      }
    });
    
    // Largest Contentful Paint
    const lcpObserver = new PerformanceObserver((list) => {
      const entries = list.getEntries();
      const lastEntry = entries[entries.length - 1];
      setMetrics(prev => ({ ...prev, lcp: Math.round(lastEntry.startTime) }));
    });
    
    // Time to Interactive (simplified)
    const ttiObserver = new PerformanceObserver((list) => {
      const navEntry = list.getEntries()[0];
      if (navEntry && navEntry.loadEventEnd) {
        setMetrics(prev => ({ ...prev, tti: Math.round(navEntry.loadEventEnd) }));
      }
    });
    
    try {
      paintObserver.observe({ entryTypes: ['paint'] });
      lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] });
      ttiObserver.observe({ entryTypes: ['navigation'] });
    } catch (e) {
      // Performance Observer not supported
      console.log('Performance monitoring not available');
    }
    
    return () => {
      paintObserver.disconnect();
      lcpObserver.disconnect();
      ttiObserver.disconnect();
    };
  }, []);
  
  return metrics;
};