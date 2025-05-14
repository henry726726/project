import React from "react";
import { Route, Routes } from "react-router-dom";
import ChatGPTapi from "./components/ChatGPTapi";  // ChatGPTapi 컴포넌트를 임포트
import {Home} from "./components/HomePage";
import UserInform from "./components/userInform";

function App() {
  return (
    <div className="App">
      <header />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/ChatGPTapi" element={<ChatGPTapi />} />
        <Route path="/userInform" element={<UserInform />} />
      </Routes>
    </div>
  );
}

export default App;
