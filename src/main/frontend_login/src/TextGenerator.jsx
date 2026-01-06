import React, { useState } from "react";
import axios from "axios";
import { useNavigate, Link } from "react-router-dom";

// ===================== Header (기존과 동일) =====================
function Header({ isLoggedIn, onLogout }) {
  const navLinkStyle = {
    color: "#374151",
    fontWeight: "500",
    fontSize: "15px",
    textDecoration: "none",
    padding: "8px 16px",
    borderRadius: "6px",
    transition: "all 0.2s ease",
    cursor: "pointer",
  };

  const logoutButtonStyle = {
    color: "#fff",
    backgroundColor: "#8B3DFF",
    border: "none",
    borderRadius: "6px",
    padding: "8px 20px",
    fontWeight: "700",
    fontSize: "15px",
    cursor: "pointer",
    boxShadow: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
    transition: "background-color 0.2s ease",
  };

  return (
    <header
      style={{
        backgroundColor: "#ffffff",
        padding: "12px 24px",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        borderBottom: "1px solid #f3f4f6",
        position: "sticky",
        top: 0,
        zIndex: 50,
      }}
    >
      <Link
        to="/"
        style={{
          fontFamily: "serif",
          fontStyle: "italic",
          fontWeight: "700",
          fontSize: "1.5rem",
          color: "#00C4CC",
          textDecoration: "none",
          cursor: "pointer",
          letterSpacing: "-0.025em",
        }}
      >
        ADaide
      </Link>

      <nav style={{ display: "flex", gap: 12, alignItems: "center" }}>
        <Link to="/mypage" style={navLinkStyle}>
          마이페이지
        </Link>
        {isLoggedIn ? (
          <button style={logoutButtonStyle} onClick={onLogout}>
            로그아웃
          </button>
        ) : (
          <Link to="/auth/login" style={navLinkStyle}>
            로그인
          </Link>
        )}
      </nav>
    </header>
  );
}

// ===================== Footer (기존과 동일) =====================
function Footer() {
  return (
    <footer
      style={{
        backgroundColor: "#ffffff",
        borderTop: "1px solid #f3f4f6",
        color: "#6b7280",
        fontSize: "0.875rem",
        padding: "48px 0",
        textAlign: "center",
        marginTop: "auto",
        fontFamily: "'Noto Sans KR', sans-serif",
      }}
    >
      <p style={{ marginBottom: "8px" }}>
        © 2025 AI Ad Manager. All rights reserved.
      </p>
      <p>대표: 장민서 | 대표 메일: msj3767@gmail.com</p>
    </footer>
  );
}

