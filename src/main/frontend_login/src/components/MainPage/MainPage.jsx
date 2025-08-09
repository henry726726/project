// src/components/MainPage/MainPage.jsx (단 한 글자도 생략 없이, 전체를 대체하세요!)

import React, { useState, useEffect } from 'react';
import Header from '../common/Header'; /* Header 컴포넌트 임포트 */
import Footer from '../common/Footer'; /* Footer 컴포넌트 임포트 */
// react-router-dom의 useNavigate 훅을 임포트하여 페이지 이동을 처리합니다.
import { useNavigate } from 'react-router-dom';


/* MainPage에서는 기능 컴포넌트들을 직접 렌더링하지 않으므로, 더 이상 임포트할 필요가 없습니다. */
/*
import TextGenerator from '../../TextGenerator';
import ImageGenerator from '../../ImageGenerator';
import FacebookInput from '../../FacebookInput';
import MetaAdManager from '../../MetaAdManager';
import AdWaitingModal from '../../AdWaitingModal';
*/


function MainPage({ userData, onLogout, isLoggedIn }) { // onShowLogin prop은 더이상 필요하지 않습니다.
  // 페이지 전환을 위한 useNavigate 훅 사용
  const navigate = useNavigate();

  // 현재 MainPage에서는 activeComponent 상태가 더 이상 필요 없습니다.
  // const [activeComponent, setActiveComponent] = useState('text');
  // 광고 대기창 모달은 이제 App.js의 라우트에서 직접 렌더링되므로, MainPage에서 제어할 필요 없습니다.
  // const [isAdModalOpen, setIsAdModalOpen] = useState(false);


  // 메뉴 버튼 클릭 시 호출되는 핸들러 (이제 페이지 이동을 담당합니다)
  const handleMenuClick = (path) => {
    // 로그인하지 않은 상태에서 클릭하면 로그인 페이지로 이동합니다.
    if (!isLoggedIn) {
      navigate('/auth/login');
      return;
    }
    // 로그인 상태이면 해당 경로로 페이지를 이동합니다.
    navigate(path);
  };

  // 미리보기 이미지 클릭 시 호출되는 핸들러 (현재 카드 클릭과 동일한 기능)
  // 미리보기 카드는 삭제되므로, 이 함수도 더 이상 사용되지 않습니다.
  /*
  const handlePreviewClick = () => {
    if (!isLoggedIn) {
      navigate('/auth/login');
    }
  };
  */

  // AdWaitingModal 자동 닫힘 효과: 이제 App.js 라우트에서 직접 렌더링되므로, MainPage에서 제어할 필요 없습니다.
  /*
  useEffect(() => {
    if (isAdModalOpen) {
      const timer = setTimeout(() => {
        setIsAdModalOpen(false);
      }, 3000); // 3초 후 닫기
      return () => clearTimeout(timer);
    }
  }, [isAdModalOpen]);
  */


  /* 메뉴 버튼의 기본 스타일 (MainPage 내부에서 정의합니다.) */
  const menuButtonStyle = {
    flex: 1, /* 5개 버튼이 부모 컨테이너 내에서 공간을 균등하게 차지 */
    padding: '15px 10px', /* 내부 여백 */
    backgroundColor: '#fff', /* 배경색 흰색 */
    color: '#007bff', /* 글자색 파란색 */
    border: '1px solid #ddd', /* 테두리 */
    borderRadius: '8px', /* 모서리 둥글게 */
    fontSize: '1.1em', /* 글자 크기 */
    fontWeight: 'bold', /* 글자 굵게 */
    cursor: 'pointer', /* 마우스 오버 시 포인터 변경 */
    transition: 'background-color 0.2s ease, color 0.2s ease', /* 색상 변화 애니메이션 */
    minWidth: '150px', /* 버튼의 최소 너비 지정 (너무 좁아지는 것 방지) */
  };

  /* 현재 활성화된 메뉴 버튼의 스타일 (MainPage에서는 activeComponent 상태를 사용하지 않으므로, 이 스타일은 더 이상 필요 없습니다.) */
  /*
  const activeMenuButtonStyle = {
    ...menuButtonStyle,
    backgroundColor: '#007bff',
    color: 'white',
    boxShadow: '0 4px 8px rgba(0, 123, 255, 0.2)',
  };
  */


  /* 컴포넌트 렌더링 부분 */
  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Header 컴포넌트: userData, onLogout, isLoggedIn 모두 전달 */}
      {/* Header 컴포넌트가 로그인/회원가입 버튼 클릭 시 useNavigate를 사용해야 하므로 onShowLogin prop은 더 이상 필요 없습니다. */}
      {/* App.js에서 Header로 전달되는 onShowLogin prop도 제거되었습니다. */}
      <Header userData={userData} onLogout={onLogout} isLoggedIn={isLoggedIn} />

      {/* 메인 기능 메뉴바 (홈페이지 길이에서 5개 칸으로 구성) */}
      <nav style={{ display: 'flex', justifyContent: 'space-around', gap: '10px', padding: '15px 20px', backgroundColor: '#e9ecef', borderBottom: '1px solid #dee2e6' }}>
        {/* 각 메뉴 버튼: 클릭 시 handleMenuClick 호출하여 해당 경로로 이동 */}
        {/* style은 active 상태 없이 menuButtonStyle만 사용합니다. */}
        <button
          onClick={() => handleMenuClick('/text-generator')}
          style={menuButtonStyle}
        >
          ✨ 문구 생성
        </button>
        <button
          onClick={() => handleMenuClick('/image-generator')}
          style={menuButtonStyle}
        >
          🎨 이미지 합성
        </button>
        <button
          onClick={() => handleMenuClick('/facebook-input')}
          style={menuButtonStyle}
        >
          📘 페이스북 입력
        </button>
        <button
          onClick={() => handleMenuClick('/ad-waiting')} /* 광고 대기창 라우트 */
          style={menuButtonStyle}
        >
          ⏳ 광고 대기창
        </button>
        <button
          onClick={() => handleMenuClick('/meta-ad-manager')}
          style={menuButtonStyle}
        >
          📈 메타 관리
        </button>
      </nav>

      <nav style={{
  display: 'flex',
  justifyContent: 'space-around',
  gap: '10px',
  padding: '15px 20px',
  backgroundColor: '#f8f9fa',
  borderBottom: '1px solid #dee2e6'
}}>
  <button onClick={() => handleMenuClick('/save-access-token')} style={menuButtonStyle}>
    🔑 액세스토큰 저장
  </button>
  <button onClick={() => handleMenuClick('/save-ad-accounts')} style={menuButtonStyle}>
    📥 광고 계정 저장
  </button>
  <button onClick={() => handleMenuClick('/sync-ad-info')} style={menuButtonStyle}>
    📊 광고 동기화
  </button>
</nav>

      {/* 활성 컴포넌트 렌더링 영역 (MainPage는 더 이상 다른 기능 컴포넌트들을 직접 렌더링하지 않습니다) */}
      <main style={{ flex: 1, padding: 20, backgroundColor: '#f0f2f5', textAlign: 'center', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <h2>원하는 메뉴를 선택해주세요!</h2>
      </main>

      {/* 푸터 컴포넌트 */}
      <Footer />
    </div>
  );
}

export default MainPage;