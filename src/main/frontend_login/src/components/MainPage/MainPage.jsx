import React from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../common/Header';
import Footer from '../common/Footer';

function MainPage({ userData, onLogout, isLoggedIn }) {
  const navigate = useNavigate();

  const handleMenuClick = (path) => {
    if (!isLoggedIn) {
      navigate('/auth/login');
      return;
    }
    navigate(path);
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Header userData={userData} onLogout={onLogout} isLoggedIn={isLoggedIn} />

      <nav style={{ display: 'flex', justifyContent: 'space-around', padding: '15px 20px', backgroundColor: '#e9ecef', borderBottom: '1px solid #dee2e6' }}>
        <button onClick={() => handleMenuClick('/text-generator')}>✨ 문구 생성</button>
        <button onClick={() => handleMenuClick('/image-generator')}>🎨 이미지 합성</button>
        <button onClick={() => handleMenuClick('/facebook-input')}>📘 페이스북 입력</button>
        <button onClick={() => handleMenuClick('/ad-waiting')}>⏳ 광고 대기창</button>
        <button onClick={() => handleMenuClick('/meta-ad-manager')}>📈 메타 관리</button>
      </nav>

      <Footer />
    </div>
  );
}

export default MainPage;