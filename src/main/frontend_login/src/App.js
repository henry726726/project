import React, { useState, useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import LoginSignup from './components/LoginSignup/LoginSignup'; // LoginSignup 컴포넌트 임포트
import MyPage from './components/MyPage/MyPage';
import ErrorBoundary from './components/ErrorBoundary/ErrorBoundary';
import MainPage from './components/MainPage/MainPage'; // MainPage 임포트
import { jwtDecode } from 'jwt-decode';

function App() { // AppFullTest라면 function AppFullTest() 로 변경
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userData, setUserData] = useState(null);
  const [showLoginModal, setShowLoginModal] = useState(false); // 로그인/회원가입 모달 표시 상태

  useEffect(() => {
    // 자동 로그인 로직: 앱 로드 시 localStorage에서 JWT 토큰 확인
    const token = localStorage.getItem('jwtToken');
    if (token) {
      try {
        const decoded = jwtDecode(token);
        // 토큰 유효성 (만료 시간) 확인
        if (decoded.exp * 1000 > Date.now()) {
          setUserData({ email: decoded.sub, roles: decoded.auth });
          setIsLoggedIn(true);
        } else {
          // 토큰 만료 시 삭제
          console.log("JWT 토큰이 만료되었습니다.");
          localStorage.removeItem('jwtToken');
        }
      } catch (e) {
        // 토큰 디코딩 오류 또는 유효하지 않은 토큰일 경우 삭제
        console.error("JWT 토큰 디코딩 오류 또는 유효하지 않은 토큰:", e);
        localStorage.removeItem('jwtToken');
      }
    }
  }, []);

  // 로그인 처리 핸들러 (LoginSignup 컴포넌트에서 호출)
  const handleLogin = (userInfo) => {
    setUserData(userInfo);
    setIsLoggedIn(true);
    setShowLoginModal(false); // 로그인 성공 시 모달 닫기
  };

  // 로그아웃 처리 핸들러
  const handleLogout = () => {
    setIsLoggedIn(false);
    setUserData(null);
    localStorage.removeItem('jwtToken');
  };

  // 로그인 모달 표시 핸들러 (MainPage 컴포넌트에서 호출)
  const handleShowLogin = () => {
    setShowLoginModal(true);
  };

  // 로그인 모달 닫기 핸들러 (LoginSignup 컴포넌트에서 호출)
  const handleCloseLogin = () => {
    setShowLoginModal(false);
  };

  return (
    <>
      <Routes>
        {/*
          💡💡💡 로그인/회원가입 페이지 라우트 제거! 💡💡💡
          이제 LoginSignup은 라우트를 통해 접근하는 대신,
          showLoginModal 상태에 따라 현재 페이지 위에 모달처럼 렌더링됩니다.
          기존의 /auth/login, /auth/signup 경로에 접근하면
          아래의 <Route path="*" element={<Navigate to="/" replace />} /> 에 의해
          메인 페이지로 리다이렉트됩니다.
        */}

        {/* 메인 페이지: 로그인 여부와 관계없이 항상 접근 가능 (메인 UI) */}
        <Route
          path="/"
          element={
            <MainPage
              userData={userData}
              onLogout={handleLogout}
              isLoggedIn={isLoggedIn}
              onShowLogin={handleShowLogin} // MainPage로 로그인 모달 표시 함수 전달
            />
          }
        />

        {/* 마이페이지: 로그인 상태일 때만 접근 가능. 로그인 안 되어 있으면 / 로 리다이렉트 */}
        {/* /auth/login으로 직접 이동하지 않고, 모달이 뜨는 구조이므로 메인으로 리다이렉트하는 것이 자연스럽습니다. */}
        <Route
          path="/mypage"
          element={
            isLoggedIn ? (
              <MyPage userData={userData} onLogout={handleLogout} />
            ) : (
              // 💡💡💡 로그인 안 되어 있으면 메인 페이지로 리다이렉트
              <Navigate to="/" replace />
            )
          }
        />

        {/* 정의되지 않은 모든 경로는 메인 페이지로 리다이렉트 */}
        {/* 💡💡💡 이제 /auth/login, /auth/signup도 이 규칙에 따라 메인 페이지로 리다이렉트됩니다. */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>

      {/* 💡💡💡 로그인/회원가입 모달 렌더링! (기존과 동일) 💡💡💡 */}
      {/* showLoginModal 상태가 true일 때만 LoginSignup 컴포넌트를 모달처럼 렌더링합니다. */}
      {/* onLogin은 로그인 성공 콜백, onClose는 모달 닫기 콜백으로 사용됩니다. */}
      {showLoginModal && (
        <ErrorBoundary> {/* 모달도 에러 방지를 위해 ErrorBoundary로 감쌈 */}
          <LoginSignup onLogin={handleLogin} onClose={handleCloseLogin} />
        </ErrorBoundary>
      )}
    </>
  );
}

export default App;