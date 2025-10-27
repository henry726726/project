// src/FacebookInput.jsx

import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate, Link } from "react-router-dom";

// ===================== 스타일 객체 (Header/Footer에서 사용) =====================
const navLinkStyle = {
  color: "#a8a5f1",
  fontWeight: "600",
  textDecoration: "none",
  padding: "6px 12px",
  borderRadius: 6,
  backgroundColor: "rgba(255,255,255,0.1)",
  transition: "background-color 0.3s ease",
  cursor: "pointer",
};

const logoutButtonStyle = {
  color: "#fff",
  backgroundColor: "#ff6536",
  border: "none",
  borderRadius: 6,
  padding: "6px 12px",
  fontWeight: "600",
  cursor: "pointer",
};

// ===================== Header 컴포넌트 (FacebookInput.jsx 내부에 정의) =====================
// Header는 다른 파일로 분리하고 FacebookInput에서는 import하여 사용하는 것이 일반적인 구조입니다.
// 그러나 사용자의 요청에 따라 이 파일 내에 함께 정의했습니다.
function Header({ isLoggedIn, onLogout }) {
  return (
    <header style={{
      backgroundColor: "#3a2a60",
      padding: "12px 24px",
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      color: "#a8a5f1",
      fontFamily: "'Noto Sans KR', sans-serif",
      boxShadow: "0 2px 8px rgba(0,0,0,0.5)"
    }}>
      <Link to="/" style={{
        fontWeight: "700",
        fontSize: "1.5rem",
        color: "#A8E6CF",
        textDecoration: "none",
        cursor: "pointer"
      }}>
        Ad Manager
      </Link>

      <nav style={{ display: "flex", gap: 12 }}>
        <Link to="/mypage" style={navLinkStyle}>마이페이지</Link>
        {isLoggedIn ? (
          <button style={logoutButtonStyle} onClick={onLogout}>로그아웃</button>
        ) : (
          <Link to="/auth/login" style={navLinkStyle}>로그인</Link>
        )}
      </nav>
    </header>
  );
}

// ===================== Footer 컴포넌트 (FacebookInput.jsx 내부에 정의) =====================
function Footer() {
  return (
    <footer style={{
      backgroundColor: '#6243a5',
      color: '#cfcce2',
      fontSize: '0.9rem',
      padding: '15px 0',
      textAlign: 'center',
      fontFamily: "'Noto Sans KR', sans-serif",
      boxShadow: 'inset 0 1px 4px rgba(255,255,255,0.15)',
      marginTop: 'auto'
    }}>
      <p>© 2025 광고 매니저. All rights reserved.</p>
      <p>연락처: support@admanager.com</p>
    </footer>
  );
}


