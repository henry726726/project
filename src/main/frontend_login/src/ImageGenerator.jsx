// src/ImageGenerator.jsx (단 한 글자도 생략 없이, 전체를 대체하세요!)

import React, { useState, useEffect } from 'react'; // Suspense는 제거 (사용하지 않으므로)
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

function ImageGenerator() {
  const navigate = useNavigate();

  const [selectedAdText, setSelectedAdText] = useState(null);
  const [textGenParams, setTextGenParams] = useState(null);

  const [imageFile, setImageFile] = useState(null);
  const [resultUrl, setResultUrl] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSavingContent, setIsSavingContent] = useState(false);
  const [error, setError] = useState('');

  // ESLint 경고 방지를 위해 주석 처리
  // eslint-disable-next-line no-unused-vars
  const [mode, setMode] = useState('controlnet');


  useEffect(() => {
    const storedText = localStorage.getItem('selectedAdText');
    const storedParams = localStorage.getItem('textGenParams');

    if (storedText) {
      setSelectedAdText(storedText);
    } else {
      alert("선택된 문구가 없습니다. 문구 생성 페이지로 이동합니다.");
      navigate('/text-generator');
      return;
    }

    if (storedParams) {
      try {
        setTextGenParams(JSON.parse(storedParams));
      } catch (e) {
        console.error("Failed to parse textGenParams from localStorage", e);
        setTextGenParams(null);
      }
    }
  }, [navigate]);


  const handleFileChange = (e) => {
    setImageFile(e.target.files[0]);
  };

  // eslint-disable-next-line no-unused-vars
  const getTimestampString = () => { // 사용되지 않으므로 제거하거나 eslint-disable 처리
    const now = new Date();
    const pad = (n) => n.toString().padStart(2, '0');
    return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
  };

  const handleCompose = async () => {
    if (!imageFile) {
      alert('이미지 파일을 선택해주세요! 😅');
      return;
    }
    if (!selectedAdText) {
      alert('합성할 문구가 선택되지 않았습니다. 문구 생성기에서 문구를 선택해주세요! 😲');
      navigate('/text-generator');
      return;
    }

    setIsLoading(true);
    setResultUrl(null);
    setIsSavingContent(false);
    setError(''); // 새로운 합성 시도 전에 에러 메시지 초기화

    try {
      const form = new FormData();
      form.append('image', imageFile);
      form.append('text', selectedAdText); // ✅ 백엔드 @RequestParam("text")에 맞춰 'text'로 변경
      // form.append('mode', mode); // 백엔드 ImageComposeController는 'mode'를 받지 않으므로 제거 (혹은 백엔드에 추가)

      // ✅ 백엔드 ImageComposeController로 직접 호출 (process.env.REACT_APP_API_URL로 백엔드 API URL 가져오기)
      const backendApiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080';
      const token = localStorage.getItem('jwtToken');

      if (!token) {
        alert('로그인이 필요합니다. 다시 로그인해주세요!');
        navigate('/auth/login');
        setIsLoading(false);
        return;
      }

      const res = await axios.post(
        `${backendApiUrl}/api/compose`, // ✅ ImageComposeController의 /api/compose 엔드포인트
        form,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
            'Authorization': `Bearer ${token}`
          },
          responseType: 'arraybuffer', // ✅ 이미지 데이터를 ArrayBuffer로 받음
        }
      );

      // ArrayBuffer (바이너리 데이터)를 Base64 문자열로 변환
      // btoa()는 이진(binary) 데이터를 Base64로 인코딩하는 데 사용됩니다.
      const base64 = btoa(
        new Uint8Array(res.data)
          .reduce((data, byte) => data + String.fromCharCode(byte), '')
      );
      setResultUrl(`data:image/png;base64,${base64}`); // ✅ PNG 이미지 형식으로 설정
      alert('이미지 합성이 완료되었습니다! ✨');
    } catch (error) {
      console.error('이미지 합성 오류:', error);
      const errorMessage = error.response && error.response.status === 401
                           ? '인증이 필요하거나 세션이 만료되었습니다. 다시 로그인해주세요.'
                           : error.response?.data?.message || error.message || '이미지 합성 중 예상치 못한 오류가 발생했습니다. 😥';
      setError(errorMessage);
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.removeItem('jwtToken');
        navigate('/auth/login');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveContent = async () => {
    if (!resultUrl) {
      alert('저장할 합성된 이미지가 없습니다. 이미지를 먼저 생성해주세요! 🙅‍♀️');
      return;
    }

    setIsSavingContent(true);

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('로그인이 필요합니다. 다시 로그인해주세요!');
      setIsSavingContent(false);
      return;
    }

    try {
      const cleanedBase64Image = resultUrl.split(',')[1];

      const savePayload = {
        // textGenParams가 내부 상태이며 TextGenerator의 form과 구조가 같다고 가정
        // TextGenerator의 form 필드 이름에 맞춰 수정: product, target, purpose, keyword, duration
        product: textGenParams?.product || '', 
        target: textGenParams?.target || '', 
        purpose: textGenParams?.purpose || '', 
        keyword: textGenParams?.keyword || '',
        duration: textGenParams?.duration || '',
        adText: selectedAdText,
        generatedImageBase64: cleanedBase64Image,
      };

      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080';
      const response = await axios.post(`${apiUrl}/api/ad-content/save`, savePayload, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });

      console.log('광고 콘텐츠 저장 응답:', response.data);
      alert('광고 콘텐츠가 성공적으로 저장되었습니다! ✅');

    } catch (error) {
      console.error('광고 콘텐츠 저장 중 오류 발생:', error);
      const errorMessage = error.response && error.response.status === 401
                           ? '인증이 필요하거나 세션이 만료되었습니다. 다시 로그인해주세요.'
                           : error.response?.data?.message || error.message || '광고 콘텐츠 저장 중 예상치 못한 오류가 발생했습니다. 😥';
      alert(errorMessage);
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.removeItem('jwtToken');
        navigate('/auth/login');
      }
    } finally {
      setIsSavingContent(false);
    }
  };


  const buttonStyle = {
    width: '100%',
    padding: '10px 15px',
    margin: '10px 0',
    backgroundColor: '#007bff',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer'
  };

  // eslint-disable-next-line no-unused-vars
  const saveButtonStyle = { // 사용되지 않으므로 제거하거나 eslint-disable 처리
    ...buttonStyle,
    backgroundColor: '#28a745',
  };


  if (selectedAdText === null) {
    return (
      <div style={{ textAlign: 'center', marginTop: '50px' }}>
        문구 데이터를 불러오는 중입니다...
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 600, margin: '40px auto', padding: 20, border: '1px solid #ddd', borderRadius: 10, boxShadow: '0 2px 8px rgba(0,0,0,0.1)', backgroundColor: 'rgba(255, 255, 255, 0.9)', fontFamily: 'Arial, sans-serif', textAlign: 'center' }}>
      <h2 style={{ marginBottom: 20, color: '#333' }}>🖼️ 광고 이미지 합성기</h2>

      <div style={{ marginBottom: 15, padding: 10, border: '1px dashed #007bff', borderRadius: 5, backgroundColor: '#e6f7ff' }}>
        <strong>선택된 문구:</strong> {selectedAdText || '문구 생성기에서 문구를 선택해주세요. ⚠️'}
        {textGenParams && (
          <div style={{ fontSize: '0.8em', color: '#666', marginTop: '5px' }}>
            ({textGenParams.product || '없음'} | {textGenParams.target || '없음'} | {textGenParams.purpose || '없음'}) {/* ✅ 필드명 일치 */}
            {textGenParams.keyword && ` | ${textGenParams.keyword}`}
            {textGenParams.duration && ` | ${textGenParams.duration}`}
          </div>
        )}
      </div>

      <input type="file" accept="image/*" onChange={handleFileChange} style={{ marginBottom: 15 }} />

      <button onClick={handleCompose} disabled={isLoading || !selectedAdText} style={{
        width: '100%', padding: 12, backgroundColor: (isLoading || !selectedAdText) ? '#999' : '#007bff',
        color: 'white', border: 'none', borderRadius: 5, fontSize: '1.1em', cursor: 'pointer', marginBottom: 10,
        opacity: (isLoading || !selectedAdText) ? 0.7 : 1
      }}>
        {isLoading ? '이미지 합성 중... ⏳' : '이미지 합성하기 🎨'}
      </button>

      {/* 에러 메시지 표시 */}
      {error && <p style={{ color: 'red', textAlign: 'center' }}>{error}</p>}

      {/* 생성된 이미지 미리보기 */}
      {resultUrl && (
        <div style={{ marginTop: 20, borderTop: '1px solid #eee', paddingTop: 20 }}>
          <h3>합성된 이미지 👇</h3>
          {/* ✅ 'alt' 속성은 그대로 'Composite Ad' */}
          <img src={resultUrl} alt="Composite Ad" style={{ maxWidth: '100%', height: 'auto', borderRadius: 8, border: '1px solid #ddd' }} />
          <button onClick={handleSaveContent} disabled={isSavingContent} style={{
            width: '100%', padding: 12, marginTop: 15, backgroundColor: isSavingContent ? '#999' : '#28a745',
            color: 'white', border: 'none', borderRadius: 5, fontSize: '1.1em', cursor: 'pointer',
            opacity: isSavingContent ? 0.7 : 1
          }}>
            {isSavingContent ? '콘텐츠 저장 중... 💾' : '광고 콘텐츠 저장 ✅'}
          </button>
        </div>
      )}
    </div>
  );
}

export default ImageGenerator;