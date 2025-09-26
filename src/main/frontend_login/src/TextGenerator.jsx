// src/TextGenerator.jsx

import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

function TextGenerator() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    product: "",
    target: "",
    purpose: "",
    keyword: "",
    duration: "",
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

    const formValues = Object.values(form);
    const isValid = formValues.every((value) => value.trim() !== "");
    if (!isValid) {
      setError("모든 필드를 입력해주세요! 😅");
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
      setAdTexts(res.data.adTexts || []);
    } catch (err) {
      console.error("❌ 광고 문구 생성 오류:", err);
      const errorMessage =
        err.response && err.response.status === 401
          ? "인증이 필요하거나 세션이 만료되었습니다. 다시 로그인해주세요."
          : err.response?.data?.message ||
            err.message ||
            "광고 문구 생성 중 오류가 발생했습니다. 백엔드 서버를 확인해주세요.";
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
    // ✅ ImageGenerator에서 읽을 키들 저장
    localStorage.setItem("selectedText", chosenText); // 새 기준
    localStorage.setItem("selectedAdText", chosenText); // 구버전 호환
    localStorage.setItem("Product", form.product || "");
    localStorage.setItem("textGenParams", JSON.stringify(form));

    navigate("/image-generator");
  };

  const inputStyle = {
    width: "100%",
    padding: 12,
    borderRadius: 10,
    border: "1px solid #ced4da",
    fontSize: "1.1em",
    backgroundColor: "#fff",
    boxShadow: "inset 0 1px 3px rgba(0,0,0,0.05)",
    outline: "none",
  };

  const buttonStyle = {
    width: "100%",
    padding: 15,
    backgroundColor: "#28a745",
    color: "white",
    border: "none",
    borderRadius: 10,
    fontSize: "1.2em",
    fontWeight: "bold",
    cursor: "pointer",
    boxShadow: "0 4px 10px rgba(40,167,69,0.2)",
  };

  const adTextButtonStyle = {
    display: "block",
    width: "100%",
    textAlign: "left",
    border: "1px solid #ced4da",
    borderRadius: 8,
    padding: 12,
    marginTop: 10,
    backgroundColor: "#fff",
    fontSize: "1.1em",
    cursor: "pointer",
    boxShadow: "0 2px 5px rgba(0,0,0,0.05)",
  };

  return (
    <div
      style={{
        maxWidth: 600,
        margin: "40px auto",
        padding: 30,
        backgroundColor: "rgba(255,255,255,0.9)",
        borderRadius: 15,
        boxShadow: "0 8px 20px rgba(0,0,0,0.08)",
        fontFamily: "Arial, sans-serif",
        color: "#343a40",
      }}
    >
      <h2
        style={{
          color: "#495057",
          textAlign: "center",
          marginBottom: 30,
          fontSize: "2em",
          fontWeight: 600,
        }}
      >
        광고 문구 생성기
      </h2>
      <form
        onSubmit={handleSubmit}
        style={{ display: "flex", flexDirection: "column", gap: 15 }}
      >
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
        <button type="submit" disabled={loading} style={buttonStyle}>
          {loading ? "문구 생성 중... ⏳" : "광고 문구 생성하기 "}
        </button>
      </form>

      {error && (
        <p style={{ color: "red", textAlign: "center", marginTop: 10 }}>
          {error}
        </p>
      )}

      {adTexts.length > 0 && (
        <div style={{ marginTop: 30 }}>
          <h3
            style={{
              color: "#495057",
              marginBottom: 15,
              fontSize: "1.3em",
              fontWeight: 600,
            }}
          >
            👇 문구를 선택하세요:
          </h3>
          {adTexts.map((t, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => handleSelectText(t)}
              style={adTextButtonStyle}
            >
              {t}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default TextGenerator;