// ===================== FacebookInput 컴포넌트 =====================
function FacebookInput() {
  const navigate = useNavigate();
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

  useEffect(() => {
    const jwtToken = localStorage.getItem('jwtToken');
    if (!jwtToken) return;

    axios
      .get(`${apiBase}/meta/adaccounts`, {
        headers: { Authorization: `Bearer ${jwtToken}` },
      })
      .then((res) => setAdAccounts(res.data))
      .catch((err) => console.error('광고 계정 불러오기 실패:', err));
  }, [apiBase]); // apiBase가 useEffect 내부에서 사용되므로 의존성 배열에 추가

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

      // eslint-disable-next-line no-unused-vars
      const response = await axios.post(`${apiBase}/meta/create-ad`, payload, {
        headers: {
          Authorization: `Bearer ${jwtToken}`,
          'Content-Type': 'application/json',
        },
      });

      alert('🎉 광고가 성공적으로 생성되었습니다!');
      setAdCreatedOrUpdated(true);
    } catch (error) {
      const message =
        error.response?.data?.message ||
        '광고 생성 중 오류가 발생했습니다.';
      alert(message);
    } finally {
      setIsSaving(false);
    }
  };

  const canShowCreateAdButton =
    adSettings.link && adSettings.dailyBudget && adSettings.startTime;
  const buttonText = adCreatedOrUpdated ? '광고 업로드하기' : '광고 생성하기';

  // 사진 스타일에 맞춰진 인라인 스타일들
  const styles = {
    container: {
      maxWidth: 600,
      margin: '40px auto',
      padding: 24,
      borderRadius: 20,
      backgroundColor: '#140d2e',
      color: '#d1c4e9',
      fontFamily: "'Noto Sans KR', sans-serif",
      boxShadow: '0 0 30px rgba(124, 80, 244, 0.7)',
      userSelect: 'none',
    },
    title: {
      fontSize: 32,
      fontWeight: '800',
      textAlign: 'center',
      marginBottom: 24,
      color: '#b4acf8',
      textShadow: '0 0 10px rgba(124, 80, 244, 0.9)',
    },
    label: {
      fontWeight: 600,
      fontSize: 16,
      marginBottom: 8,
      display: 'block',
      color: '#8274bc',
      textShadow: '0 0 3px rgba(124, 80, 244, 0.5)',
    },
    input: {
      width: '100%',
      padding: 12,
      fontSize: 16,
      borderRadius: 14,
      border: 'none',
      outline: 'none',
      backgroundColor: 'rgba(55, 43, 94, 0.7)',
      color: '#ddd',
      marginBottom: 20,
      boxShadow: 'inset 0 0 8px rgba(0, 0, 0, 0.7)',
      transition: 'background-color 0.3s ease',
    },
    select: {
      width: '100%',
      padding: 12,
      fontSize: 16,
      borderRadius: 14,
      border: 'none',
      outline: 'none',
      backgroundColor: 'rgba(55, 43, 94, 0.7)',
      color: '#ddd',
      marginBottom: 20,
      boxShadow: 'inset 0 0 8px rgba(0, 0, 0, 0.7)',
      transition: 'background-color 0.3s ease',
    },
    button: {
      width: '100%',
      padding: 16,
      fontSize: 18,
      fontWeight: 700,
      borderRadius: 24,
      border: 'none',
      cursor: 'pointer',
      backgroundImage: 'linear-gradient(45deg, #bb86fc, #a06dfb)',
      color: 'white',
      boxShadow: '0 0 20px rgba(187,134,252,0.9)',
      transition: 'background-image 0.3s ease, box-shadow 0.3s ease',
      userSelect: 'none',
    },
    buttonDisabled: {
      opacity: 0.6,
      cursor: 'not-allowed',
      boxShadow: 'none',
      backgroundImage: 'none',
      backgroundColor: '#555',
    },
    textarea: { // 현재 이 컴포넌트에서는 textarea가 사용되지 않지만, 정의되어 있으므로 유지
      width: '100%',
      height: 100,
      padding: 12,
      fontSize: 16,
      borderRadius: 14,
      border: 'none',
      outline: 'none',
      resize: 'none',
      backgroundColor: 'rgba(55, 43, 94, 0.7)',
      color: '#ddd',
      marginBottom: 20,
      boxShadow: 'inset 0 0 8px rgba(0, 0, 0, 0.7)',
      transition: 'background-color 0.3s ease',
    },
    previewContainer: {
      backgroundColor: 'rgba(38, 31, 67, 0.8)',
      borderRadius: 20,
      padding: 20,
      marginTop: 30,
      color: '#bbb',
      minHeight: 200,
      boxShadow: '0 0 12px rgba(187, 134, 252, 0.7)',
    },
  };

  // Header에 전달할 onLogout 함수 정의
  const handleHeaderLogout = () => {
    localStorage.removeItem("jwtToken");
    navigate("/auth/login");
  };

  // 현재 로그인 상태 (Header 컴포넌트에 전달하기 위함)
  const isLoggedIn = Boolean(localStorage.getItem('jwtToken'));


  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* 헤더 */}
      <Header isLoggedIn={isLoggedIn} onLogout={handleHeaderLogout} />

      {/* 메인 콘텐츠 영역 (남은 공간을 차지하여 푸터를 아래로 밀어냄) */}
      <div style={{ flexGrow: 1, ...styles.container, margin: '40px auto' }}> {/* 기존 styles.container에 flexGrow:1을 추가 */}
        <h2 style={styles.title}>📊 페이스북 광고 설정</h2>

        <label style={styles.label}>광고 계정 선택:</label>
        <select
          value={selectedAccount}
          onChange={handleAccountSelect}
          style={styles.select}
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

        <label style={styles.label}>랜딩 URL (Link):</label>
        <input
          type="url"
          name="link"
          value={adSettings.link}
          onChange={handleChange}
          placeholder="https://example.com/your-landing"
          style={styles.input}
        />

        <label style={styles.label}>과금 기준 (Billing Event):</label>
        <select
          name="billingEvent"
          value={adSettings.billingEvent}
          onChange={handleChange}
          style={styles.select}
        >
          <option value="IMPRESSIONS">노출 (IMPRESSIONS)</option>
        </select>

        <label style={styles.label}>최적화 목표 (Optimization Goal):</label>
        <select
          name="optimizationGoal"
          value={adSettings.optimizationGoal}
          onChange={handleChange}
          style={styles.select}
        >
          <option value="LINK_CLICKS">링크 클릭</option>
        </select>

        <label style={styles.label}>입찰 방식 (Bid Strategy):</label>
        <select
          name="bidStrategy"
          value={adSettings.bidStrategy}
          onChange={handleChange}
          style={styles.select}
        >
          <option value="LOWEST_COST_WITHOUT_CAP">최저 비용</option>
        </select>

        <label style={styles.label}>하루 예산 (Daily Budget - 원):</label>
        <input
          type="number"
          name="dailyBudget"
          value={adSettings.dailyBudget}
          onChange={handleChange}
          placeholder="예: 15000"
          style={styles.input}
        />

        <label style={styles.label}>광고 시작 시간 (Start Time):</label>
        <input
          type="datetime-local"
          name="startTime"
          value={adSettings.startTime}
          onChange={handleChange}
          style={styles.input}
        />

        {canShowCreateAdButton && (
          <button
            onClick={handleCreateAd}
            disabled={isSaving}
            style={{
              ...styles.button,
              ...(isSaving ? styles.buttonDisabled : {}),
            }}
          >
            {isSaving ? '메타 광고 생성 중…' : buttonText}
          </button>
        )}

        <div style={styles.previewContainer}>
          <h3>📋 현재 설정 미리보기</h3>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              <tr>
                <th
                  style={{
                    textAlign: 'left',
                    padding: 8,
                    backgroundColor: '#2b2452',
                    borderBottom: '1px solid #665c9f',
                    width: '40%',
                    color: '#b9afff',
                  }}
                >
                  광고 계정
                </th>
                <td
                  style={{
                    padding: 8,
                    borderBottom: '1px solid #665c9f',
                    color: '#ccc',
                  }}
                >
                  {selectedAccount
                    ? `${adSettings.accountId} / ${adSettings.pageId}`
                    : '-'}
                </td>
              </tr>
              <tr>
                <th
                  style={{
                    textAlign: 'left',
                    padding: 8,
                    backgroundColor: '#2b2452',
                    borderBottom: '1px solid #665c9f',
                    color: '#b9afff',
                  }}
                >
                  랜딩 URL
                </th>
                <td
                  style={{
                    padding: 8,
                    borderBottom: '1px solid #665c9f',
                    color: '#ccc',
                  }}
                >
                  {adSettings.link || '-'}
                </td>
              </tr>
              <tr>
                <th
                  style={{
                    textAlign: 'left',
                    padding: 8,
                    backgroundColor: '#2b2452',
                    borderBottom: '1px solid #665c9f',
                    color: '#b9afff',
                  }}
                >
                  과금 기준
                </th>
                <td
                  style={{
                    padding: 8,
                    borderBottom: '1px solid #665c9f',
                    color: '#ccc',
                  }}
                >
                  {adSettings.billingEvent}
                </td>
              </tr>
              <tr>
                <th
                  style={{
                    textAlign: 'left',
                    padding: 8,
                    backgroundColor: '#2b2452',
                    borderBottom: '1px solid #665c9f',
                    color: '#b9afff',
                  }}
                >
                  최적화 목표
                </th>
                <td
                  style={{
                    padding: 8,
                    borderBottom: '1px solid #665c9f',
                    color: '#ccc',
                  }}
                >
                  {adSettings.optimizationGoal}
                </td>
              </tr>
              <tr>
                <th
                  style={{
                    textAlign: 'left',
                    padding: 8,
                    backgroundColor: '#2b2452',
                    borderBottom: '1px solid #665c9f',
                    color: '#b9afff',
                  }}
                >
                  입찰 방식
                </th>
                <td
                  style={{
                    padding: 8,
                    borderBottom: '1px solid #665c9f',
                    color: '#ccc',
                  }}
                >
                  {adSettings.bidStrategy}
                </td>
              </tr>
              <tr>
                <th
                  style={{
                    textAlign: 'left',
                    padding: 8,
                    backgroundColor: '#2b2452',
                    borderBottom: '1px solid #665c9f',
                    color: '#b9afff',
                  }}
                >
                  하루 예산
                </th>
                <td
                  style={{
                    padding: 8,
                    borderBottom: '1px solid #665c9f',
                    color: '#ccc',
                  }}
                >
                  {adSettings.dailyBudget} 원
                </td>
              </tr>
              <tr>
                <th
                  style={{
                    textAlign: 'left',
                    padding: 8,
                    backgroundColor: '#2b2452',
                    borderBottom: '1px solid #665c9f',
                    color: '#b9afff',
                  }}
                >
                  광고 시작 시간
                </th>
                <td
                  style={{
                    padding: 8,
                    borderBottom: '1px solid #665c9f',
                    color: '#ccc',
                  }}
                >
                  {adSettings.startTime}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      {/* 푸터 */}
      <Footer />
    </div>
  );
}

export default FacebookInput;