// Performance monitoring utilities

class PerformanceMonitor {
  constructor() {
    this.metrics = {
      fcp: null,
      lcp: null,
      fid: null,
      cls: null,
      ttfb: null,
      tti: null,
      tbt: null,
      longTasks: []
    };
    
    this.observers = new Map();
    this.initializeObservers();
  }
  
  initializeObservers() {
    // First Contentful Paint & Largest Contentful Paint
    if ('PerformanceObserver' in window) {
      try {
        // Paint timing
        const paintObserver = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            if (entry.name === 'first-contentful-paint') {
              this.metrics.fcp = Math.round(entry.startTime);
              this.reportMetric('FCP', this.metrics.fcp);
            }
          }
        });
        paintObserver.observe({ entryTypes: ['paint'] });
        this.observers.set('paint', paintObserver);
        
        // Largest Contentful Paint
        const lcpObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries();
          const lastEntry = entries[entries.length - 1];
          this.metrics.lcp = Math.round(lastEntry.startTime);
          this.reportMetric('LCP', this.metrics.lcp);
        });
        lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] });
        this.observers.set('lcp', lcpObserver);
        
        // First Input Delay
        const fidObserver = new PerformanceObserver((list) => {
          const firstInput = list.getEntries()[0];
          this.metrics.fid = Math.round(firstInput.processingStart - firstInput.startTime);
          this.reportMetric('FID', this.metrics.fid);
        });
        fidObserver.observe({ entryTypes: ['first-input'] });
        this.observers.set('fid', fidObserver);
        
        // Layout Shift
        let clsValue = 0;
        let clsEntries = [];
        const clsObserver = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            if (!entry.hadRecentInput) {
              clsValue += entry.value;
              clsEntries.push(entry);
            }
          }
          this.metrics.cls = clsValue;
          this.reportMetric('CLS', this.metrics.cls);
        });
        clsObserver.observe({ entryTypes: ['layout-shift'] });
        this.observers.set('cls', clsObserver);
        
        // Long Tasks
        const longTaskObserver = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            this.metrics.longTasks.push({
              startTime: entry.startTime,
              duration: entry.duration,
              name: entry.name
            });
            this.reportLongTask(entry);
          }
        });
        longTaskObserver.observe({ entryTypes: ['longtask'] });
        this.observers.set('longtask', longTaskObserver);
        
      } catch (e) {
        console.warn('Some performance observers not supported:', e);
      }
    }
    
    // Navigation timing
    if ('performance' in window && 'timing' in performance) {
      window.addEventListener('load', () => {
        setTimeout(() => {
          const timing = performance.timing;
          const navigation = performance.getEntriesByType('navigation')[0];
          
          // Time to First Byte
          this.metrics.ttfb = timing.responseStart - timing.fetchStart;
          this.reportMetric('TTFB', this.metrics.ttfb);
          
          // Time to Interactive (simplified)
          this.metrics.tti = timing.loadEventEnd - timing.fetchStart;
          this.reportMetric('TTI', this.metrics.tti);
          
          // Total Blocking Time (approximation)
          if (navigation) {
            const fcp = this.metrics.fcp || 0;
            const tti = this.metrics.tti || 0;
            const longTasksDuringLoad = this.metrics.longTasks.filter(
              task => task.startTime > fcp && task.startTime < tti
            );
            this.metrics.tbt = longTasksDuringLoad.reduce(
              (total, task) => total + Math.max(0, task.duration - 50),
              0
            );
            this.reportMetric('TBT', this.metrics.tbt);
          }
        }, 0);
      });
    }
  }
  
  reportMetric(name, value) {
    // Log to console in development
    if (process.env.NODE_ENV === 'development') {
      console.log(`[Performance] ${name}:`, value);
    }
    
    // Send to analytics in production
    if (process.env.NODE_ENV === 'production' && window.gtag) {
      window.gtag('event', 'web_vitals', {
        event_category: 'Performance',
        event_label: name,
        value: Math.round(value),
        non_interaction: true
      });
    }
  }
  
  reportLongTask(entry) {
    if (process.env.NODE_ENV === 'development') {
      console.warn('[Performance] Long task detected:', {
        duration: Math.round(entry.duration),
        startTime: Math.round(entry.startTime)
      });
    }
  }
  
  getMetrics() {
    return { ...this.metrics };
  }
  
  // Mark custom performance marks
  mark(name) {
    if ('performance' in window && 'mark' in performance) {
      performance.mark(name);
    }
  }
  
  // Measure between marks
  measure(name, startMark, endMark) {
    if ('performance' in window && 'measure' in performance) {
      try {
        performance.measure(name, startMark, endMark);
        const measures = performance.getEntriesByName(name, 'measure');
        const duration = measures[measures.length - 1]?.duration;
        this.reportMetric(name, duration);
        return duration;
      } catch (e) {
        console.error('Failed to measure:', e);
      }
    }
  }
  
  // Track component render performance
  trackComponentRender(componentName, renderTime) {
    this.reportMetric(`Component_${componentName}`, renderTime);
  }
  
  // Memory usage monitoring
  getMemoryUsage() {
    if ('memory' in performance) {
      return {
        usedJSHeapSize: performance.memory.usedJSHeapSize,
        totalJSHeapSize: performance.memory.totalJSHeapSize,
        jsHeapSizeLimit: performance.memory.jsHeapSizeLimit
      };
    }
    return null;
  }
  
  // Network monitoring
  getNetworkInfo() {
    if ('connection' in navigator) {
      return {
        effectiveType: navigator.connection.effectiveType,
        downlink: navigator.connection.downlink,
        rtt: navigator.connection.rtt,
        saveData: navigator.connection.saveData
      };
    }
    return null;
  }
  
  // Bundle size tracking
  trackBundleSize() {
    const scripts = Array.from(document.getElementsByTagName('script'));
    const stylesheets = Array.from(document.getElementsByTagName('link'))
      .filter(link => link.rel === 'stylesheet');
    
    const totalScriptSize = scripts.reduce((total, script) => {
      if (script.src) {
        // In production, you'd fetch actual sizes
        return total + 1;
      }
      return total;
    }, 0);
    
    const totalStyleSize = stylesheets.length;
    
    return {
      scripts: scripts.length,
      stylesheets: stylesheets.length,
      estimatedSize: `${totalScriptSize + totalStyleSize} resources`
    };
  }
  
  // Cleanup
  disconnect() {
    this.observers.forEach(observer => observer.disconnect());
    this.observers.clear();
  }
}

// Export singleton instance
export const performanceMonitor = new PerformanceMonitor();

// React performance profiler wrapper
export const withPerformanceTracking = (Component, componentName) => {
  return (props) => {
    const startTime = performance.now();
    
    React.useEffect(() => {
      const renderTime = performance.now() - startTime;
      performanceMonitor.trackComponentRender(componentName, renderTime);
    });
    
    return <Component {...props} />;
  };
};