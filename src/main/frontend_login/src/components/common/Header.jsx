// src/components/common/Header.jsx

import React from 'react';
import { Link, useNavigate } from 'react-router-dom';

function Header({ userData, onLogout, isLoggedIn, onShowLogin }) { // onShowLogin prop 추가
  const navigate = useNavigate();

  const handleLogoutClick = () => {
    onLogout(); // App.js의 handleLogout 호출
    navigate('/auth/login'); // 로그인 페이지로 리다이렉트
  };

  return (
    <header style={{
      display: 'flex',
      justifyContent: 'space-between', // 요소를 양 끝으로 배치
      alignItems: 'center',
      padding: '15px 30px',
      backgroundColor: '#f8f9fa',
      borderBottom: '1px solid #e9ecef',
      boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
      fontFamily: 'Arial, sans-serif'
    }}>
      {/* 왼쪽 여백을 위한 빈 div. flex-grow로 남은 공간 차지하여 중앙 요소 균형 맞춤 */}
      <div style={{ flex: 1 }}></div>

      {/* 홈페이지 제목: flex:1로 남은 공간 차지하며 가운데 정렬 */}
      <div style={{ flex: 1, textAlign: 'center', fontSize: '1.8em', fontWeight: 'bold', color: '#007bff' }}>
        <Link to="/" style={{ textDecoration: 'none', color: 'inherit' }}>
          ✨ 광고 매니저 ✨
        </Link>
      </div>

      {/* 오른쪽 로그인/로그아웃 버튼 */}
      <nav style={{ flex: 1, textAlign: 'right' }}> {/* flex:1로 남은 공간 차지 */}
        {isLoggedIn ? (
          <>
            <span style={{ fontSize: '1.0em', color: '#555', alignSelf: 'center', marginRight: '15px' }}>
              환영합니다, <strong>{userData?.email?.split('@')[0]}</strong>님!
            </span>
            <button onClick={handleLogoutClick} style={{
              background: '#dc3545', border: 'none', color: 'white', cursor: 'pointer',
              fontSize: '1.0em', fontWeight: 'bold', padding: '8px 15px', borderRadius: '5px'
            }}>
              로그아웃
            </button>
          </>
        ) : (
          <button
            onClick={onShowLogin} // 로그인/회원가입 모달 열기 (App.js에서 받아옴)
            style={{
              background: '#007bff', border: 'none', color: 'white', cursor: 'pointer',
              fontSize: '1.0em', fontWeight: 'bold', padding: '8px 15px', borderRadius: '5px'
            }}>
            로그인 / 회원가입
          </button>
        )}
      </nav>
    </header>
  );
}

export default Header;