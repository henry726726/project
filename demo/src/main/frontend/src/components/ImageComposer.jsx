import React, { useState } from "react";
import { useLocation } from "react-router-dom";
import axios from "axios";

export default function ImageComposer() {
  const { state } = useLocation();
  const selectedText = state?.selectedText;

  const [imageFile, setImageFile] = useState(null);
  const [resultUrl, setResultUrl] = useState(null);
  const [generatedTime, setGeneratedTime] = useState(null);
  const [mode, setMode] = useState("controlnet"); // 기본값을 controlnet으로 지정

  const handleFileChange = (e) => {
    setImageFile(e.target.files[0]);
  };

  const getTimestampString = () => {
    const now = new Date();
    const pad = (n) => n.toString().padStart(2, "0");
    const yyyy = now.getFullYear();
    const MM = pad(now.getMonth() + 1);
    const dd = pad(now.getDate());
    const hh = pad(now.getHours());
    const mm = pad(now.getMinutes());
    const ss = pad(now.getSeconds());
    return `${yyyy}-${MM}-${dd}_${hh}-${mm}-${ss}`;
  };

  const handleCompose = async () => {
    if (!imageFile || !selectedText) return;

    const form = new FormData();
    form.append("image", imageFile);

    // Prompt 문자열의 백틱(`) 사용에 주의하세요.
    // 변수 ${selectedText}가 제대로 삽입되도록 템플릿 리터럴로 전체를 감쌌습니다.
    const prompt = `Add the text '${selectedText}' to the bottom in a clean, close-up high-resolution advertisement of a silver T&CO ring, on a Tiffany Blue gradient background with sparkles, professional softbox lighting, shallow depth of field, glossy metal, product photography style`;
    form.append("prompt", prompt);

    // 만약 백엔드에서 합성 방식(mode)을 사용한다면 이 줄을 추가하세요.
    // form.append('mode', mode);

    const url = "http://localhost:8000/generate";

    try {
      setGeneratedTime(new Date()); // 요청 시작 시각 기록

      const res = await axios.post(url, form);
      const base64 = res.data?.image_base64;

      if (base64) {
        setResultUrl(`data:image/jpeg;base64,${base64}`);
      } else {
        console.error("⚠️ 서버 응답에 image_base64가 없습니다:", res.data);
        setResultUrl(null);
      }
    } catch (err) {
      console.error("❌ 이미지 합성 오류:", err);
      setResultUrl(null);
    }
  };

  const formatKoreanTime = (date) => {
    return (
      `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일 ` +
      `${date.getHours()}시 ${date.getMinutes()}분 ${date.getSeconds()}초`
    );
  };

  // 다운로드 파일 이름에 타임스탬프를 포함
  const downloadFileName = `composite_${getTimestampString()}.png`;

  return (
    <div className="max-w-xl mx-auto p-6 bg-white rounded shadow">
      <h1 className="text-xl font-bold mb-4">광고 이미지 합성기</h1>
      <p>
        <strong>선택한 문구:</strong> {selectedText}
      </p>

      <label className="block mt-4">
        <span className="mr-2 font-medium">합성 방식:</span>
        <select
          value={mode}
          onChange={(e) => setMode(e.target.value)}
          className="border rounded p-1"
        >
          <option value="controlnet">AI 스타일 합성 (ControlNet)</option>
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
