// src/ImageGenerator.jsx (단 한 글자도 생략 없이, 전체를 대체하세요!)

import React, { useState, useEffect, Suspense } from 'react'; // Suspense 추가
import axios from 'axios';
import { useNavigate } from 'react-router-dom'; // 페이지 이동을 위해 useNavigate 임포트


// ImageGenerator 컴포넌트: 더 이상 props를 받지 않고 localStorage에서 데이터를 직접 가져옵니다.
function ImageGenerator() {
  const navigate = useNavigate(); // useNavigate 훅 사용

  // === 내부 상태로 전환: localStorage에서 가져온 값을 저장할 State들 ===
  const [selectedAdText, setSelectedAdText] = useState(null); // 선택된 광고 문구 (null로 초기화하여 로딩 상태 확인)
  const [textGenParams, setTextGenParams] = useState(null); // 문구 생성 파라미터
  // ===============================================================

  const [imageFile, setImageFile] = useState(null); // 사용자가 선택한 이미지 파일
  const [resultUrl, setResultUrl] = useState(null); // 합성된 이미지의 Base64 URL
  const [isLoading, setIsLoading] = useState(false); // 이미지 합성 중 로딩 상태
  const [isSavingContent, setIsSavingContent] = useState(false); // DB 저장 중 상태
  const [error, setError] = useState(''); // 에러 메시지

  // 현재는 'controlnet'으로 고정 (ESLint 경고 방지를 위해 주석 처리)
  // eslint-disable-next-line no-unused-vars
  const [mode, setMode] = useState('controlnet'); // 합성 방식 선택용 (필요하면 사용)


  // === useEffect: 컴포넌트 마운트 시 localStorage에서 데이터 불러오기 ===
  useEffect(() => {
    const storedText = localStorage.getItem('selectedAdText'); // 'selectedAdText' 가져오기
    const storedParams = localStorage.getItem('textGenParams'); // 'textGenParams' 가져오기

    if (storedText) { // 문구가 있다면 상태에 설정
      setSelectedAdText(storedText);
    } else {
      // 💡💡💡 문구가 없으면 경고 후 문구 생성 페이지로 강제 리다이렉트! 💡💡💡
      alert("선택된 문구가 없습니다. 문구 생성 페이지로 이동합니다.");
      navigate('/text-generator');
      return; // 리다이렉트 후 함수 종료
    }

    if (storedParams) { // 파라미터가 있다면 JSON 파싱 후 상태에 설정
      try {
        setTextGenParams(JSON.parse(storedParams));
      } catch (e) {
        console.error("Failed to parse textGenParams from localStorage", e);
        setTextGenParams(null); // 파싱 실패 시 null로 설정
      }
    }
    // 이 useEffect는 딱 한 번만 실행되도록 빈 의존성 배열을 둡니다.
  }, [navigate]); // navigate가 변경될 때 useEffect 재실행 (React Hook의 규칙 준수)


  // 이미지 파일 선택 핸들러
  const handleFileChange = (e) => {
    setImageFile(e.target.files[0]);
  };

  // 타임스탬프 문자열 생성 (현재는 사용되지 않으므로 유지할지 제거할지 결정 필요)
  const getTimestampString = () => {
    const now = new Date();
    const pad = (n) => n.toString().padStart(2, '0');
    return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
  };

  // 이미지 합성 버튼 클릭 핸들러
  const handleCompose = async () => {
    if (!imageFile) {
      alert('이미지 파일을 선택해주세요! 😅');
      return;
    }
    // selectedAdText (내부 상태)를 사용합니다.
    if (!selectedAdText) { // 💡 selectedAdText가 null인 경우도 포함하여 체크
      alert('합성할 문구가 선택되지 않았습니다. 문구 생성기에서 문구를 선택해주세요! 😲');
      // TextGenerator로 이동
      navigate('/text-generator');
      return;
    }

    setIsLoading(true); // 로딩 상태 활성화
    setResultUrl(null); // 이전 결과 URL 초기화
    setIsSavingContent(false); // 저장 상태 초기화 (새 합성 시작)

    try {
      const form = new FormData(); // FormData 객체 생성 (파일 전송용)
      form.append('image', imageFile); // 이미지 파일 추가
      form.append('prompt', selectedAdText); // 광고 문구를 'prompt'로 전송
      form.append('mode', mode); // 'mode' (현재 'controlnet') 추가

      const pythonApiUrl = process.env.REACT_APP_PYTHON_API_URL || 'http://localhost:8000/generate'; // 파이썬 이미지 서버 URL
      const token = localStorage.getItem('jwtToken'); // JWT 토큰 가져오기

      if (!token) { // 토큰 없으면 로그인 경고 후 리다이렉트
        alert('로그인이 필요합니다. 다시 로그인해주세요!');
        navigate('/auth/login');
        setIsLoading(false);
        return;
      }

      // axios.post 요청: FormData와 인증 헤더 포함
      const res = await axios.post(
        pythonApiUrl, // 이미지 생성 엔드포인트
        form, // 요청 본문: FormData 객체
        {
          headers: {
            'Content-Type': 'multipart/form-data', // FormData 사용 시 필수
            'Authorization': `Bearer ${token}` // JWT 토큰 포함
          },
          responseType: 'arraybuffer', // 이미지 데이터를 ArrayBuffer로 받음
        }
      );

      // ArrayBuffer (바이너리 데이터)를 Base64 문자열로 변환
      // btoa()는 이진(binary) 데이터를 Base64로 인코딩하는 데 사용됩니다.
      const base64 = btoa(
        new Uint8Array(res.data) // 받은 arraybuffer를 Uint8Array로 변환
          .reduce((data, byte) => data + String.fromCharCode(byte), '') // 각 바이트를 문자열로 변환 후 합침
      );
      setResultUrl(`data:image/png;base64,${base64}`); // PNG 이미지 형식으로 설정
      alert('이미지 합성이 완료되었습니다! ✨');
    } catch (error) {
      console.error('이미지 합성 오류:', error); // 콘솔에 에러 출력
      const errorMessage = error.response && error.response.status === 401
                           ? '인증이 필요하거나 세션이 만료되었습니다. 다시 로그인해주세요.'
                           : error.response?.data?.message || error.message || '이미지 합성 중 예상치 못한 오류가 발생했습니다. 😥';
      setError(errorMessage);
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.removeItem('jwtToken');
        navigate('/auth/login');
      }
    } finally {
      setIsLoading(false); // 로딩 상태 비활성화
    }
  };

  // 광고 콘텐츠 (합성 이미지와 문구) 저장 핸들러
  const handleSaveContent = async () => {
    if (!resultUrl) {
      alert('저장할 합성된 이미지가 없습니다. 이미지를 먼저 생성해주세요! 🙅‍♀️');
      return;
    }

    setIsSavingContent(true); // 저장 로딩 상태 활성화

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('로그인이 필요합니다. 다시 로그인해주세요!');
      setIsSavingContent(false);
      return;
    }

    try {
      const cleanedBase64Image = resultUrl.split(',')[1]; // Base64 접두사 제거 (백엔드 요구사항에 따라)

      const savePayload = {
        // textGenParams (내부 상태)를 사용합니다.
        product: textGenParams?.productName || '', // textGenParams가 productName 속성을 가질 것으로 가정
        target: textGenParams?.targetAudience || '', // textGenParams가 targetAudience 속성을 가질 것으로 가정
        purpose: textGenParams?.purpose || '', // textGenParams가 purpose 속성을 가질 것으로 가정
        keyword: textGenParams?.keyword || '', // (TextGenerator 폼에 keyword와 duration이 있다면)
        duration: textGenParams?.duration || '',
        adText: selectedAdText, // selectedAdText (내부 상태) 사용
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


  /* --- 스타일 정의 --- */
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

  const saveButtonStyle = {
    ...buttonStyle,
    backgroundColor: '#28a745', // 저장 버튼은 다른 색상으로
  };
  /* --- 스타일 정의 끝 --- */

  // 로딩 스피너 (데이터 불러오는 중)
  if (selectedAdText === null) { // selectedAdText가 아직 null이면 로딩 중
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
        {/* selectedAdText가 비어있으면 경고 메시지 표시 */}
        <strong>선택된 문구:</strong> {selectedAdText || '문구 생성기에서 문구를 선택해주세요. ⚠️'}
        {/* textGenParams가 존재하면 문구 생성 시 사용된 파라미터 표시 */}
        {textGenParams && (
          <div style={{ fontSize: '0.8em', color: '#666', marginTop: '5px' }}>
            ({textGenParams.productName || '없음'} | {textGenParams.targetAudience || '없음'} | {textGenParams.purpose || '없음'})
            {textGenParams.keyword && ` | ${textGenParams.keyword}`} {/* keyword가 있다면 추가 표시 */}
            {textGenParams.duration && ` | ${textGenParams.duration}`} {/* duration이 있다면 추가 표시 */}
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