import { useState } from 'react';
import ChatBubble from './ChatBubble';
import ChatInput from './ChatInput';
import ProgressBar from './ProgressBar';

function UserInform() {
  const [messages, setMessages] = useState([
    { text: 'Could you give me some explain your product?', isUser: false },
  ]);
  const [progress, setProgress] = useState(30);

  const handleUserInput = (input) => {
    const newMessages = [
      ...messages,
      { text: input, isUser: true },
      { text: "Great! I got your product,\nLet’s make our AD!", isUser: false }
    ];
    setMessages(newMessages);
    setProgress(100);
  };

  return (
    <div>
      <ProgressBar progress={30} />
      <div style={{ minHeight: '300px', padding: '10px' }}>
        {messages.map((m, i) => (
          <ChatBubble key={i} message={m.text} isUser={m.isUser} />
        ))}
      </div>
      <ChatInput onSend={handleUserInput} />
    </div>
  );
}

export default UserInform;



// import React, { useState, useEffect } from "react";
// import { Link } from "react-router-dom";

// function UserInform() {
//   const [productName, setProductName] = useState("");
//   const [productDescription, setProductDescription] = useState("");
//   const [productImage, setProductImage] = useState(null);
//   const [chatHistory, setChatHistory] = useState([]); 
//   const [questionIndex, setQuestionIndex] = useState(0);
//   const [loading, setLoading] = useState(false);

//   const questions = [
//     "상품 이름을 입력해 주세요.",
//     "상품 설명을 입력해 주세요.",
//     "상품 이미지를 업로드해 주세요.",
//   ];

//   // 질문에 따른 입력값 업데이트
//   const handleInputChange = (e) => {
//     if (questionIndex === 0) {
//       setProductName(e.target.value);
//     } else if (questionIndex === 1) {
//       setProductDescription(e.target.value);
//     }
//   };

//   // 답변 제출 후 다음 질문으로 이동
//   const handleSubmit = (e) => {
//     e.preventDefault();
//     if (questionIndex === 0) {
//       setChatHistory((prev) => [
//         ...prev,
//         { sender: "user", message: `상품 이름: ${productName}` },
//       ]);
//     } else if (questionIndex === 1) {
//       setChatHistory((prev) => [
//         ...prev,
//         { sender: "user", message: `상품 설명: ${productDescription}` },
//       ]);
//     } else if (questionIndex === 2) {
//       setChatHistory((prev) => [
//         ...prev,
//         { sender: "user", message: `상품 이미지: ${productImage ? productImage.name : "없음"}` },
//       ]);
//     }

//     // 질문 인덱스 업데이트 (다음 질문으로)
//     if (questionIndex < questions.length - 1) {
//       setQuestionIndex(questionIndex + 1);
//     } else {
//       setLoading(true);
//       setTimeout(() => {
//         setChatHistory((prev) => [
//           ...prev,
//           { sender: "system", message: "상품 정보가 저장되었습니다!" },
//         ]);
//         setLoading(false);
//       }, 2000);
//     }
//   };

//   // 질문을 채팅처럼 하나씩 출력하는 함수
//   useEffect(() => {
//     if (questionIndex < questions.length) {
//       setChatHistory((prev) => [
//         ...prev,
//         { sender: "system", message: questions[questionIndex] },
//       ]);
//     }
//   }, [questionIndex]);

//   return (
//     <div
//       style={{
//         display: "flex",
//         flexDirection: "column",
//         justifyContent: "flex-end",
//         alignItems: "center",
//         height: "100vh",
//         padding: "20px",
//         fontFamily: "Arial, sans-serif",
//         backgroundColor: "#f9f9f9",
//       }}
//     >
//       {/* 이 부분에 게이지바 넣을 예정<h2></h2> */}
//       <div
//         style={{
//           width: "100%",
//           maxWidth: "600px",
//           backgroundColor: "#fff",
//           borderRadius: "10px",
//           border: "1px solid #ccc",
//           padding: "20px",
//           height: "70vh",
//           overflowY: "auto",
//           display: "flex",
//           flexDirection: "column-reverse", // 최신 메시지가 아래로 오도록
//         }}
//       >
//         {chatHistory.map((chat, index) => (
//           <div
//             key={index}
//             style={{
//               alignSelf: chat.sender === "user" ? "flex-end" : "flex-start", // 오른쪽에 배치
//               backgroundColor: chat.sender === "user" ? "#e0f7fa" : "#f0f0f0",
//               borderRadius: "10px",
//               padding: "10px",
//               margin: "5px",
//               maxWidth: "80%",
//             }}
//           >
//             {chat.message}
//           </div>
//         ))}
//       </div>

//       <form onSubmit={handleSubmit} style={{ width: "100%", maxWidth: "600px", display: "flex", flexDirection: "column" }}>
//         {questionIndex === 0 && (
//           <input
//             type="text"
//             placeholder="상품 이름"
//             value={productName}
//             onChange={handleInputChange}
//             style={{
//               padding: "10px",
//               marginBottom: "10px",
//               borderRadius: "5px",
//               border: "1px solid #ccc",
//               fontSize: "16px",
//             }}
//           />
//         )}

//         {questionIndex === 1 && (
//           <textarea
//             placeholder="상품 설명"
//             value={productDescription}
//             onChange={handleInputChange}
//             style={{
//               padding: "10px",
//               marginBottom: "10px",
//               borderRadius: "5px",
//               border: "1px solid #ccc",
//               fontSize: "16px",
//               height: "100px",
//               resize: "none",
//             }}
//           />
//         )}

//         {questionIndex === 2 && (
//           <input
//             type="file"
//             onChange={(e) => setProductImage(e.target.files[0])}
//             style={{ marginBottom: "10px" }}
//           />
//         )}

//         <button
//           type="submit"
//           style={{
//             padding: "10px 20px",
//             backgroundColor: "#2093FF",
//             color: "white",
//             fontSize: "16px",
//             border: "none",
//             borderRadius: "5px",
//             cursor: "pointer",
//           }}
//           disabled={loading}
//         >
//           {loading ? "로딩 중..." : "다음"}
//         </button>
//       </form>

//       <Link
//         to="/"
//         style={{
//           marginTop: "20px",
//           color: "#007BFF",
//           fontSize: "16px",
//           textDecoration: "none",
//         }}
//       >
//         뒤로 가기
//       </Link>
//     </div>
//   );
// }

// export default UserInform;
