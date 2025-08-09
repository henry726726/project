// src/App.js (단 한 글자도 생략 없이, 전체를 대체하세요!)

import React, { useState, useEffect } from 'react';
// BrowserRouter as Router 임포트는 유지하고, useNavigate 훅도 임포트합니다.
// 이제 모든 기능이 라우트될 것이므로!
import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate } from 'react-router-dom';

// 프로젝트 내 다른 컴포넌트들을 임포트합니다.
// 이 컴포넌트들이 이제 별도의 라우트에서 렌더링됩니다.
import LoginSignup from './components/LoginSignup/LoginSignup';
import MyPage from './components/MyPage/MyPage';
import ErrorBoundary from './components/ErrorBoundary/ErrorBoundary'; // 에러 바운더리 컴포넌트
import MainPage from './components/MainPage/MainPage'; // 메인 페이지 컴포넌트

// 기능 컴포넌트들을 직접 App.js에서 임포트합니다.
import TextGenerator from './TextGenerator';
import ImageGenerator from './ImageGenerator';
import FacebookInput from './FacebookInput';
import MetaAdManager from './MetaAdManager';
import AdWaitingModal from './AdWaitingModal'; // AdWaitingModal은 이제 별도 라우트에서 렌더링 (단, isOpen true로)
import SaveAdAccounts from './Pages/SaveAdAccounts';
import SyncAdInfo from './Pages/SyncAdInfo';
import AccessTokenInput from './Pages/AccessTokenInput';

// jwt-decode 라이브러리를 임포트합니다.
import { jwtDecode } from 'jwt-decode';

