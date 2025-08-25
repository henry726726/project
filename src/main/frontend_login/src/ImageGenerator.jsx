import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

function ImageGenerator() {
  const navigate = useNavigate();

  const [selectedAdText, setSelectedAdText] = useState(null);
  const [textGenParams, setTextGenParams] = useState(null);

  const [imageFile, setImageFile] = useState(null);
  const [originalBase64, setOriginalBase64] = useState(null); // ✅ 원본 이미지 Base64 저장용 추가
  const [resultUrl, setResultUrl] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSavingContent, setIsSavingContent] = useState(false);
  const [error, setError] = useState('');

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

  // ✅ 이미지 파일 선택 시 원본 Base64로 변환해 저장
  const handleFileChange = (e) => {
    const file = e.target.files[0];
    setImageFile(file);

    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        const base64String = reader.result.split(',')[1];
        setOriginalBase64(base64String); // ✅ 원본 Base64 저장
      };
      reader.readAsDataURL(file);
    }
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
    setError('');

    try {
      const form = new FormData();
      form.append('image', imageFile);
      form.append('text', selectedAdText);

      const backendApiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080';
      const token = localStorage.getItem('jwtToken');

      if (!token) {
        alert('로그인이 필요합니다. 다시 로그인해주세요!');
        navigate('/auth/login');
        setIsLoading(false);
        return;
      }

      const res = await axios.post(
        `${backendApiUrl}/api/compose`,
        form,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
            'Authorization': `Bearer ${token}`
          },
          responseType: 'arraybuffer',
        }
      );

      const base64 = btoa(
        new Uint8Array(res.data)
          .reduce((data, byte) => data + String.fromCharCode(byte), '')
      );
      setResultUrl(`data:image/png;base64,${base64}`);
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
        product: textGenParams?.product || '',
        target: textGenParams?.target || '',
        purpose: textGenParams?.purpose || '',
        keyword: textGenParams?.keyword || '',
        duration: textGenParams?.duration || '',
        adText: selectedAdText,
        generatedImageBase64: cleanedBase64Image,
        originalImageBase64: originalBase64, // ✅ 원본 Base64도 저장
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
            ({textGenParams.product || '없음'} | {textGenParams.target || '없음'} | {textGenParams.purpose || '없음'})
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

      {error && <p style={{ color: 'red', textAlign: 'center' }}>{error}</p>}

      {resultUrl && (
        <div style={{ marginTop: 20, borderTop: '1px solid #eee', paddingTop: 20 }}>
          <h3>합성된 이미지 👇</h3>
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
