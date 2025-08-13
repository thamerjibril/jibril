import React from 'react';

export const LoadingSpinner = () => {
  return (
    <div className="loading-spinner" role="status" aria-label="Loading">
      <div className="spinner"></div>
      <span className="sr-only">Loading...</span>
    </div>
  );
};