// ===================== TextGenerator 컴포넌트 =====================
function TextGenerator() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    product: "",
    benefit: "",
    painPoint: "",
    promotion: "",
    toneGuide: "",
  });

  const [adTexts, setAdTexts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (e) => {
    setForm((prevForm) => ({ ...prevForm, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setAdTexts([]);
    setError("");

    const requiredFields = ["product", "benefit", "painPoint"];
    const missing = requiredFields.filter((k) => !form[k]?.trim());
    if (missing.length > 0) {
      setError("필수 항목을 모두 입력해주세요! 😅");
      setLoading(false);
      return;
    }

    const token = localStorage.getItem("jwtToken");
    if (!token) {
      setError("로그인이 필요합니다. 다시 로그인해주세요!");
      navigate("/auth/login");
      setLoading(false);
      return;
    }

    try {
      const apiUrl = process.env.REACT_APP_API_URL || "http://localhost:8080";
      const res = await axios.post(`${apiUrl}/api/generate`, form, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      if (res.data?.ok === false) {
        setError(res.data?.warning || "광고 문구 생성에 실패했어요.");
        setAdTexts([]);
      } else {
        setAdTexts(res.data?.adTexts || []);
      }
    } catch (err) {
      console.error("❌ 광고 문구 생성 오류:", err);
      const errorMessage =
        err.response && err.response.status === 401
          ? "인증이 필요하거나 세션이 만료되었습니다. 다시 로그인해주세요."
          : err.response?.data?.message ||
            err.response?.data?.warning ||
            err.message ||
            "광고 문구 생성 중 오류가 발생했습니다.";
      setError(errorMessage);

      if (err.response?.status === 401 || err.response?.status === 403) {
        localStorage.removeItem("jwtToken");
        navigate("/auth/login");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSelectText = (chosenText) => {
    if (!chosenText) {
      setError("선택할 문구가 없습니다.");
      return;
    }
    localStorage.setItem("selectedText", chosenText);
    localStorage.setItem("selectedAdText", chosenText);
    localStorage.setItem("Product", form.product || "");
    localStorage.setItem("textGenParams", JSON.stringify(form));
    navigate("/image-generator");
  };

  // ================= 스타일 객체 (레이아웃 변경) =================
  const pageContainerStyle = {
    display: "flex",
    flexDirection: "column",
    minHeight: "100vh",
    backgroundColor: "#F9FAFB",
    fontFamily: "'Noto Sans KR', sans-serif",
  };

  const mainContentStyle = {
    flexGrow: 1,
    padding: "60px 20px",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
  };

  // ✅ 좌우 배치를 위한 Wrapper
  const contentWrapperStyle = {
    display: "flex",
    flexDirection: "row",
    gap: "30px",
    width: "100%",
    maxWidth: "1100px",
    justifyContent: "center",
    alignItems: "flex-start",
    flexWrap: "wrap", // 화면이 좁아지면 세로로 배치
  };

  // ✅ 공통 카드 스타일
  const cardBaseStyle = {
    backgroundColor: "#ffffff",
    borderRadius: "16px",
    boxShadow:
      "0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)",
    padding: "40px",
    display: "flex",
    flexDirection: "column",
    boxSizing: "border-box",
  };

  // 왼쪽 (입력) 패널
  const inputCardStyle = {
    ...cardBaseStyle,
    flex: "1 1 400px",
    maxWidth: "600px",
  };

  // 오른쪽 (결과) 패널
  const resultCardStyle = {
    ...cardBaseStyle,
    flex: "1 1 400px",
    maxWidth: "600px",
    backgroundColor: "#F3F4F6", // 결과창은 약간 다른 배경색으로 구분 (선택사항)
    border: "1px solid #E5E7EB",
  };

  const titleStyle = {
    fontSize: "1.75rem",
    fontWeight: "800",
    color: "#111827",
    marginBottom: "10px",
    textAlign: "center",
  };

  const subTextStyle = {
    fontSize: "0.95rem",
    color: "#6B7280",
    marginBottom: "30px",
    textAlign: "center",
  };

  const labelStyle = {
    display: "block",
    fontSize: "0.9rem",
    fontWeight: "600",
    color: "#374151",
    marginBottom: "6px",
  };

  const inputStyle = {
    width: "100%",
    padding: "12px",
    marginBottom: "16px",
    borderRadius: "8px",
    border: "1px solid #E5E7EB",
    backgroundColor: "#F9FAFB",
    color: "#1F2937",
    fontSize: "1rem",
    outline: "none",
    transition: "border-color 0.2s",
    boxSizing: "border-box",
  };

  const helperTextStyle = {
    fontSize: "0.82rem",
    color: "#6B7280",
    marginTop: "-10px",
    marginBottom: "14px",
    lineHeight: "1.4",
  };

  const buttonStyle = {
    width: "100%",
    padding: "14px",
    backgroundColor: loading ? "#E5E7EB" : "#8B3DFF",
    color: loading ? "#9CA3AF" : "white",
    border: "none",
    borderRadius: "8px",
    fontSize: "1.1rem",
    fontWeight: "700",
    cursor: loading ? "not-allowed" : "pointer",
    transition: "background-color 0.2s ease",
    marginTop: "10px",
  };

  const resultButtonStyle = {
    display: "block",
    width: "100%",
    textAlign: "left",
    border: "1px solid #d1d5db", // 좀 더 진한 테두리
    borderRadius: "8px",
    padding: "16px",
    marginTop: "12px",
    backgroundColor: "#ffffff",
    fontSize: "1rem",
    color: "#374151",
    cursor: "pointer",
    transition: "all 0.2s ease",
    lineHeight: "1.5",
    boxShadow: "0 1px 2px rgba(0,0,0,0.05)",
  };

  return (
    <div style={pageContainerStyle}>
      <Header
        isLoggedIn={Boolean(localStorage.getItem("jwtToken"))}
        onLogout={() => {
          localStorage.removeItem("jwtToken");
          navigate("/auth/login");
        }}
      />

      <main style={mainContentStyle}>
        <div style={contentWrapperStyle}>
          {/* ============ 왼쪽 패널: 입력 폼 ============ */}
          <div style={inputCardStyle}>
            <h2 style={titleStyle}>광고 문구 생성기</h2>
            <p style={subTextStyle}>
              AI가 “베네핏 + 상황” 기반으로 더 강한 광고 문구를 만듭니다.
            </p>

            <form onSubmit={handleSubmit}>
              <div>
                <label style={labelStyle}>제품명 (필수)</label>
                <input
                  name="product"
                  value={form.product}
                  onChange={handleChange}
                  placeholder="예: 럭셔리 시계"
                  style={inputStyle}
                />
              </div>

              <div>
                <label style={labelStyle}>핵심 베네핏 1줄 (필수)</label>
                <input
                  name="benefit"
                  value={form.benefit}
                  onChange={handleChange}
                  placeholder="예: 30분 걸리던 일을 5분으로"
                  style={inputStyle}
                />
                <div style={helperTextStyle}>
                  숫자/전후 비교가 있으면 문구가 확 살아나요.
                </div>
              </div>

              <div>
                <label style={labelStyle}>타겟 상황/고통 1줄 (필수)</label>
                <input
                  name="painPoint"
                  value={form.painPoint}
                  onChange={handleChange}
                  placeholder="예: 회의록 정리 때문에 매번 야근"
                  style={inputStyle}
                />
                <div style={helperTextStyle}>
                  “언제/왜 불편한지”가 구체적일수록 좋아요.
                </div>
              </div>

              <div>
                <label style={labelStyle}>프로모션/가격 (선택)</label>
                <input
                  name="promotion"
                  value={form.promotion}
                  onChange={handleChange}
                  placeholder="예: 오늘 자정까지 20% / 첫달 무료"
                  style={inputStyle}
                />
              </div>

              <div>
                <label style={labelStyle}>금지 표현/톤 가이드 (선택)</label>
                <input
                  name="toneGuide"
                  value={form.toneGuide}
                  onChange={handleChange}
                  placeholder="예: 존댓말 금지, 과장 금지, 직설 톤"
                  style={inputStyle}
                />
                <div style={helperTextStyle}>
                  브랜드 말투를 한 줄로 적어주면 결과가 안정돼요.
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                style={buttonStyle}
                onMouseOver={(e) => {
                  if (!loading)
                    e.currentTarget.style.backgroundColor = "#7C3AED";
                }}
                onMouseOut={(e) => {
                  if (!loading)
                    e.currentTarget.style.backgroundColor = "#8B3DFF";
                }}
              >
                {loading ? "AI 생성 중..." : "문구 생성하기"}
              </button>
            </form>

            {/* 에러 메시지는 입력창 아래에 둠 */}
            {error && (
              <div
                style={{
                  marginTop: "20px",
                  padding: "12px",
                  borderRadius: "8px",
                  backgroundColor: "#FEE2E2",
                  color: "#DC2626",
                  textAlign: "center",
                  fontWeight: "500",
                }}
              >
                {error}
              </div>
            )}
          </div>

          {/* ============ 오른쪽 패널: 결과 목록 (결과 있을 때만 보임) ============ */}
          {adTexts.length > 0 && (
            <div style={resultCardStyle}>
              <h3
                style={{
                  fontSize: "1.25rem",
                  fontWeight: "700",
                  color: "#111827",
                  marginBottom: "20px",
                  textAlign: "center",
                }}
              >
                🎉 생성된 카피라이팅
              </h3>
              <p
                style={{
                  textAlign: "center",
                  color: "#6B7280",
                  marginBottom: "20px",
                }}
              >
                마음에 드는 문구를 클릭하면
                <br />
                이미지 생성 단계로 이동합니다.
              </p>

              <div
                style={{
                  maxHeight: "500px", // 너무 길어지면 스크롤 생기도록
                  overflowY: "auto",
                  paddingRight: "5px",
                }}
              >
                {adTexts.map((text, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => handleSelectText(text)}
                    style={resultButtonStyle}
                    onMouseOver={(e) => {
                      e.currentTarget.style.backgroundColor = "#F9FAFB";
                      e.currentTarget.style.borderColor = "#8B3DFF";
                    }}
                    onMouseOut={(e) => {
                      e.currentTarget.style.backgroundColor = "#ffffff";
                      e.currentTarget.style.borderColor = "#d1d5db";
                    }}
                  >
                    {text}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </main>

      <Footer />
    </div>
  );
}

export default TextGenerator;