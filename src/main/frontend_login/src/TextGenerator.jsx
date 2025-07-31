// src/TextGenerator.jsx (단 한 글자도 생략 없이, 전체를 대체하세요!)

import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom'; // 페이지 이동을 위해 useNavigate 임포트

// onTextSelect prop은 더 이상 필요 없습니다.
function TextGenerator() {
  const navigate = useNavigate(); // useNavigate 훅 사용

  // 폼 데이터 상태 (기존 코드 유지)
  const [form, setForm] = useState({
    product: '',
    target: '',
    purpose: '',
    keyword: '',
    duration: '',
  });
  // 생성된 광고 문구들 (여러 개일 수 있으므로 배열)
  const [adTexts, setAdTexts] = useState([]);
  // 로딩 상태
  const [loading, setLoading] = useState(false);
  // 에러 메시지 상태
  const [error, setError] = useState(''); // 에러 메시지 상태 추가

  // 입력 필드 변경 핸들러 (기존 코드 유지)
  const handleChange = (e) => {
    setForm(prevForm => ({ ...prevForm, [e.target.name]: e.target.value }));
  };

  // 폼 제출 (문구 생성) 핸들러 (기존 코드 유지)
  const handleSubmit = async (e) => {
    e.preventDefault(); // 기본 폼 제출 동작 방지
    setLoading(true); // 로딩 상태 활성화
    setAdTexts([]); // 이전 문구 초기화
    setError(''); // 에러 메시지 초기화 (새 요청 시)

    // 모든 필드 입력 유효성 검사 (기존 코드 유지)
    const formValues = Object.values(form);
    const isValid = formValues.every(value => value.trim() !== '');
    if (!isValid) {
      setError('모든 필드를 입력해주세요! 😅');
      setLoading(false);
      return;
    }

    // JWT 토큰 가져오기 및 확인 (기존 코드 유지)
    const token = localStorage.getItem('jwtToken'); 
    if (!token) {
      setError('로그인이 필요합니다. 다시 로그인해주세요!'); // 에러 메시지 설정
      // alert('로그인이 필요합니다. 다시 로그인해주세요!'); // alert 추가 (옵션)
      navigate('/auth/login'); // 로그인 페이지로 리다이렉트
      setLoading(false);
      return;
    }

    try {
      // axios 요청에 headers 객체를 추가하고 Authorization 헤더를 포함 (기존 코드 유지)
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080';
      const res = await axios.post(`${apiUrl}/api/generate`, form, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}` // JWT 토큰을 'Bearer ' 스키마로 추가
        }
      });
      // OpenAI API 응답에서 생성된 텍스트 목록을 받아옵니다.
      setAdTexts(res.data.adTexts || []); // res.data.adTexts 사용 (기존 코드 유지)
    } catch (err) {
      console.error('❌ 광고 문구 생성 오류:', err); // 콘솔에 에러 출력

      // 에러 메시지 개선: 401 Unauthorized 에러 처리 추가 (기존 코드 유지)
      const errorMessage = err.response && err.response.status === 401
                         ? '인증이 필요하거나 세션이 만료되었습니다. 다시 로그인해주세요.'
                         : err.response?.data?.message || err.message || '광고 문구 생성 중 오류가 발생했습니다. 백엔드 서버를 확인해주세요.';
      setError(errorMessage); // 사용자에게 표시될 에러 메시지 설정

      // 토큰 만료 등 인증 오류 시 로그인 페이지로 리다이렉트 (기존 코드 유지)
      if (err.response?.status === 401 || err.response?.status === 403) {
        // alert('로그인이 만료되었거나 권한이 없습니다. 다시 로그인해주세요.'); // alert 추가 (옵션)
        localStorage.removeItem('jwtToken'); // 만료된 토큰 삭제
        navigate('/auth/login'); // 로그인 페이지로 리다이렉트
      }
    } finally {
      setLoading(false); // 로딩 상태 비활성화
    }
  };

  // 문구 선택 핸들러: 선택된 문구와 form 데이터를 localStorage에 저장하고 이미지 생성 페이지로 이동
  const handleSelectText = (selectedText) => {
    // 💡💡💡 기존 onTextSelect 로직을 localStorage 저장 및 navigate로 변경 💡💡💡
    // TextGenerator에서 여러 문구가 생성되므로, 선택된 하나만 넘겨줌
    if (!selectedText) {
      setError('선택할 문구가 없습니다.'); // 선택된 문구가 없으면 에러
      return;
    }

    // localStorage에 데이터 저장
    localStorage.setItem('selectedAdText', selectedText); // 선택된 텍스트 저장
    // 폼 데이터는 ImageGenerator에서 필요하므로 JSON.stringify로 문자열 변환하여 저장
    localStorage.setItem('textGenParams', JSON.stringify(form)); 

    // ImageGenerator 페이지로 이동
    navigate('/image-generator');
  };


  // --- 스타일 정의 --- (기존 코드 유지)
  const inputStyle = {
    width: '100%',
    padding: 12,
    borderRadius: 10,
    border: '1px solid #ced4da',
    fontSize: '1.1em',
    backgroundColor: '#fff',
    boxShadow: 'inset 0 1px 3px rgba(0,0,0,0.05)',
    outline: 'none',
  };

  const buttonStyle = {
    width: '100%',
    padding: 15,
    backgroundColor: '#28a745',
    color: 'white',
    border: 'none',
    borderRadius: 10,
    fontSize: '1.2em',
    fontWeight: 'bold',
    cursor: 'pointer',
    boxShadow: '0 4px 10px rgba(40,167,69,0.2)',
  };

  const adTextButtonStyle = {
    display: 'block',
    width: '100%',
    textAlign: 'left',
    border: '1px solid #ced4da',
    borderRadius: 8,
    padding: 12,
    marginTop: 10,
    backgroundColor: '#fff',
    fontSize: '1.1em',
    cursor: 'pointer',
    boxShadow: '0 2px 5px rgba(0,0,0,0.05)',
  };
  // --- 스타일 정의 끝 ---


  // 컴포넌트 렌더링 부분 (기존 코드 유지)
  return (
    <div style={{
      maxWidth: 600,
      margin: '40px auto',
      padding: 30,
      backgroundColor: 'rgba(255,255,255,0.9)',
      borderRadius: 15,
      boxShadow: '0 8px 20px rgba(0,0,0,0.08)',
      fontFamily: 'Arial, sans-serif',
      color: '#343a40'
    }}>
      <h2 style={{ color: '#495057', textAlign: 'center', marginBottom: 30, fontSize: '2em', fontWeight: 600 }}>
        ✨ 광고 문구 생성기 ✨
      </h2>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 15 }}>
        <input name="product" value={form.product} onChange={handleChange} placeholder="제품명 (예: 럭셔리 시계)" style={inputStyle} />
        <input name="target" value={form.target} onChange={handleChange} placeholder="타겟 (예: 30대 남성 직장인)" style={inputStyle} />
        <input name="purpose" value={form.purpose} onChange={handleChange} placeholder="목적 (예: 구매 유도, 브랜드 인지도 향상)" style={inputStyle} />
        <input name="keyword" value={form.keyword} onChange={handleChange} placeholder="강조 키워드 (예: 프리미엄, 한정판)" style={inputStyle} />
        <input name="duration" value={form.duration} onChange={handleChange} placeholder="광고 기간 (예: 5일, 1개월)" style={inputStyle} />
        <button type="submit" disabled={loading} style={buttonStyle}>
          {loading ? '문구 생성 중... ⏳' : '광고 문구 생성하기 🚀'}
        </button>
      </form>
      {error && <p style={{ color: 'red', textAlign: 'center', marginTop: '10px' }}>{error}</p>} {/* 에러 표시 */}
      {adTexts.length > 0 && (
        <div style={{ marginTop: 30 }}>
          <h3 style={{ color: '#495057', marginBottom: 15, fontSize: '1.3em', fontWeight: 600 }}>
            👇 문구를 선택하세요:
          </h3>
          {adTexts.map((text, idx) => (
            <button key={idx} onClick={() => handleSelectText(text)} style={adTextButtonStyle}>
              {text}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default TextGenerator;