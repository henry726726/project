// src/ImageGenerator.jsx

import React, { useState } from 'react';
import axios from 'axios';

// selectedText prop을 받도록 수정
function ImageGenerator({ selectedText }) {
  const [imageFile, setImageFile] = useState(null);
  const [resultUrl, setResultUrl] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [mode, setMode] = useState('controlnet'); // 합성 방식 선택용 (필요하면)

  const handleFileChange = (e) => {
    setImageFile(e.target.files[0]);
  };

  const getTimestampString = () => {
    const now = new Date();
    const pad = (n) => n.toString().padStart(2, '0');
    return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
  };

  const handleCompose = async () => {
    if (!imageFile) {
      alert('이미지 파일을 선택해주세요!');
      return;
    }
    // selectedText prop이 없으면 합성 진행 불가
    if (!selectedText) {
      alert('합성할 문구가 선택되지 않았습니다. 문구 생성기에서 문구를 선택해주세요!');
      return;
    }

    setIsLoading(true);
    setResultUrl(null);

    try {
      const form = new FormData();
      form.append('image', imageFile);
      // description 대신 selectedText prop을 prompt로 사용
      form.append('prompt', selectedText);
      form.append('mode', mode); // 필요하면 서버에서 처리

      // 백엔드 서버가 8000 포트에서 실행 중이어야 합니다.
      const url = 'http://localhost:8000/generate';

      const res = await axios.post(url, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      const base64 = res.data?.image_base64;
      if (base64) {
        setResultUrl(`data:image/jpeg;base64,${base64}`);
      } else {
        alert('서버에서 이미지 생성 결과를 받지 못했습니다.');
      }
    } catch (error) {
      console.error('이미지 합성 오류:', error);
      alert('이미지 합성 중 오류가 발생했습니다. 백엔드 서버를 확인해주세요.');
      
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div style={{
      maxWidth: '600px',
      margin: '40px auto',
      padding: '20px',
      border: '1px solid #ddd',
      borderRadius: '10px',
      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
      backgroundColor: 'rgba(255, 255, 255, 0.9)', // 배경 이미지와 어울리도록 반투명하게
      fontFamily: 'Arial, sans-serif',
      textAlign: 'center'
    }}>
      <h2 style={{ marginBottom: '20px', color: '#333' }}>🖼️ 광고 이미지 합성기</h2>

      {/* 선택된 문구를 보여주는 부분 */}
      <div style={{ marginBottom: '15px', padding: '10px', border: '1px dashed #007bff', borderRadius: '5px', backgroundColor: '#e6f7ff' }}>
        <strong>선택된 문구:</strong> {selectedText || '문구 생성기에서 문구를 선택해주세요.'}
      </div>

      <input
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        style={{ marginBottom: '15px' }}
      />

      <button
        onClick={handleCompose}
        disabled={isLoading || !selectedText} // 문구가 선택되지 않으면 버튼 비활성화
        style={{
          width: '100%',
          padding: '12px',
          backgroundColor: (isLoading || !selectedText) ? '#999' : '#007bff',
          color: 'white',
          border: 'none',
          borderRadius: '6px',
          fontSize: '16px',
          cursor: (isLoading || !selectedText) ? 'not-allowed' : 'pointer',
          marginBottom: '20px'
        }}
      >
        {isLoading ? '합성 중...' : '이미지 합성하기'}
      </button>

      {resultUrl && (
        <div>
          <img src={resultUrl} alt="합성 결과" style={{ maxWidth: '100%', borderRadius: '8px' }} />
          <a
            href={resultUrl}
            download={`composite_${getTimestampString()}.png`}
            style={{ display: 'block', marginTop: '10px', color: '#007bff', textDecoration: 'underline', cursor: 'pointer' }}
          >
            이미지 다운로드
          </a>
        </div>
      )}
    </div>
  );
}

export default ImageGenerator;