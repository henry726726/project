// src/FacebookInput.jsx

import React, { useState, useEffect } from 'react';
import axios from 'axios';

function FacebookInput() {
  const apiBase = process.env.REACT_APP_API_URL || 'http://localhost:8080';

  const [adAccounts, setAdAccounts] = useState([]);
  const [selectedAccount, setSelectedAccount] = useState('');

  const [adSettings, setAdSettings] = useState({
    accountId: '',
    pageId: '',
    link: '',
    billingEvent: 'IMPRESSIONS',
    optimizationGoal: 'LINK_CLICKS',
    bidStrategy: 'LOWEST_COST_WITHOUT_CAP',
    dailyBudget: '',
    startTime: '',
  });

  const [isSaving, setIsSaving] = useState(false);
  const [adCreatedOrUpdated, setAdCreatedOrUpdated] = useState(false);

  // 광고 계정 목록 불러오기
  useEffect(() => {
    const jwtToken = localStorage.getItem('jwtToken');
    if (!jwtToken) return;

    axios
      .get(`${apiBase}/meta/adaccounts`, {
        headers: { Authorization: `Bearer ${jwtToken}` },
      })
      .then((res) => setAdAccounts(res.data))
      .catch((err) => console.error('광고 계정 불러오기 실패:', err));
  }, [apiBase]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setAdSettings((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleAccountSelect = (e) => {
    const value = e.target.value;
    setSelectedAccount(value);
    if (value) {
      const [accountId, pageId] = value.split(',');
      setAdSettings((prev) => ({
        ...prev,
        accountId,
        pageId,
      }));
    } else {
      setAdSettings((prev) => ({
        ...prev,
        accountId: '',
        pageId: '',
      }));
    }
  };

  const handleCreateAd = async () => {
    if (!adSettings.accountId || !adSettings.pageId) {
      alert('광고 계정을 선택해 주세요.');
      return;
    }
    if (!adSettings.link) {
      alert('랜딩 URL을 입력해 주세요.');
      return;
    }
    if (!adSettings.dailyBudget || !adSettings.startTime) {
      alert('하루 예산과 광고 시작 시간은 필수 입력 항목입니다! 😅');
      return;
    }

    const jwtToken = localStorage.getItem('jwtToken');
    if (!jwtToken) {
      alert('로그인이 필요합니다!');
      return;
    }

    setIsSaving(true);

    try {
      const payload = {
        accountId: adSettings.accountId,
        pageId: adSettings.pageId,
        link: adSettings.link,
        billingEvent: adSettings.billingEvent,
        optimizationGoal: adSettings.optimizationGoal,
        bidStrategy: adSettings.bidStrategy,
        dailyBudget: adSettings.dailyBudget,
        startTime: adSettings.startTime,
      };

      const response = await axios.post(`${apiBase}/meta/create-ad`, payload, {
        headers: {
          Authorization: `Bearer ${jwtToken}`,
          'Content-Type': 'application/json',
        },
      });

      console.log('✅ 광고 생성 응답:', response.data);
      alert('🎉 광고가 성공적으로 생성되었습니다!');
      setAdCreatedOrUpdated(true);
    } catch (error) {
      console.error('❌ 광고 생성 실패:', error);
      const message =
        error.response?.data?.message ||
        error.response?.data ||
        '광고 생성 중 오류가 발생했습니다.';
      alert(message);
    } finally {
      setIsSaving(false);
    }
  };

  const canShowCreateAdButton =
    adSettings.link && adSettings.dailyBudget && adSettings.startTime;
  const buttonText = adCreatedOrUpdated ? '광고 업로드하기' : '광고 생성하기';

  const tdStyle = {
    border: '1px solid #ccc',
    padding: '8px',
    verticalAlign: 'top',
    fontWeight: 'normal',
    color: '#555',
  };
  const thStyle = {
    border: '1px solid #ccc',
    padding: '8px',
    backgroundColor: '#e0e0e0',
    textAlign: 'left',
    fontWeight: 'bold',
    color: '#333',
    width: '40%',
  };
  const labelStyle = {
    display: 'block',
    marginBottom: '5px',
    fontWeight: 'bold',
    color: '#444',
    fontSize: '0.95em',
  };
  const inputStyle = {
    width: '100%',
    padding: '10px 12px',
    border: '1px solid #ccc',
    borderRadius: '5px',
    fontSize: '1em',
    boxSizing: 'border-box',
  };

  return (
    <div
      style={{
        maxWidth: '600px',
        margin: '40px auto',
        padding: '25px',
        border: '1px solid #ddd',
        borderRadius: '10px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
        backgroundColor: '#fff',
        fontFamily: 'Arial, sans-serif',
      }}
    >
      <h2 style={{ color: '#333', textAlign: 'center', marginBottom: '30px' }}>
        📊 페이스북 광고 설정
      </h2>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
        {/* 광고 계정 선택 */}
        <div>
          <label style={labelStyle}>광고 계정 선택:</label>
          <select
            value={selectedAccount}
            onChange={handleAccountSelect}
            style={inputStyle}
          >
            <option value="">-- 선택 --</option>
            {adAccounts.map((acc) => (
              <option
                key={`${acc.accountId}_${acc.pageId}`}
                value={`${acc.accountId},${acc.pageId}`}
              >
                {acc.name} ({acc.accountId})
              </option>
            ))}
          </select>
        </div>

        {/* 랜딩 URL */}
        <div>
          <label style={labelStyle}>랜딩 URL (Link):</label>
          <input
            type="url"
            name="link"
            value={adSettings.link}
            onChange={handleChange}
            placeholder="https://example.com/your-landing"
            style={inputStyle}
          />
        </div>

        {/* 과금 기준 */}
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

        {/* 최적화 목표 */}
        <div>
          <label style={labelStyle}>최적화 목표 (Optimization Goal):</label>
          <select
            name="optimizationGoal"
            value={adSettings.optimizationGoal}
            onChange={handleChange}
            style={inputStyle}
          >
            <option value="LINK_CLICKS">링크 클릭</option>
            <option value="REACH">도달</option>
            <option value="CONVERSIONS">전환</option>
          </select>
        </div>

        {/* 입찰 방식 */}
        <div>
          <label style={labelStyle}>입찰 방식 (Bid Strategy):</label>
          <select
            name="bidStrategy"
            value={adSettings.bidStrategy}
            onChange={handleChange}
            style={inputStyle}
          >
            <option value="LOWEST_COST_WITHOUT_CAP">최저 비용</option>
            <option value="COST_CAP">비용 상한</option>
          </select>
        </div>

        {/* 하루 예산 */}
        <div>
          <label style={labelStyle}>하루 예산 (Daily Budget - 원):</label>
          <input
            type="number"
            name="dailyBudget"
            value={adSettings.dailyBudget}
            onChange={handleChange}
            placeholder="예: 15000"
            style={inputStyle}
          />
        </div>

        {/* 광고 시작 시간 */}
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

        {/* 버튼 */}
        {canShowCreateAdButton && (
          <button
            onClick={handleCreateAd}
            disabled={isSaving}
            style={{
              width: '100%',
              padding: '12px 20px',
              marginTop: '20px',
              backgroundColor: isSaving ? '#cccccc' : '#6f42c1',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              fontSize: '18px',
              fontWeight: 'bold',
              cursor: isSaving ? 'not-allowed' : 'pointer',
              transition: 'background-color 0.2s ease',
              boxShadow: '0 4px 8px rgba(111,66,193,0.2)',
            }}
            onMouseOver={(e) =>
              !isSaving && (e.currentTarget.style.backgroundColor = '#5a37a9')
            }
            onMouseOut={(e) =>
              !isSaving && (e.currentTarget.style.backgroundColor = '#6f42c1')
            }
          >
            {isSaving ? '메타 광고 생성 중…' : buttonText}
          </button>
        )}
      </div>

      {/* 미리보기 */}
      <div
        style={{
          marginTop: '40px',
          padding: '15px',
          backgroundColor: '#eef3f9',
          borderRadius: '8px',
        }}
      >
        <h3 style={{ color: '#444', marginBottom: '15px' }}>📋 현재 설정 미리보기</h3>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <tbody>
            <tr>
              <th style={thStyle}>광고 계정</th>
              <td style={tdStyle}>
                {selectedAccount
                  ? `${adSettings.accountId} / ${adSettings.pageId}`
                  : '-'}
              </td>
            </tr>
            <tr>
              <th style={thStyle}>랜딩 URL</th>
              <td style={tdStyle}>{adSettings.link || '-'}</td>
            </tr>
            <tr>
              <th style={thStyle}>과금 기준</th>
              <td style={tdStyle}>{adSettings.billingEvent}</td>
            </tr>
            <tr>
              <th style={thStyle}>최적화 목표</th>
              <td style={tdStyle}>{adSettings.optimizationGoal}</td>
            </tr>
            <tr>
              <th style={thStyle}>입찰 방식</th>
              <td style={tdStyle}>{adSettings.bidStrategy}</td>
            </tr>
            <tr>
              <th style={thStyle}>하루 예산</th>
              <td style={tdStyle}>{adSettings.dailyBudget} 원</td>
            </tr>
            <tr>
              <th style={thStyle}>광고 시작 시간</th>
              <td style={tdStyle}>{adSettings.startTime}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default FacebookInput;