// ==========================================================
// App 컴포넌트 정의 시작
// 이 컴포넌트가 앱의 최상위 루트 컴포넌트입니다.
// ==========================================================
function App() {
  // 사용자 로그인 상태를 관리하는 State
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  // 로그인한 사용자의 정보를 저장하는 State
  const [userData, setUserData] = useState(null);

  // useEffect Hook: 앱 로드 시 localStorage에서 JWT 토큰 확인하여 자동 로그인 처리
  useEffect(() => {
    const token = localStorage.getItem('jwtToken');
    if (token) {
      try {
        const decoded = jwtDecode(token);
        if (decoded.exp * 1000 > Date.now()) {
          setUserData({ email: decoded.sub, roles: decoded.auth });
          setIsLoggedIn(true);
        } else {
          console.log("JWT 토큰이 만료되었습니다. 다시 로그인해주세요.");
          localStorage.removeItem('jwtToken');
        }
      } catch (e) {
        console.error("JWT 토큰 디코딩 오류 또는 유효하지 않은 토큰:", e);
        localStorage.removeItem('jwtToken');
      }
    }
  }, []); // 빈 배열 []: 컴포넌트가 처음 마운트될 때 한 번만 실행됩니다.

  // handleLogin 함수: 로그인 성공 시 호출되며, 사용자 정보를 설정하고 로그인 상태를 true로 변경합니다.
  const handleLogin = (userInfo) => {
    setUserData(userInfo);
    setIsLoggedIn(true);
    // 로그인 성공 후 메인 페이지로 자동으로 이동시키고 싶다면 여기에 navigate('/') 추가 (단, navigate 훅 필요)
    // 현재는 로그인 버튼 클릭하는 곳에서 navigate 처리
  };

  // handleLogout 함수: 로그아웃 버튼 클릭 시 호출되며, 로그인 상태를 초기화하고 JWT 토큰을 삭제합니다.
  const handleLogout = () => {
    setIsLoggedIn(false);
    setUserData(null);
    localStorage.removeItem('jwtToken');
  };

  // App 컴포넌트의 렌더링 부분
  // App 컴포넌트 자체는 index.js에서 BrowserRouter로 감싸져 있으므로, 여기서는 <Router>를 사용하지 않습니다.
  return (
    // <Router>를 다시 추가합니다. (이전 오류 해결 과정에서 잠시 삭제했으나, 모든 라우팅 기능을 App에서 직접 관리하기 위해 복구)
    // 주의: index.js에서도 BrowserRouter를 사용하고 있으므로, 만약 여기서 Router를 사용한다면 index.js의 BrowserRouter를 제거해야 합니다.
    // **가장 간단한 해결책은 index.js의 BrowserRouter를 그대로 두고, App.js의 <Router> 임포트 줄에서 as Router를 제거하는 것입니다.**
    // **(단, 이렇게 하면 App.js 내에서 navigate 훅을 사용해야 합니다.)**

    // === 최종 결정: index.js에서 BrowserRouter로 감싸고, App.js는 Router 임포트 없이 <Routes>부터 시작 ===
    // 이렇게 하면 중복 Router 오류는 발생하지 않습니다.
    // navigate 훅은 필요한 컴포넌트에서 직접 사용합니다.

    <>
      <Routes>
        {/*
          로그인/회원가입 페이지: '/auth/login', '/auth/signup' 경로로 접근 시 해당 컴포넌트 렌더링
          ErrorBoundary로 감싸서 에러 방지
        */}
        <Route
          path="/auth/login"
          element={
            <ErrorBoundary>
              <LoginSignup onLogin={handleLogin} />
            </ErrorBoundary>
          }
        />
        <Route
          path="/auth/signup"
          element={
            <ErrorBoundary>
              <LoginSignup onLogin={handleLogin} />
            </ErrorBoundary>
          }
        />

        {/*
          메인 페이지: '/' 경로로 접근 시 MainPage 컴포넌트 렌더링
          이 페이지는 메뉴바와 일반 소개 등만 포함하며,
          기능 컴포넌트들은 다른 경로에서 렌더링됩니다.
        */}
        <Route
          path="/"
          element={
            <MainPage
              userData={userData}
              onLogout={handleLogout}
              isLoggedIn={isLoggedIn}
            />
          }
        />

        {/* ==================================================================== */}
        {/* 💡💡💡 각 기능 컴포넌트들을 별도의 라우트로 정의합니다. 💡💡💡 */}
        {/* 로그인하지 않은 사용자가 이 경로로 직접 접근 시 로그인 페이지로 리다이렉트 */}
        {/* ==================================================================== */}
        <Route
          path="/text-generator"
          element={
            isLoggedIn ? (
              <TextGenerator />
            ) : (
              <Navigate to="/auth/login" replace />
            )
          }
        />
        <Route
          path="/image-generator"
          element={
            isLoggedIn ? (
              <ImageGenerator />
            ) : (
              <Navigate to="/auth/login" replace />
            )
          }
        />
        <Route
          path="/facebook-input"
          element={
            isLoggedIn ? (
              <FacebookInput />
            ) : (
              <Navigate to="/auth/login" replace />
            )
          }
        />
        <Route
          path="/meta-ad-manager"
          element={
            isLoggedIn ? (
              <MetaAdManager />
            ) : (
              <Navigate to="/auth/login" replace />
            )
          }
        />
        {/* AdWaitingModal은 특성상 모달이므로, 실제 페이지로서는 좀 어색할 수 있지만 요청대로 구현합니다. */}
        {/* isOpen을 true로 고정하여 해당 라우트에 오면 항상 보이게 합니다. onClose는 필요하면 라우팅으로 처리. */}
        <Route
          path="/ad-waiting"
          element={
            isLoggedIn ? (
              <AdWaitingModal isOpen={true} onClose={() => { /* 이 페이지에서는 직접 닫히기보다 다른 곳으로 Navigate를 권장 */ }} />
            ) : (
              <Navigate to="/auth/login" replace />
            )
          }
        />

        {/* 마이페이지: 로그인 상태일 때만 접근 가능. 로그인 안 되어 있으면 로그인 페이지로 리다이렉트 */}
        <Route
          path="/mypage"
          element={
            isLoggedIn ? (
              <MyPage userData={userData} onLogout={handleLogout} />
            ) : (
              <Navigate to="/auth/login" replace />
            )
          }
        />

        <Route
          path="/save-access-token"
          element={isLoggedIn ? <AccessTokenInput /> : <Navigate to="/auth/login" replace />}
        />
        <Route
          path="/save-ad-accounts"
          element={isLoggedIn ? <SaveAdAccounts /> : <Navigate to="/auth/login" replace />}
        />
        <Route
          path="/sync-ad-info"
          element={isLoggedIn ? <SyncAdInfo /> : <Navigate to="/auth/login" replace />}
        />

        {/* 정의되지 않은 모든 경로는 메인 페이지로 리다이렉트 */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}

// App 컴포넌트를 이 파일의 기본 내보내기로 설정합니다.
export default App;