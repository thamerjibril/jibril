import React, { useMemo, useCallback } from 'react';
import { usePerformanceMonitor } from '../hooks/usePerformanceMonitor';

const Dashboard = () => {
  const [data, setData] = React.useState([]);
  const performanceMetrics = usePerformanceMonitor();
  
  // Memoize expensive calculations
  const processedData = useMemo(() => {
    return data.map(item => ({
      ...item,
      processed: true,
      timestamp: Date.now()
    }));
  }, [data]);
  
  // Use callback to prevent unnecessary re-renders
  const handleLoadData = useCallback(() => {
    // Simulate data loading
    const newData = Array.from({ length: 10 }, (_, i) => ({
      id: i,
      value: Math.random() * 100,
      label: `Item ${i + 1}`
    }));
    setData(newData);
  }, []);
  
  return (
    <div className="dashboard" id="dashboard">
      <h2>Dashboard</h2>
      
      <div className="metrics">
        <h3>Performance Metrics</h3>
        <ul>
          <li>FCP: {performanceMetrics.fcp}ms</li>
          <li>LCP: {performanceMetrics.lcp}ms</li>
          <li>TTI: {performanceMetrics.tti}ms</li>
        </ul>
      </div>
      
      <button onClick={handleLoadData} className="load-data-btn">
        Load Data
      </button>
      
      <div className="data-grid">
        {processedData.map(item => (
          <div key={item.id} className="data-item">
            <h4>{item.label}</h4>
            <p>Value: {item.value.toFixed(2)}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Dashboard;