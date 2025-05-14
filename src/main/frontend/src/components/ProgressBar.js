import React from 'react';


function ProgressBar({ progress }) {
    return (
      <div style={{ width: '100%', height: '8px', backgroundColor: '#ccc' }}>
        <div
          style={{
            width: `${progress}%`,
            height: '100%',
            backgroundColor: '#4285f4',
            borderRadius: '4px',
          }}
        />
      </div>
    );
  }
  
  export default ProgressBar;
  