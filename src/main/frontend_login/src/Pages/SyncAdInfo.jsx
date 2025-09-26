import React, { useEffect, useState } from "react";
import axios from "axios";

function SyncAdInfo() {
  const [adAccounts, setAdAccounts] = useState([]);
  const [selectedAccount, setSelectedAccount] = useState("");
  const [message, setMessage] = useState("");

  const jwt = localStorage.getItem("jwtToken");
  const axiosConfig = {
    headers: {
      Authorization: `Bearer ${jwt}`,
    },
  };

  // 🔹 광고 계정 리스트 로드
  useEffect(() => {
    const fetchAdAccounts = async () => {
      try {
        const response = await axios.get(
          "http://localhost:8080/meta/adaccounts",
          axiosConfig
        );
        setAdAccounts(response.data);
      } catch (error) {
        console.error("광고 계정 불러오기 실패:", error);
        setMessage("❌ 광고 계정 불러오기 실패");
      }
    };

    fetchAdAccounts();
  }, []);

  // 🔹 광고 정보 동기화
  const handleSyncAds = async () => {
    if (!selectedAccount) {
      setMessage("❗ 광고 계정을 선택해주세요.");
      return;
    }

    // 👉 accountId,pageId 형식에서 분리
    const [accountId, pageId] = selectedAccount.split(",");

    try {
      const res = await axios.get(
        `http://localhost:8080/meta/sync-ads?adAccountId=${accountId}&accessToken=${jwt}`,
        axiosConfig
      );
      setMessage(res.data || "✅ 광고 정보 동기화 완료");
    } catch (error) {
      console.error("광고 정보 동기화 실패:", error);
      setMessage(
        "❌ 광고 정보 동기화 실패: " + (error.response?.data || error.message)
      );
    }
  };

  return (
    <div style={{ padding: "30px", maxWidth: "700px", margin: "0 auto" }}>
      <h2> 광고 정보 동기화</h2>

      <div style={{ marginBottom: "20px" }}>
        <label>📌 광고 계정 선택:</label>
        <select
          value={selectedAccount}
          onChange={(e) => setSelectedAccount(e.target.value)}
          style={{ marginLeft: "10px", padding: "5px" }}
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
        <button
          onClick={handleSyncAds}
          style={{ marginLeft: "15px", padding: "6px 12px" }}
        >
          광고 동기화
        </button>
      </div>

      {message && (
        <p
          style={{
            marginTop: "15px",
            color: message.startsWith("✅") ? "green" : "red",
          }}
        >
          {message}
        </p>
      )}
    </div>
  );
}

export default SyncAdInfo;
