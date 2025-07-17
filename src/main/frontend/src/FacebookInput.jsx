// src/FacebookInput.jsx

import React, { useState } from 'react';

function FacebookInput() {
  const [postContent, setPostContent] = useState('');

  // 입력 필드의 값이 변경될 때마다 postContent 상태를 업데이트하는 함수
  const handleContentChange = (event) => {
    setPostContent(event.target.value);
  };

  // '게시' 버튼을 클릭했을 때 실행될 함수
  const handlePost = () => {
    if (!postContent.trim()) {
      alert('게시할 내용을 입력해주세요! 😅');
      return;
    }

    // 실제로는 여기에 서버로 데이터를 전송하는 로직이 들어갈 거야.
    // 지금은 간단히 콘솔에 내용을 출력하고 입력창을 비워줄게.
    console.log('게시 내용:', postContent);
    alert('게시글이 작성되었습니다! 🎉');
    setPostContent(''); // 게시 후 입력창 비우기
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
      fontFamily: 'Arial, sans-serif'
    }}>
      <h2 style={{ color: '#333', textAlign: 'center', marginBottom: '25px' }}>💬 페이스북 입력창</h2>

      {/* 상단 프로필 이미지 및 입력 시작 부분 */}
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '15px' }}>
        <div style={{
          width: '40px',
          height: '40px',
          borderRadius: '50%',
          backgroundColor: '#e0e0e0', // 프로필 이미지 대체 색상
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          color: '#777',
          fontWeight: 'bold',
          fontSize: '18px',
          marginRight: '10px'
        }}>
          👤
        </div>
        <textarea
          value={postContent}
          onChange={handleContentChange}
          placeholder="무슨 생각을 하고 계신가요, 블랙맘바?"
          rows="4"
          style={{
            flexGrow: 1, // 남은 공간을 모두 차지하도록
            padding: '10px',
            border: 'none',
            borderRadius: '8px',
            fontSize: '16px',
            backgroundColor: '#f0f2f5', // 페이스북 입력창 배경색
            resize: 'none', // 크기 조절 불가
            outline: 'none' // 포커스 시 테두리 제거
          }}
        />
      </div>

      {/* 구분선 */}
      <hr style={{ border: '0', borderTop: '1px solid #e0e0e0', margin: '20px 0' }} />

      {/* 하단 버튼 영역 */}
      <div style={{ display: 'flex', justifyContent: 'space-around', marginBottom: '20px' }}>
        <button style={{
          flex: 1,
          padding: '10px',
          backgroundColor: 'transparent',
          border: 'none',
          color: '#65676B',
          fontWeight: 'bold',
          fontSize: '15px',
          borderRadius: '6px',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '5px',
          transition: 'background-color 0.2s ease'
        }} onMouseOver={e => e.currentTarget.style.backgroundColor = '#f0f2f5'} onMouseOut={e => e.currentTarget.style.backgroundColor = 'transparent'}>
          📸 사진/동영상
        </button>
        <button style={{
          flex: 1,
          padding: '10px',
          backgroundColor: 'transparent',
          border: 'none',
          color: '#65676B',
          fontWeight: 'bold',
          fontSize: '15px',
          borderRadius: '6px',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '5px',
          transition: 'background-color 0.2s ease'
        }} onMouseOver={e => e.currentTarget.style.backgroundColor = '#f0f2f5'} onMouseOut={e => e.currentTarget.style.backgroundColor = 'transparent'}>
          😊 기분/활동
        </button>
      </div>

      {/* 게시 버튼 */}
      <button
        onClick={handlePost}
        disabled={!postContent.trim()} // 내용이 없으면 버튼 비활성화
        style={{
          width: '100%',
          padding: '12px 20px',
          backgroundColor: postContent.trim() ? '#1877F2' : '#E4E6EB', // 내용 있으면 파란색, 없으면 회색
          color: postContent.trim() ? 'white' : '#BCBFC4',
          border: 'none',
          borderRadius: '6px',
          fontSize: '18px',
          fontWeight: 'bold',
          cursor: postContent.trim() ? 'pointer' : 'not-allowed',
          transition: 'background-color 0.2s ease'
        }}
      >
        게시
      </button>
    </div>
  );
}

export default FacebookInput;