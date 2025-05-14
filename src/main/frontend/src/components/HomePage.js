// components/HomePage.js
import React from "react";
import { Link } from "react-router-dom";

export function Home() {
  return (
    <div
     style={{
           display: "flex",  // Flexbox 사용
           flexDirection: "column",  // 세로 방향으로 정렬
           justifyContent: "center",  // 세로 중앙 정렬
           alignItems: "center",  // 가로 중앙 정렬
           height: "100vh",  // 화면 전체 높이를 사용
           textAlign: "center",  // 텍스트 중앙 정렬
           fontFamily: "Arial, sans-serif",  // 글꼴 설정  
     }}
    >
      <h1>Main page</h1>
      <Link to="/ChatGPTapi">
        <button
         style={{
            padding: "10px 20px",
            fontSize: "16px",
            cursor: "pointer",
            border: "none",
            backgroundColor:"#2093FF",
            color: "white",
            borderRadius: "5px",
            marginTop: "20px",
          }} 
        >
         Go to ChatGPT API Page</button>
      </Link>

      <Link to="/userInform">
      <button
         style={{
            padding: "10px 20px",
            fontSize: "16px",
            cursor: "pointer",
            border: "none",
            backgroundColor: "#2093FF",
            color: "white",
            borderRadius: "5px",
            marginTop: "20px",
          }} 
        >
         Go to UserInform Page</button>
      </Link>
    </div>
  );
}
