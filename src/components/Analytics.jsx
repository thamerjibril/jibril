import React from 'react';

const Analytics = () => {
  const [chartData, setChartData] = React.useState(null);
  
  React.useEffect(() => {
    // Simulate loading heavy analytics library
    const loadChartLibrary = async () => {
      // In real app, this would load a chart library like Chart.js
      await new Promise(resolve => setTimeout(resolve, 500));
      
      setChartData({
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May'],
        values: [65, 59, 80, 81, 56]
      });
    };
    
    loadChartLibrary();
  }, []);
  
  return (
    <div className="analytics" id="analytics">
      <h2>Analytics</h2>
      {chartData ? (
        <div className="chart-container">
          <h3>Performance Over Time</h3>
          <div className="simple-chart">
            {chartData.labels.map((label, i) => (
              <div key={label} className="chart-bar">
                <div 
                  className="bar" 
                  style={{ height: `${chartData.values[i]}%` }}
                />
                <span className="label">{label}</span>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <p>Loading analytics data...</p>
      )}
    </div>
  );
};

export default Analytics;