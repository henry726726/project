import React, { useState } from "react";

function App() {
  const [product, setProduct] = useState("");
  const [style, setStyle] = useState("");
  const [result, setResult] = useState("");

  const handleSubmit = async () => {
    const response = await fetch("http://localhost:8080/api/generate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ product, style }),
    });

    const data = await response.json();
    setResult(data.output);
  };

  return (
    <div style={{ padding: "2rem", fontFamily: "Arial" }}>
      <h2>GPT 광고 문구 생성기</h2>

      <input
        type="text"
        placeholder="제품명"
        value={product}
        onChange={(e) => setProduct(e.target.value)}
        style={{ width: "300px", marginBottom: "10px", display: "block" }}
      />

      <input
        type="text"
        placeholder="스타일 (예: 친근한)"
        value={style}
        onChange={(e) => setStyle(e.target.value)}
        style={{ width: "300px", marginBottom: "10px", display: "block" }}
      />

      <button onClick={handleSubmit}>문구 생성하기</button>

      <div style={{ marginTop: "2rem", whiteSpace: "pre-wrap" }}>
        <h4>생성된 문구:</h4>
        <div>{result}</div>
      </div>
    </div>
  );
}

export default App;
