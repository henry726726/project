// src/TextGenerator.jsx

import React, { useState } from 'react';
import axios from 'axios'; // axios 임포트 추가

// TextGenerator 컴포넌트가 부모로부터 onTextSelect prop을 받도록 수정
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
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault(); // 폼 기본 제출 동작 방지
    setLoading(true);
    setAdTexts([]); // 이전 문구 초기화

    // 모든 필드가 비어있지 않은지 간단히 검사
    const formValues = Object.values(form);
    const isValid = formValues.every(value => value.trim() !== '');
    if (!isValid) {
      alert('모든 필드를 입력해주세요! 😅');
      setLoading(false);
      return;
    }

    try {
      // PromptForm의 API 호출 주소 사용
      // 백엔드 서버가 8080 포트에서 실행 중이어야 합니다.
      const res = await axios.post('http://localhost:8080/api/generate', form);
      setAdTexts(res.data.adTexts || []);
    } catch (err) {
      console.error('❌ 광고 문구 생성 오류:', err);
      alert('광고 문구 생성 중 오류가 발생했습니다. 백엔드 서버를 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectText = async (selectedText) => {
    // 선택한 문구를 부모 컴포넌트(App.js)로 전달
    // App.js에서 이 문구를 ImageGenerator로 넘겨주고 화면을 전환할 것임
    if (onTextSelect) {
      onTextSelect(selectedText);
    }

    // ⭐ 참고: PromptForm에 있던 서버 저장 로직은 여기에 직접 넣을 수도 있고,
    // 필요하다면 App.js에서 onTextSelect를 받은 후 처리할 수도 있습니다.
    // 여기서는 TextGenerator의 역할에 집중하여 문구 선택 후 전달까지만 구현했습니다.
    // 만약 여기서 서버에 저장해야 한다면 아래 코드를 추가하세요.
    /*
    try {
      await axios.post('http://localhost:8080/userdatainput/content', {
        userId: 'sql_test',
        name: '',
        caption: selectedText,
        imageUrl: '',
        product: form.product,
        target: form.target,
        purpose: form.purpose,
        keyword: form.keyword,
        duration: form.duration,
      });
      alert('문구가 성공적으로 저장되었습니다!');
    } catch (err) {
      console.error('❌ 문구 저장 오류:', err);
      alert('문구 저장 중 오류가 발생했습니다.');
    }
    */
  };

  return (
    <div style={{
      maxWidth: '600px',
      margin: '40px auto',
      padding: '30px',
      border: 'none',
      borderRadius: '15px',
      boxShadow: '0 8px 20px rgba(0,0,0,0.08)',
      backgroundColor: 'rgba(255, 255, 255, 0.9)', // 배경 이미지와 어울리도록 반투명하게
      fontFamily: 'Arial, sans-serif',
      color: '#343a40'
    }}>
      <h2 style={{
        color: '#495057',
        textAlign: 'center',
        marginBottom: '30px',
        fontSize: '2em',
        fontWeight: '600'
      }}>✨ 광고 문구 생성기 ✨</h2>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
        <input
          name="product"
          value={form.product}
          onChange={handleChange}
          placeholder="제품명 (예: 럭셔리 시계)"
          style={inputStyle}
        />
        <input
          name="target"
          value={form.target}
          onChange={handleChange}
          placeholder="타겟 (예: 30대 남성 직장인)"
          style={inputStyle}
        />
        <input
          name="purpose"
          value={form.purpose}
          onChange={handleChange}
          placeholder="목적 (예: 구매 유도, 브랜드 인지도 향상)"
          style={inputStyle}
        />
        <input
          name="keyword"
          value={form.keyword}
          onChange={handleChange}
          placeholder="강조 키워드 (예: 프리미엄, 한정판)"
          style={inputStyle}
        />
        <input
          name="duration"
          value={form.duration}
          onChange={handleChange}
          placeholder="광고 기간 (예: 5일, 1개월)"
          style={inputStyle}
        />

        <button
          type="submit"
          disabled={loading}
          style={{
            width: '100%',
            padding: '15px 20px',
            backgroundColor: loading ? '#a0d9b5' : '#28a745',
            color: 'white',
            border: 'none',
            borderRadius: '10px',
            fontSize: '1.2em',
            fontWeight: 'bold',
            cursor: loading ? 'not-allowed' : 'pointer',
            transition: 'background-color 0.3s ease, transform 0.1s ease',
            boxShadow: '0 4px 10px rgba(40,167,69,0.2)'
          }}
          onMouseOver={(e) => {
            if (!loading) e.currentTarget.style.backgroundColor = '#218838';
          }}
          onMouseOut={(e) => {
            if (!loading) e.currentTarget.style.backgroundColor = '#28a745';
          }}
        >
          {loading ? '문구 생성 중... ⏳' : '광고 문구 생성하기 🚀'}
        </button>
      </form>

      {adTexts.length > 0 && (
        <div style={{ marginTop: '30px' }}>
          <h3 style={{
            color: '#495057',
            marginBottom: '15px',
            fontSize: '1.3em',
            fontWeight: '600'
          }}>👇 문구를 선택하세요:</h3>
          {adTexts.map((text, idx) => (
            <button
              key={idx}
              onClick={() => handleSelectText(text)}
              style={{
                display: 'block',
                width: '100%',
                textAlign: 'left',
                border: '1px solid #ced4da',
                borderRadius: '8px',
                padding: '12px',
                marginTop: '10px',
                backgroundColor: '#ffffff',
                fontSize: '1.1em',
                cursor: 'pointer',
                transition: 'background-color 0.2s ease, transform 0.1s ease',
                boxShadow: '0 2px 5px rgba(0,0,0,0.05)'
              }}
              onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#f0f2f5'}
              onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#ffffff'}
            >
              {text}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

// 인풋 필드 공통 스타일
const inputStyle = {
  width: 'calc(100% - 24px)', // 패딩 고려
  padding: '12px', // 패딩 증가
  border: '1px solid #ced4da', // 테두리 색상
  borderRadius: '10px', // 모서리 둥글게
  fontSize: '1.1em', // 글자 크기
  backgroundColor: '#ffffff', // 흰색 배경
  boxShadow: 'inset 0 1px 3px rgba(0,0,0,0.05)', // 안쪽 그림자
  outline: 'none' // 포커스 시 테두리 제거
};

export default TextGenerator;