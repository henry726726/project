// src/TextGenerator.jsx

import React, { useState } from 'react';
import axios from 'axios';

function TextGenerator({ onTextSelect }) {
  const [form, setForm] = useState({
    product: '',
    target: '',
    purpose: '',
    keyword: '',
    duration: '',
  });
  const [adTexts, setAdTexts] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm(prevForm => ({ ...prevForm, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setAdTexts([]);

    const formValues = Object.values(form);
    const isValid = formValues.every(value => value.trim() !== '');
    if (!isValid) {
      alert('모든 필드를 입력해주세요! 😅');
      setLoading(false);
      return;
    }

    // 💡💡💡 JWT 토큰을 localStorage에서 가져옵니다! 💡💡💡
    const token = localStorage.getItem('jwtToken'); 
    if (!token) {
      alert('로그인이 필요합니다. 다시 로그인해주세요!'); // 토큰 없으면 알림
      setLoading(false);
      return;
    }

    try {
      // 💡💡💡 axios 요청에 headers 객체를 추가하고 Authorization 헤더를 포함합니다! 💡💡💡
      const res = await axios.post('http://localhost:8080/api/generate', form, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}` // JWT 토큰을 'Bearer ' 스키마로 추가
        }
      });
      // OpenAI API 응답에서 생성된 텍스트 목록을 받아옵니다.
      setAdTexts(res.data.adTexts || []);
    } catch (err) {
      console.error('❌ 광고 문구 생성 오류:', err);
      // 💡💡💡 에러 메시지 개선: 401 Unauthorized 에러 처리 추가 💡💡💡
      const errorMessage = err.response && err.response.status === 401
                         ? '인증이 필요하거나 세션이 만료되었습니다. 다시 로그인해주세요.'
                         : err.response?.data?.message || err.message || '광고 문구 생성 중 오류가 발생했습니다. 백엔드 서버를 확인해주세요.';
      alert(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectText = (selectedText) => {
    if (onTextSelect) {
      // 최신 form 상태를 안전하게 전달
      onTextSelect(selectedText, { ...form });
    }
  };

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

export default TextGenerator;