// src/FacebookInput.jsx

import React, { useState } from 'react';
import axios from 'axios';

function FacebookInput() {
  // 광고 설정 값들을 저장할 상태
  const [adSettings, setAdSettings] = useState({
    billingEvent: 'IMPRESSIONS',
    optimizationGoal: 'LINK_CLICKS',
    bidStrategy: 'LOWEST_COST_WITHOUT_CAP',
    dailyBudget: '',
    startTime: '',
  });

  const [isSaving, setIsSaving] = useState(false); // 저장 중 상태
  // ✅ 추가: 광고가 한 번이라도 성공적으로 생성(업로드)되었는지 추적하는 상태
  const [adCreatedOrUpdated, setAdCreatedOrUpdated] = useState(false);

  // 입력 필드 값이 변경될 때 상태를 업데이트하는 함수
  const handleChange = (e) => {
    const { name, value } = e.target;
    setAdSettings(prevSettings => ({
      ...prevSettings,
      [name]: value,
    }));
    // 입력값이 변경되면, "생성/업데이트" 상태를 초기화하여 다시 "생성하기" 버튼으로 돌아가게 할 수도 있습니다.
    // 여기서는 유지하되, 필요에 따라 setAdCreatedOrUpdated(false); 추가 고려
  };

  // '광고 생성하기' 또는 '업로드하기' 버튼을 클릭했을 때 실행될 함수
  const handleCreateAd = async () => {
    if (!adSettings.dailyBudget || !adSettings.startTime) {
      alert('하루 예산과 광고 시작 시간은 필수로 입력해야 합니다! 😅');
      return;
    }

    setIsSaving(true);

    try {
      // ✅ 백엔드 API로 설정값 전송 로직
      const response = await axios.post('http://localhost:8080/api/meta/create-ad', adSettings);

      console.log('광고 캠페인 생성/업데이트 응답:', response.data);
      alert('광고 캠페인이 성공적으로 생성/업데이트되었습니다! 🎉');

      // ✅ 성공 시: 광고가 생성되었음을 나타내는 상태 업데이트
      setAdCreatedOrUpdated(true);

      // 성공 후 입력 필드 초기화 (선택 사항) - 일반적으로 업데이트 버튼으로 변경되면 초기화 안함
      // setAdSettings({ ... });

    } catch (error) {
      console.error('광고 캠페인 생성/업데이트 중 오류 발생:', error);
      const errorMessage = error.response && error.response.data && error.response.data.message
                           ? error.response.data.message
                           : '광고 캠페인 생성/업데이트 중 예상치 못한 오류가 발생했습니다.';
      alert(errorMessage);
    } finally {
      setIsSaving(false);
    }
  };

  // '광고 생성하기' 버튼 표시 조건: dailyBudget과 startTime이 모두 채워졌을 때
  const canShowCreateAdButton = adSettings.dailyBudget && adSettings.startTime;

  // ✅ 버튼 텍스트 결정: adCreatedOrUpdated 상태에 따라 달라짐
  const buttonText = adCreatedOrUpdated ? '광고 업로드하기' : '광고 생성하기';


  // 스타일 정의 (이전과 동일)
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
        {/* 광고 설정 입력 필드들은 이전과 동일 */}
        {/* ... (과금 기준, 최적화 목표, 입찰 방식, 하루 예산, 광고 시작 시간 필드) ... */}

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
          </select>
        </div>

        {/* 하루 예산 (dailyBudget) */}
        <div>
          <label style={labelStyle}>하루 예산 (Daily Budget - 원):</label>
          <input
            type="number"
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
            type="datetime-local"
            name="startTime"
            value={adSettings.startTime}
            onChange={handleChange}
            style={inputStyle}
          />
        </div>


        {/* ✅ 광고 생성/업로드하기 버튼: 조건부 렌더링 및 텍스트 변경 적용 */}
        {canShowCreateAdButton && (
          <button
            onClick={handleCreateAd}
            disabled={isSaving}
            style={{
              width: '100%',
              padding: '12px 20px',
              marginTop: '20px',
              backgroundColor: isSaving ? '#cccccc' : '#1877F2',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              fontSize: '18px',
              fontWeight: 'bold',
              cursor: isSaving ? 'not-allowed' : 'pointer',
              transition: 'background-color 0.2s ease',
              boxShadow: '0 4px 8px rgba(24,119,242,0.2)'
            }}
            onMouseOver={e => !isSaving && (e.currentTarget.style.backgroundColor = '#105fb2')}
            onMouseOut={e => !isSaving && (e.currentTarget.style.backgroundColor = '#1877F2')}
          >
            {isSaving ? '진행 중...' : buttonText}
          </button>
        )}
      </div>

      {/* 현재 설정 미리보기는 이전과 동일 */}
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

export default FacebookInput;