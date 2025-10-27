// src/Header.jsx

import React from 'react';
import { Link, useNavigate } from 'react-router-dom';

const Header = () => {
  const navigate = useNavigate();
  const isLoggedIn = localStorage.getItem('jwtToken');
  const userEmail = localStorage.getItem('userEmail');

  const handleLogout = () => {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('userEmail');
    alert('로그아웃 되었습니다.');
    navigate('/auth/login');
  };

  return (
    <header style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '15px 30px',
      background: 'linear-gradient(90deg, #0e103d 0%, #6243a5 100%)', // ✅ 네이비-퍼플 그라디언트 배경 적용
      color: '#e0e0ff',        // ✅ 밝은 텍스트 색상 적용
      boxShadow: '0 4px 15px rgba(0,0,0,0.3)', // ✅ 테마에 맞는 그림자 효과 조정
      fontFamily: 'Arial, sans-serif',
      position: 'sticky',
      top: 0,
      zIndex: 100
    }}>
      <div style={{
        fontSize: '1.8em',
        fontWeight: 'bold',
        color: '#A8E6CF', // ✅ 로고를 포인트 컬러(밝은 그린)로 강조
        textShadow: '1px 1px 2px rgba(0,0,0,0.5)' // ✅ 텍스트 그림자 추가
      }}>
        <Link to="/" style={{ textDecoration: 'none', color: 'inherit' }}>
          Ad Manager
        </Link>
      </div>

      <nav>
        {isLoggedIn ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
            {userEmail && (
              <span style={{ fontSize: '1em', color: '#d1c4e9' }}> {/* ✅ 사용자 이메일 텍스트 색상 조정 */}
                {userEmail} 님
              </span>
            )}
            <Link to="/mypage" style={{ textDecoration: 'none' }}>
              <button style={{
                padding: '10px 20px',
                border: 'none',
                borderRadius: '20px',
                background: 'linear-gradient(45deg, #4CAF50, #8BC34A)', // ✅ 그린 계열 그라디언트 유지
                color: 'white',
                fontSize: '1em',
                fontWeight: 'bold',
                cursor: 'pointer',
                transition: 'all 0.3s ease',
                boxShadow: '0 2px 10px rgba(76, 175, 80, 0.4)' // ✅ 그림자 조정
              }}>
                마이페이지
              </button>
            </Link>
            <button onClick={handleLogout} style={{
              padding: '10px 20px',
              border: 'none',
              borderRadius: '20px',
              background: 'linear-gradient(45deg, #f44336, #FF6F00)', // ✅ 레드-오렌지 계열 그라디언트 유지
              color: 'white',
              fontSize: '1em',
              fontWeight: 'bold',
              cursor: 'pointer',
              transition: 'all 0.3s ease',
              boxShadow: '0 2px 10px rgba(244, 67, 54, 0.4)' // ✅ 그림자 조정
            }}>
              로그아웃
            </button>
          </div>
        ) : (
          <> {/* JSX 규칙 준수를 위한 React Fragment는 그대로 유지 */}
            <Link to="/auth/login" style={{ textDecoration: 'none' }}>
              <button style={{
                padding: '10px 20px',
                border: 'none',
                borderRadius: '20px',
                background: 'linear-gradient(45deg, #007bff, #00BFFF)', // ✅ 블루 계열 그라디언트 유지
                color: 'white',
                fontSize: '1em',
                fontWeight: 'bold',
                cursor: 'pointer',
                transition: 'all 0.3s ease',
                boxShadow: '0 2px 10px rgba(0, 123, 255, 0.4)' // ✅ 그림자 조정
              }}>
                로그인 / 회원가입
              </button>
            </Link>
          </>
        )}
      </nav>
    </header>
  );
};

export default Header;