// src/App.js

import React, { useState, useEffect } from 'react';
import TextGenerator from './TextGenerator';
import ImageGenerator from './ImageGenerator';
import FacebookInput from './FacebookInput';
import AdWaitingModal from './AdWaitingModal';
import MetaAdManager from './MetaAdManager';

function App() {
  const [activeComponent, setActiveComponent] = useState('text');
  const [isAdModalOpen, setIsAdModalOpen] = useState(false);
  const [selectedAdText, setSelectedAdText] = useState(''); // 선택된 광고 문구를 저장할 상태

  // ✅ 추가: TextGenerator의 입력 파라미터를 저장할 상태
  const [textGenParams, setTextGenParams] = useState(null); // { product: '...', target: '...', ... } 형태

  useEffect(() => {
    let timer;
    if (isAdModalOpen) {
      timer = setTimeout(() => {
        setIsAdModalOpen(false);
      }, 3000);
    }
    return () => clearTimeout(timer);
  }, [isAdModalOpen]);

  // TextGenerator에서 문구가 선택되었을 때 호출될 함수
  // ✅ 수정: 선택된 텍스트와 함께, 텍스트 생성에 사용된 전체 파라미터도 받도록 수정
  const handleAdTextSelect = (text, params) => {
    setSelectedAdText(text); // 선택된 문구 저장
    setTextGenParams(params); // ✅ 파라미터 저장
    setActiveComponent('image'); // 이미지 생성 컴포넌트로 전환
  };

  return (
    <div
      className="App"
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        padding: '40px 20px',
        boxSizing: 'border-box',
        fontFamily: 'Arial, sans-serif',

        backgroundImage: 'url("/homepage_design.png")', // public 폴더에 이미지 넣고 경로 맞춰주기
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
        backgroundAttachment: 'fixed',

        backgroundColor: 'rgba(0, 0, 0, 0.6)', // 어두운 오버레이 효과
        backgroundBlendMode: 'overlay',
      }}
    >
      {/* 버튼 영역 */}
      <div
        style={{
          marginBottom: '30px',
          textAlign: 'center',
          padding: '20px',
          borderRadius: '15px',
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          boxShadow: '0 4px 15px rgba(0,0,0,0.1)',
          display: 'flex',
          flexWrap: 'wrap',
          justifyContent: 'center',
          gap: '10px',
        }}
      >
        <button
          onClick={() => setActiveComponent('text')}
          style={getButtonStyle(activeComponent === 'text', '#28a745')}
        >
          광고 문구 생성기
        </button>
        <button
          onClick={() => setActiveComponent('image')}
          style={getButtonStyle(activeComponent === 'image', '#007bff')}
        >
          이미지 합성기
        </button>
        <button
          onClick={() => setActiveComponent('facebook')}
          style={getButtonStyle(activeComponent === 'facebook', '#1877F2')}
        >
          페이스북 입력창
        </button>
        <button
          onClick={() => setIsAdModalOpen(true)}
          style={getButtonStyle(false, '#ffc107')}
        >
          광고 대기창
        </button>
        <button
          onClick={() => setActiveComponent('metaAds')}
          style={getButtonStyle(activeComponent === 'metaAds', '#3b5998')}
        >
          메타 광고 관리
        </button>
      </div>

      {/* 컴포넌트 렌더링 */}
      {/* ✅ 수정: TextGenerator에 onTextSelect prop 전달 */}
      {activeComponent === 'text' && <TextGenerator onTextSelect={handleAdTextSelect} />}
      {/* ✅ 수정: ImageGenerator에 textGenParams prop 전달 */}
      {activeComponent === 'image' && <ImageGenerator selectedText={selectedAdText} textGenParams={textGenParams} />}
      {activeComponent === 'facebook' && <FacebookInput />}
      {activeComponent === 'metaAds' && <MetaAdManager />}

      <AdWaitingModal isOpen={isAdModalOpen} onClose={() => setIsAdModalOpen(false)} />
    </div>
  );
}

const getButtonStyle = (isActive, activeColor) => ({
  padding: '10px 20px',
  backgroundColor: isActive ? activeColor : '#e9ecef',
  color: isActive ? 'white' : '#495057',
  border: 'none',
  borderRadius: '8px',
  fontSize: '15px', // 폰트 크기 조정
  fontWeight: 'bold',
  cursor: 'pointer',
  boxShadow: '0 2px 5px rgba(0,0,0,0.1)',
  whiteSpace: 'nowrap',
  transition: 'background-color 0.3s ease, color 0.3s ease, transform 0.1s ease',
  height: '50px' // 버튼 높이 조정
});


export default App;