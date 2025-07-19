// ImageComposer.jsx 파일
import React, { useState } from "react";
import { useLocation } from "react-router-dom";
import axios from "axios";

export default function ImageComposer() {
  const { state } = useLocation();
  const selectedText = state?.selectedText;

  const [imageFile, setImageFile] = useState(null);
  const [resultUrl, setResultUrl] = useState(null);
  const [generatedTime, setGeneratedTime] = useState(null);
  const [mode, setMode] = useState("auto"); // 기본 모드

  const handleFileChange = (e) => {
    setImageFile(e.target.files[0]);
  };

  const getTimestampString = () => {
    const now = new Date();
    const pad = (n) => n.toString().padStart(2, "0");
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(
      now.getDate()
    )}_${pad(now.getHours())}-${pad(now.getMinutes())}-${pad(
      now.getSeconds()
    )}`;
  };

  const handleCompose = async () => {
    if (!imageFile || !selectedText) return;

    const form = new FormData();
    form.append("image_file", imageFile);
    form.append("text", selectedText);
    form.append("layout", mode); // 선택된 모드 값 전송

    const url = "http://localhost:8000/add_text_to_image";

    try {
      setGeneratedTime(new Date());

      const res = await axios.post(url, form);
      const base64 = res.data?.image_base64;

      if (base64) {
        setResultUrl(`data:image/png;base64,${base64}`);
      } else {
        console.error("⚠️ 서버 응답에 image_base64가 없습니다:", res.data);
        setResultUrl(null);
      }
    } catch (err) {
      console.error("❌ 이미지 합성 오류:", err);
      setResultUrl(null);
    }
  };

  const formatKoreanTime = (date) =>
    `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일 ` +
    `${date.getHours()}시 ${date.getMinutes()}분 ${date.getSeconds()}초`;

  const downloadFileName = `composite_${getTimestampString()}.png`;

  return (
    <div className="max-w-xl mx-auto p-6 bg-white rounded shadow">
      <h1 className="text-xl font-bold mb-4">광고 이미지 합성기</h1>
      <p>
        <strong>선택한 문구:</strong> {selectedText}
      </p>

      <label className="block mt-4">
        <span className="mr-2 font-medium">레이아웃 방식:</span>
        <select
          value={mode}
          onChange={(e) => setMode(e.target.value)}
          className="border rounded p-1"
        >
          <option value="auto">자동 배치</option>
          <option value="box">상자 배치 (이미지 위에 겹침)</option>
          <option value="expanded-side-box">
            새로운 상자 (이미지 옆에 붙임)
          </option>{" "}
          {/* 이 부분 추가 */}
        </select>
      </label>

      <input
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        className="mt-4"
      />
      <button
        onClick={handleCompose}
        className="block w-full mt-4 bg-blue-600 text-white p-2 rounded"
      >
        이미지 합성하기
      </button>

      {resultUrl && (
        <div className="mt-6">
          <img src={resultUrl} alt="합성 결과" className="rounded border" />
          <a
            href={resultUrl}
            download={downloadFileName}
            className="block mt-2 text-blue-500"
          >
            {downloadFileName} 다운로드
          </a>
          {generatedTime && (
            <p className="mt-2 text-sm text-gray-500">
              생성된 시각: {formatKoreanTime(generatedTime)}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
