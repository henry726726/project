// src/MetaAdManager.jsx

import React from 'react';

function MetaAdManager() {
  const handleGoToMetaAds = () => {
    // 실제 메타 광고 관리자 URL로 변경해야 함
    // 예시: https://business.facebook.com/adsmanager/
    const metaAdsUrl = 'https://business.facebook.com/adsmanager/';
    window.open(metaAdsUrl, '_blank'); // 새 탭으로 열기
  };

  return (
    <div style={{
      maxWidth: '600px',
      margin: '40px auto',
      padding: '20px',
      border: '1px solid #ddd',
      borderRadius: '8px',
      boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
      backgroundColor: '#fff',
      textAlign: 'center'
    }}>
      <h2 style={{ color: '#333', marginBottom: '25px' }}>📈 메타 광고 관리</h2>

      <p style={{ fontSize: '1.1em', color: '#555', lineHeight: '1.6' }}>
        여기에서 메타(페이스북/인스타그램) 광고 캠페인을 관리하고 성과를 확인하실 수 있습니다.
        아래 버튼을 클릭하여 메타 광고 관리자 페이지로 이동하세요.
      </p>

      <button
        onClick={handleGoToMetaAds}
        style={{
          marginTop: '30px',
          padding: '15px 30px',
          backgroundColor: '#3b5998', // 페이스북 브랜드 색상
          color: 'white',
          border: 'none',
          borderRadius: '8px',
          fontSize: '1.2em',
          fontWeight: 'bold',
          cursor: 'pointer',
          transition: 'background-color 0.3s ease'
        }}
      >
        메타 광고 관리자 페이지로 이동 ➡️
      </button>

      <p style={{ fontSize: '0.8em', color: '#888', marginTop: '20px' }}>
        (새 창으로 열립니다.)
      </p>
    </div>
  );
}

export default MetaAdManager;