import React, { useState } from "react";
import axios from "axios";

function AccessTokenInput() {
  const [accessToken, setAccessToken] = useState("");
  const [message, setMessage] = useState("");

  const handleSubmit = async () => {
    try {
      const token = localStorage.getItem("jwtToken"); // ✅ 변수명 수정

      const response = await axios.post(
        "http://localhost:8080/api/access-token",
        { accessToken },
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      setMessage("✅ 액세스토큰 저장 완료");
    } catch (err) {
      console.error(err);
      setMessage("❌ 저장 실패: " + (err.response?.data || err.message));
    }
  };

  return (
    <div style={{ padding: "30px", maxWidth: "500px", margin: "0 auto" }}>
      <h2> 액세스토큰 입력</h2>
      <input
        type="text"
        value={accessToken}
        onChange={(e) => setAccessToken(e.target.value)}
        placeholder="Meta 엑세스토큰을 입력하세요"
        style={{ width: "100%", padding: "10px", marginBottom: "10px" }}
      />
      <button onClick={handleSubmit} style={{ padding: "10px 20px" }}>
        저장
      </button>
      {message && <p style={{ marginTop: "10px" }}>{message}</p>}
    </div>
  );
}

export default AccessTokenInput;
