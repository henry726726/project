import React, { useState } from 'react';

function FacebookInput() {
  // 광고 설정 값들을 저장할 상태
  const [adSettings, setAdSettings] = useState({
    billingEvent: 'IMPRESSIONS', // 기본값 설정
    optimizationGoal: 'LINK_CLICKS', // 기본값 설정
    bidStrategy: 'LOWEST_COST_WITHOUT_CAP', // 기본값 설정
    dailyBudget: '', // 초기에는 비워둠 (숫자 입력)
    startTime: '', // 초기에는 비워둠 (날짜/시간 입력)
  });

  // 입력 필드 값이 변경될 때 상태를 업데이트하는 함수
  const handleChange = (e) => {
    const { name, value } = e.target;
    setAdSettings(prevSettings => ({
      ...prevSettings,
      [name]: value,
    }));
  };

  // '설정 저장' 버튼을 클릭했을 때 실행될 함수
  const handleSaveSettings = () => {
    // 여기에 실제 백엔드 서버로 데이터를 전송하는 로직이 들어갈 거예요.
    // 예를 들어, axios.post('/api/meta/ad-settings', adSettings);
    console.log('저장할 광고 설정:', adSettings);
    alert('광고 설정이 저장되었습니다! 🎉');
  };

  // 현재 설정 미리보기를 위한 컴포넌트 내부 스타일
  const tdStyle = {
    border: '1px solid #ccc',
    padding: '8px',
    verticalAlign: 'top',
    fontWeight: 'normal',
    color: '#555'
  };
  const thStyle = {
    border: '1px solid #ccc',
    padding: '8px',
    backgroundColor: '#e0e0e0',
    textAlign: 'left',
    fontWeight: 'bold',
    color: '#333',
    width: '40%'
  };

  return (
    <div style={{
      maxWidth: '600px',
      margin: '40px auto',
      padding: '25px',
      border: '1px solid #ddd',
      borderRadius: '10px',
      boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
      backgroundColor: '#fff',
      fontFamily: 'Arial, sans-serif'
    }}>
      <h2 style={{ color: '#333', textAlign: 'center', marginBottom: '30px' }}>📊 페이스북 광고 설정</h2>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
        {/* 과금 기준 (billingEvent) */}
        <div>
          <label style={labelStyle}>과금 기준 (Billing Event):</label>
          <select
            name="billingEvent"
            value={adSettings.billingEvent}
            onChange={handleChange}
            style={inputStyle}
          >
            <option value="IMPRESSIONS">노출 (IMPRESSIONS)</option>
            <option value="LINK_CLICKS">링크 클릭 (LINK_CLICKS)</option>
            {/* 추가 옵션은 Meta API 문서 참고 */}
          </select>
        </div>

        {/* 최적화 목표 (optimizationGoal) */}
        <div>
          <label style={labelStyle}>최적화 목표 (Optimization Goal):</label>
          <select
            name="optimizationGoal"
            value={adSettings.optimizationGoal}
            onChange={handleChange}
            style={inputStyle}
          >
            <option value="LINK_CLICKS">링크 클릭 (LINK_CLICKS)</option>
            <option value="REACH">도달 (REACH)</option>
            <option value="CONVERSIONS">전환 (CONVERSIONS)</option>
            {/* 추가 옵션은 Meta API 문서 참고 */}
          </select>
        </div>

        {/* 입찰 방식 (bidStrategy) */}
        <div>
          <label style={labelStyle}>입찰 방식 (Bid Strategy):</label>
          <select
            name="bidStrategy"
            value={adSettings.bidStrategy}
            onChange={handleChange}
            style={inputStyle}
          >
            <option value="LOWEST_COST_WITHOUT_CAP">최저 비용 (LOWEST_COST_WITHOUT_CAP)</option>
            <option value="COST_CAP">비용 상한 (COST_CAP)</option>
            {/* 추가 옵션은 Meta API 문서 참고 */}
          </select>
        </div>

        {/* 하루 예산 (dailyBudget) */}
        <div>
          <label style={labelStyle}>하루 예산 (Daily Budget - 원):</label>
          <input
            type="number" // 숫자만 입력 가능
            name="dailyBudget"
            value={adSettings.dailyBudget}
            onChange={handleChange}
            placeholder="예: 140000 (1400원)"
            style={inputStyle}
          />
        </div>

        {/* 광고 시작 시간 (startTime) */}
        <div>
          <label style={labelStyle}>광고 시작 시간 (Start Time):</label>
          <input
            type="datetime-local" // 날짜와 시간 선택 필드
            name="startTime"
            value={adSettings.startTime}
            onChange={handleChange}
            style={inputStyle}
          />
        </div>

        {/* 설정 저장 버튼 */}
        <button
          onClick={handleSaveSettings}
          style={{
            width: '100%',
            padding: '12px 20px',
            marginTop: '20px',
            backgroundColor: '#1877F2',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            fontSize: '18px',
            fontWeight: 'bold',
            cursor: 'pointer',
            transition: 'background-color 0.2s ease',
            boxShadow: '0 4px 8px rgba(24,119,242,0.2)'
          }}
          onMouseOver={e => e.currentTarget.style.backgroundColor = '#105fb2'}
          onMouseOut={e => e.currentTarget.style.backgroundColor = '#1877F2'}
        >
          설정 저장하기
        </button>
      </div>

      {/* 현재 설정 미리보기 */}
      <div style={{ marginTop: '40px', padding: '15px', backgroundColor: '#eef3f9', borderRadius: '8px', boxShadow: 'inset 0 1px 3px rgba(0,0,0,0.1)' }}>
        <h3 style={{ color: '#444', marginBottom: '15px', borderBottom: '1px solid #ccc', paddingBottom: '10px' }}>현재 설정 미리보기</h3>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <tbody>
            <tr>
              <th style={thStyle}>과금 기준</th>
              <td style={tdStyle}>{adSettings.billingEvent || '미설정'}</td>
            </tr>
            <tr>
              <th style={thStyle}>최적화 목표</th>
              <td style={tdStyle}>{adSettings.optimizationGoal || '미설정'}</td>
            </tr>
            <tr>
              <th style={thStyle}>입찰 방식</th>
              <td style={tdStyle}>{adSettings.bidStrategy || '미설정'}</td>
            </tr>
            <tr>
              <th style={thStyle}>하루 예산</th>
              <td style={tdStyle}>{adSettings.dailyBudget ? `${adSettings.dailyBudget} 원` : '미설정'}</td>
            </tr>
            <tr>
              <th style={thStyle}>광고 시작 시간</th>
              <td style={tdStyle}>{adSettings.startTime || '미설정'}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}

// 공통 스타일 정의
const labelStyle = {
  display: 'block',
  marginBottom: '5px',
  fontWeight: 'bold',
  color: '#444',
  fontSize: '0.95em'
};

const inputStyle = {
  width: '100%',
  padding: '10px 12px',
  border: '1px solid #ccc',
  borderRadius: '5px',
  fontSize: '1em',
  boxSizing: 'border-box'
};

export default FacebookInput;