import React from 'react';

function Footer() {
  return (
    <footer style={{
      padding: '20px',
      background: 'linear-gradient(135deg, #0e103d 0%, #6243a5 100%)', // 네이비-퍼플 그라디언트 배경
      borderTop: '1px solid rgba(98, 67, 165, 0.3)',              // 부드러운 퍼플 계열 테두리
      textAlign: 'center',
      marginTop: 'auto', // 하단 고정
      fontFamily: 'Arial, sans-serif',
      fontSize: '0.9em',
      color: '#e0e0ff'    // 밝고 부드러운 글자색
    }}>
      <p>&copy; 2025 광고 매니저. All rights reserved.</p>
      <p>연락처: support@admanager.com</p>
    </footer>
  );
}

export default Footer;