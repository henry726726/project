import React, { useState, useEffect, useRef, useCallback } from "react";
// import "./MyPage.css"; // 스타일은 아래 인라인 객체로 대체되었으므로 주석 처리 가능
import { Link, useNavigate } from "react-router-dom";
import user_icon from "../Assets/person.png"; // 경로가 맞는지 확인해주세요
import email_icon from "../Assets/email.png"; // 경로가 맞는지 확인해주세요

// ===================== 상수 =====================
const AUTO_LOGOUT_MINUTES = 90;
const AUTO_LOGOUT_MS = AUTO_LOGOUT_MINUTES * 60 * 1000;

// ===================== Header (통일된 밝은 테마) =====================
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

// ===================== Footer (통일된 밝은 테마) =====================
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
      }}
    >
      <p style={{ marginBottom: "8px" }}>
        © 2025 AI Ad Manager. All rights reserved.
      </p>
      <p>대표: 장민서 | 대표 메일: msj3767@gmail.com</p>
    </footer>
  );
}

// ===================== MyPage 컴포넌트 =====================
const MyPage = ({ userData }) => {
  const navigate = useNavigate();

  // 초기 상태 설정
  const [userInfo, setUserInfo] = useState(
    userData || {
      nickname: "",
      email: "",
      joinDate: "",
      bio: "",
    }
  );

  const [isEditing, setIsEditing] = useState(false);
  const [editInfo, setEditInfo] = useState(userInfo);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [remaining, setRemaining] = useState(AUTO_LOGOUT_MS);

  const timerRef = useRef();
  const lastActivityRef = useRef(Date.now());

  const apiUrl = process.env.REACT_APP_API_URL || "http://localhost:8080";

  // 1. 페이지 로드 시 사용자 정보 불러오기 (GET /user/me)
  // 수정 내용: userData prop 존재 여부와 상관없이 항상 최신 데이터를 서버에서 받아옵니다.
  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const jwtToken = localStorage.getItem("jwtToken");
        if (!jwtToken) return; // 비로그인 상태면 패스

        const response = await fetch(`${apiUrl}/user/me`, {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${jwtToken}`,
          },
        });

        if (response.ok) {
          const data = await response.json();
          // 서버에서 받아온 최신 데이터로 덮어쓰기
          setUserInfo(data);
          setEditInfo(data);
        } else {
          console.error("Failed to fetch fresh user info");
        }
      } catch (err) {
        console.error("Fetch user info error:", err);
      }
    };

    fetchUserInfo();
  }, [apiUrl]); // userData 의존성 제거

  // 2. 로그아웃 로직
  const handleLogout = useCallback(async () => {
    try {
      localStorage.removeItem("jwtToken");

      // 백엔드 로그아웃 엔드포인트 호출 (선택 사항)
      await fetch(`${apiUrl}/api/logout`, {
        method: "POST",
        credentials: "include",
      }).catch(() => {}); // 에러 무시

      alert("로그아웃 되었습니다.");
    } catch (error) {
      console.error("Logout error:", error);
    } finally {
      navigate("/auth/login");
      window.location.reload();
    }
  }, [navigate, apiUrl]);

  // 3. 자동 로그아웃 로직
  const handleAutoLogout = useCallback(async () => {
    try {
      localStorage.removeItem("jwtToken");
      await fetch(`${apiUrl}/api/logout`, {
        method: "POST",
        credentials: "include",
      }).catch(() => {});
    } catch (error) {
      console.error("Auto logout failed:", error);
    }
    alert("활동이 없어 자동 로그아웃되었습니다. 다시 로그인해주세요.");
    navigate("/auth/login");
    window.location.reload();
  }, [navigate, apiUrl]);

  // 4. Activity Detection (활동 감지)
  useEffect(() => {
    const resetTimer = () => {
      lastActivityRef.current = Date.now();
      setRemaining(AUTO_LOGOUT_MS);
    };
    const events = ["mousemove", "keydown", "mousedown", "touchstart"];
    events.forEach((event) => window.addEventListener(event, resetTimer));
    return () => {
      events.forEach((event) => window.removeEventListener(event, resetTimer));
    };
  }, []);

  // 5. Timer Countdown
  useEffect(() => {
    setRemaining(AUTO_LOGOUT_MS);
    timerRef.current = setInterval(() => {
      const elapsed = Date.now() - lastActivityRef.current;
      const timeLeft = AUTO_LOGOUT_MS - elapsed;
      setRemaining(timeLeft);
      if (timeLeft <= 0) {
        clearInterval(timerRef.current);
        handleAutoLogout();
      }
    }, 1000);
    return () => clearInterval(timerRef.current);
  }, [handleAutoLogout]);

  // ===================== 핸들러 함수들 =====================

  // 수정 모드 진입
  const handleEdit = () => {
    setIsEditing(true);
    setEditInfo(userInfo);
    setError("");
  };

  // 입력값 변경 핸들러 (누락된 부분 추가)
  const handleInputChange = (field, value) => {
    setEditInfo((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // 수정 취소 핸들러 (누락된 부분 추가)
  const handleCancel = () => {
    setEditInfo(userInfo);
    setIsEditing(false);
    setError("");
  };

  // [수정됨] 저장 핸들러 (PUT /user/me)
  const handleSave = async () => {
    setLoading(true);
    setError("");

    try {
      const jwtToken = localStorage.getItem("jwtToken");

      if (!jwtToken) {
        throw new Error("프로필을 업데이트하려면 로그인이 필요합니다.");
      }

      // 👇 [수정됨] 경로를 '/user/me'로 변경, 메서드 PUT 사용
      const response = await fetch(`${apiUrl}/user/me`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${jwtToken}`,
        },
        body: JSON.stringify({
          nickname: editInfo.nickname,
          email: editInfo.email, // 이메일 수정이 가능한지 여부는 백엔드 정책에 따름
          bio: editInfo.bio,
        }),
      });

      // 응답 처리
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || "프로필 업데이트 실패");
      }

      // 성공 메시지 받기 (필요시 데이터 활용)
      const data = await response.json();

      setUserInfo(editInfo); // 화면 정보 갱신
      setIsEditing(false); // 수정 모드 닫기
      alert("프로필이 성공적으로 업데이트되었습니다.");
    } catch (error) {
      console.error("Save error:", error);
      setError(error.message);
      if (error.message.includes("로그인") || error.message.includes("401")) {
        alert("세션이 만료되었거나 로그인이 필요합니다.");
        handleLogout();
      }
    } finally {
      setLoading(false);
    }
  };

  // PDF 생성 핸들러
  const handleGeneratePdf = async () => {
    setLoading(true);
    setError("");
    try {
      const jwtToken = localStorage.getItem("jwtToken");

      if (!jwtToken) {
        alert("PDF 리포트를 생성하려면 로그인이 필요합니다.");
        setLoading(false);
        return;
      }

      const response = await fetch(`${apiUrl}/api/report/pdf`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      if (!response.ok) {
        const errorText = await response.text();
        if (response.status === 401) {
          throw new Error("PDF 생성 실패: 세션이 만료되었습니다.");
        }
        throw new Error(`PDF 생성 실패: ${response.status} - ${errorText}`);
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      window.open(url, "_blank");
      alert("PDF 리포트가 성공적으로 생성되었습니다!");
    } catch (err) {
      console.error("PDF 생성 중 오류:", err);
      setError("PDF 생성 오류: " + err.message);
      if (err.message.includes("로그인") || err.response?.status === 401) {
        alert("세션이 만료되었습니다.");
        handleLogout();
      }
    } finally {
      setLoading(false);
    }
  };

  const isLoggedIn = Boolean(localStorage.getItem("jwtToken"));

  // ================= 스타일 객체 (밝은 테마 적용) =================
  const pageContainerStyle = {
    display: "flex",
    flexDirection: "column",
    minHeight: "100vh",
    backgroundColor: "#F9FAFB", // gray-50
    fontFamily: "'Noto Sans KR', sans-serif",
  };

  const mainContentStyle = {
    flexGrow: 1,
    padding: "40px 20px",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
  };

  const cardStyle = {
    width: "100%",
    maxWidth: "700px",
    backgroundColor: "#ffffff",
    borderRadius: "16px",
    boxShadow:
      "0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)",
    padding: "40px",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
  };

  const titleStyle = {
    fontSize: "1.75rem",
    fontWeight: "800",
    color: "#111827",
    marginBottom: "8px",
  };

  const labelStyle = {
    fontWeight: "700",
    color: "#374151",
    width: "80px",
    display: "inline-block",
  };

  const valueStyle = {
    color: "#4B5563",
    fontSize: "1rem",
  };

  const inputStyle = {
    flex: 1,
    padding: "10px 14px",
    borderRadius: "8px",
    border: "1px solid #E5E7EB",
    backgroundColor: "#F9FAFB",
    color: "#1F2937",
    fontSize: "1rem",
    outline: "none",
    transition: "border-color 0.2s",
  };

  const infoRowStyle = {
    display: "flex",
    alignItems: "center",
    width: "100%",
    marginBottom: "16px",
    padding: "12px",
    backgroundColor: "#fff",
    borderBottom: "1px solid #f3f4f6",
  };

  // 공통 버튼 스타일
  const btnBaseStyle = {
    padding: "10px 24px",
    borderRadius: "8px",
    fontWeight: "600",
    fontSize: "0.95rem",
    cursor: "pointer",
    border: "none",
    transition: "all 0.2s ease",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  };

  return (
    <div style={pageContainerStyle}>
      <Header isLoggedIn={isLoggedIn} onLogout={handleLogout} />

      <main style={mainContentStyle}>
        <div style={cardStyle}>
          {/* 타이틀 & 데코레이션 */}
          <div style={titleStyle}>내 프로필</div>
          <div
            style={{
              width: "40px",
              height: "4px",
              backgroundColor: "#8B3DFF",
              borderRadius: "2px",
              marginBottom: "16px",
            }}
          ></div>

          {error && (
            <div
              style={{
                color: "#DC2626",
                backgroundColor: "#FEE2E2",
                padding: "12px",
                borderRadius: "8px",
                width: "100%",
                marginBottom: "20px",
                fontSize: "14px",
                textAlign: "center",
                fontWeight: "500",
              }}
            >
              {error}
            </div>
          )}

          {/* 프로필 이미지 */}
          <div style={{ marginBottom: "30px", position: "relative" }}>
            <img
              src={user_icon}
              alt="Profile"
              style={{
                width: "100px",
                height: "100px",
                borderRadius: "50%",
                border: "3px solid #F3E8FF",
                padding: "2px",
                backgroundColor: "#fff",
              }}
            />
          </div>

          {/* 정보 표시/수정 영역 */}
          <div style={{ width: "100%", marginBottom: "20px" }}>
            {!isEditing ? (
              // [보기 모드]
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                }}
              >
                <div style={infoRowStyle}>
                  <img
                    src={user_icon}
                    alt=""
                    style={{ width: 20, opacity: 0.5, marginRight: 12 }}
                  />
                  <span style={labelStyle}>닉네임</span>
                  <span style={valueStyle}>{userInfo.nickname}</span>
                </div>
                <div style={infoRowStyle}>
                  <img
                    src={email_icon}
                    alt=""
                    style={{ width: 20, opacity: 0.5, marginRight: 12 }}
                  />
                  <span style={labelStyle}>이메일</span>
                  <span style={valueStyle}>{userInfo.email}</span>
                </div>
                <div style={infoRowStyle}>
                  <div style={{ width: 20, marginRight: 12 }}>📅</div>
                  <span style={labelStyle}>가입일</span>
                  <span style={valueStyle}>{userInfo.joinDate || "-"}</span>
                </div>
                <div
                  style={{
                    ...infoRowStyle,
                    alignItems: "flex-start",
                    borderBottom: "none",
                  }}
                >
                  <div style={{ width: 20, marginRight: 12, marginTop: 2 }}>
                    📝
                  </div>
                  <span style={labelStyle}>소개</span>
                  <p style={{ ...valueStyle, margin: 0, lineHeight: 1.6 }}>
                    {userInfo.bio || "자기소개가 없습니다."}
                  </p>
                </div>
              </div>
            ) : (
              // [수정 모드]
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "15px",
                }}
              >
                <div style={{ display: "flex", alignItems: "center" }}>
                  <span style={labelStyle}>닉네임</span>
                  <input
                    type="text"
                    name="nickname"
                    value={editInfo.nickname}
                    onChange={(e) =>
                      handleInputChange("nickname", e.target.value)
                    }
                    style={inputStyle}
                  />
                </div>
                <div style={{ display: "flex", alignItems: "center" }}>
                  <span style={labelStyle}>이메일</span>
                  <input
                    type="email"
                    name="email"
                    value={editInfo.email}
                    onChange={(e) => handleInputChange("email", e.target.value)}
                    style={inputStyle}
                  />
                </div>
                <div style={{ display: "flex", alignItems: "flex-start" }}>
                  <span style={{ ...labelStyle, paddingTop: "10px" }}>
                    소개
                  </span>
                  <textarea
                    name="bio"
                    value={editInfo.bio}
                    onChange={(e) => handleInputChange("bio", e.target.value)}
                    rows="4"
                    style={{ ...inputStyle, resize: "none" }}
                  />
                </div>
              </div>
            )}
          </div>

          {/* 버튼 그룹 */}
          <div
            style={{
              display: "flex",
              gap: "12px",
              flexWrap: "wrap",
              justifyContent: "center",
            }}
          >
            {!isEditing ? (
              <>
                <button
                  onClick={handleEdit}
                  style={{
                    ...btnBaseStyle,
                    backgroundColor: "#F3E8FF",
                    color: "#7E22CE",
                  }}
                  onMouseOver={(e) =>
                    (e.currentTarget.style.backgroundColor = "#E9D5FF")
                  }
                  onMouseOut={(e) =>
                    (e.currentTarget.style.backgroundColor = "#F3E8FF")
                  }
                >
                  프로필 수정
                </button>
                <button
                  onClick={handleGeneratePdf}
                  disabled={loading}
                  style={{
                    ...btnBaseStyle,
                    backgroundColor: loading ? "#E5E7EB" : "#10B981",
                    color: "white",
                    cursor: loading ? "not-allowed" : "pointer",
                  }}
                >
                  {loading ? "생성 중..." : "PDF 리포트"}
                </button>
                <button
                  onClick={handleLogout}
                  style={{
                    ...btnBaseStyle,
                    backgroundColor: "#EF4444",
                    color: "white",
                  }}
                >
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <button
                  onClick={handleSave}
                  disabled={loading}
                  style={{
                    ...btnBaseStyle,
                    backgroundColor: "#8B3DFF",
                    color: "white",
                    flex: 1,
                  }}
                >
                  {loading ? "저장 중..." : "저장하기"}
                </button>
                <button
                  onClick={handleCancel}
                  style={{
                    ...btnBaseStyle,
                    backgroundColor: "#F3F4F6",
                    color: "#4B5563",
                  }}
                >
                  취소
                </button>
              </>
            )}
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};

export default MyPage;