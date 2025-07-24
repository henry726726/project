// src/ImageGenerator.jsx

import React, { useState } from 'react';
import axios from 'axios';

// selectedText prop 외에 textGenParams prop을 받도록 수정
function ImageGenerator({ selectedText, textGenParams }) {
  const [imageFile, setImageFile] = useState(null);
  const [resultUrl, setResultUrl] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSavingContent, setIsSavingContent] = useState(false); // ✅ 추가: DB 저장 중 상태
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
      alert('이미지 파일을 선택해주세요! 😅');
      return;
    }
    if (!selectedText) {
      alert('합성할 문구가 선택되지 않았습니다. 문구 생성기에서 문구를 선택해주세요! 😲');
      return;
    }

    setIsLoading(true);
    setResultUrl(null); // 새로운 이미지 생성을 위해 이전 결과 초기화
    setIsSavingContent(false); // 새로운 이미지 생성이므로 저장 상태 초기화

    try {
      const form = new FormData();
      form.append('image', imageFile);
      form.append('prompt', selectedText);
      form.append('mode', mode);

      const url = 'http://localhost:8000/generate'; // 파이썬 이미지 서버 URL

      const res = await axios.post(url, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      const base64 = res.data?.image_base64;
      if (base64) {
        setResultUrl(`data:image/png;base64,${base64}`); // 이미지 형식 PNG로 고정
        alert('이미지 합성이 완료되었습니다! ✨');
      } else {
        alert('서버에서 이미지 생성 결과를 받지 못했습니다. 😥');
      }
    } catch (error) {
      console.error('이미지 합성 오류:', error);
      alert('이미지 합성 중 오류가 발생했습니다. 백엔드 서버를 확인해주세요. 😭');
    } finally {
      setIsLoading(false);
    }
  };

  // ✅ 추가: 합성된 이미지와 정보를 백엔드에 저장하는 함수
  const handleSaveContent = async () => {
    if (!resultUrl) {
      alert('저장할 합성된 이미지가 없습니다. 이미지를 먼저 생성해주세요! 🙅‍♀️');
      return;
    }

    setIsSavingContent(true);

    try {
      // 이미지 Base64 데이터에서 'data:image/png;base64,' 접두사 제거
      const cleanedBase64Image = resultUrl.split(',')[1];

      const savePayload = {
        // 문구 생성에 사용된 파라미터 (App.js에서 받아옴)
        product: textGenParams?.product || '',
        target: textGenParams?.target || '',
        purpose: textGenParams?.purpose || '',
        keyword: textGenParams?.keyword || '',
        duration: textGenParams?.duration || '',
        // 선택된 광고 문구
        adText: selectedText,
        // 합성된 이미지 Base64 (접두사 제거)
        generatedImageBase64: cleanedBase64Image,
      };

      // ✅ 백엔드 저장 API 호출
      // 이 엔드포인트는 다음 단계에서 백엔드에 구현할 예정입니다.
      const response = await axios.post('http://localhost:8080/api/ad-content/save', savePayload);

      console.log('광고 콘텐츠 저장 응답:', response.data);
      alert('광고 콘텐츠가 성공적으로 저장되었습니다! ✅');

    } catch (error) {
      console.error('광고 콘텐츠 저장 중 오류 발생:', error);
      const errorMessage = error.response && error.response.data && error.response.data.message
                           ? error.response.data.message
                           : '광고 콘텐츠 저장 중 예상치 못한 오류가 발생했습니다. 😥';
      alert(errorMessage);
    } finally {
      setIsSavingContent(false);
    }
  };


  return (
    <div style={{ maxWidth: 600, margin: '40px auto', padding: 20, border: '1px solid #ddd', borderRadius: 10, boxShadow: '0 2px 8px rgba(0,0,0,0.1)', backgroundColor: 'rgba(255, 255, 255, 0.9)', fontFamily: 'Arial, sans-serif', textAlign: 'center' }}>
      <h2 style={{ marginBottom: 20, color: '#333' }}>🖼️ 광고 이미지 합성기</h2>

      <div style={{ marginBottom: 15, padding: 10, border: '1px dashed #007bff', borderRadius: 5, backgroundColor: '#e6f7ff' }}>
        <strong>선택된 문구:</strong> {selectedText || '문구 생성기에서 문구를 선택해주세요. ⚠️'}
        {/* ✅ 추가: TextGen 파라미터 미리보기 (디버깅용) */}
        {textGenParams && (
          <div style={{ fontSize: '0.8em', color: '#666', marginTop: '5px' }}>
            ({textGenParams.product} | {textGenParams.target})
          </div>
        )}
      </div>

      <input type="file" accept="image/*" onChange={handleFileChange} style={{ marginBottom: 15 }} />

      <button onClick={handleCompose} disabled={isLoading || !selectedText} style={{
        width: '100%', padding: 12, backgroundColor: (isLoading || !selectedText) ? '#999' : '#007bff',
        color: 'white', border: 'none', borderRadius: 6, fontSize: 16,
        cursor: (isLoading || !selectedText) ? 'not-allowed' : 'pointer', marginBottom: 20
      }}>
        {isLoading ? '합성 중...' : '이미지 합성하기'}
      </button>

      {resultUrl && (
        <div style={{ marginBottom: 20 }}>
          <img src={resultUrl} alt="합성 결과" style={{ maxWidth: '100%', borderRadius: 8 }} />
          <a href={resultUrl} download={`composite_${getTimestampString()}.png`} style={{ display: 'block', marginTop: 10, color: '#007bff', textDecoration: 'underline', cursor: 'pointer' }}>
            이미지 다운로드
          </a>
        </div>
      )}

      {/* ✅ 추가: 콘텐츠 저장 버튼 */}
      {resultUrl && ( // 이미지가 생성되었을 때만 버튼 표시
        <button onClick={handleSaveContent} disabled={isSavingContent} style={{
          width: '100%', padding: 12, backgroundColor: isSavingContent ? '#ccc' : '#28a745',
          color: 'white', border: 'none', borderRadius: 6, fontSize: 16,
          cursor: isSavingContent ? 'not-allowed' : 'pointer'
        }}>
          {isSavingContent ? '저장 중...' : '생성된 광고 콘텐츠 저장하기 ✅'}
        </button>
      )}
    </div>
  );
}

export default ImageGenerator;