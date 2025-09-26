// src/Pages/SaveAdAccounts.jsx

import React, { useState } from "react";
import axios from "axios";

function SaveAdAccounts() {
  const [message, setMessage] = useState("");

  const handleSave = async () => {
    try {
      const jwt = localStorage.getItem("jwtToken");
      const response = await axios.get(
        "http://localhost:8080/meta/adaccounts/save",
        {
          headers: {
            Authorization: `Bearer ${jwt}`,
          },
        }
      );

      setMessage(response.data || "✅ 광고 계정 저장 완료");
    } catch (error) {
      console.error(error);
      setMessage(
        "❌ 광고 계정 저장 실패: " + (error.response?.data || error.message)
      );
    }
  };

  return (
    <div style={{ padding: "30px", maxWidth: "500px", margin: "0 auto" }}>
      <h2> 광고 계정 저장</h2>
      <p>버튼을 누르면 현재 로그인된 사용자 기준으로 광고 계정이 저장됩니다.</p>
      <button onClick={handleSave} style={{ padding: "10px 20px" }}>
        광고 계정 저장 실행
      </button>
      {message && <p style={{ marginTop: "15px" }}>{message}</p>}
    </div>
  );
}

export default SaveAdAccounts;
