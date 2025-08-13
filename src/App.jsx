import React, { lazy, Suspense } from 'react';
import { ErrorBoundary } from './components/ErrorBoundary';
import { LoadingSpinner } from './components/LoadingSpinner';

// Lazy load components for code splitting
const Header = lazy(() => import('./components/Header'));
const Dashboard = lazy(() => import('./components/Dashboard'));
const Analytics = lazy(() => import('./components/Analytics'));

// Preload critical components
const preloadComponent = (component) => {
  if ('requestIdleCallback' in window) {
    requestIdleCallback(() => component());
  } else {
    setTimeout(() => component(), 1);
  }
};

// Preload components that are likely to be used
preloadComponent(() => import('./components/Dashboard'));

function App() {
  const [showAnalytics, setShowAnalytics] = React.useState(false);
  
  // Use intersection observer for lazy loading below-the-fold content
  React.useEffect(() => {
    const options = {
      root: null,
      rootMargin: '50px',
      threshold: 0.01
    };
    
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          // Load component when it's about to come into view
          const lazyElement = entry.target;
          lazyElement.classList.add('loaded');
        }
      });
    }, options);
    
    // Observe all lazy-load elements
    document.querySelectorAll('.lazy-load').forEach(el => {
      observer.observe(el);
    });
    
    return () => observer.disconnect();
  }, []);
  
  return (
    <ErrorBoundary>
      <div className="app">
        <Suspense fallback={<LoadingSpinner />}>
          <Header />
        </Suspense>
        
        <main className="main-content">
          <Suspense fallback={<LoadingSpinner />}>
            <Dashboard />
          </Suspense>
          
          <div className="analytics-section lazy-load">
            <button 
              onClick={() => setShowAnalytics(!showAnalytics)}
              className="analytics-toggle"
            >
              {showAnalytics ? 'Hide' : 'Show'} Analytics
            </button>
            
            {showAnalytics && (
              <Suspense fallback={<LoadingSpinner />}>
                <Analytics />
              </Suspense>
            )}
          </div>
        </main>
      </div>
    </ErrorBoundary>
  );
}

export default App;