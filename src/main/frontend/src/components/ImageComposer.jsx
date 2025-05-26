import React, { useState } from 'react';
import { useLocation } from 'react-router-dom';
import axios from 'axios';

export default function ImageComposer() {
  const { state } = useLocation();
  const selectedText = state?.selectedText;

  const [imageFile, setImageFile] = useState(null);
  const [resultUrl, setResultUrl] = useState(null);

  const handleFileChange = (e) => {
    setImageFile(e.target.files[0]);
  };

  const handleCompose = async () => {
    if (!imageFile || !selectedText) return;

    const form = new FormData();
    form.append('image', imageFile);
    form.append('text', selectedText);

    try {
      const res = await axios.post('http://localhost:8080/api/compose', form, {
        responseType: 'blob',
      });
      const blob = new Blob([res.data], { type: 'image/png' });
      setResultUrl(URL.createObjectURL(blob));
    } catch (err) {
      console.error('❌ 이미지 합성 오류:', err);
    }
  };

  return (
    <div className="max-w-xl mx-auto p-6 bg-white rounded shadow">
      <h1 className="text-xl font-bold mb-4">광고 이미지 합성기</h1>
      <p><strong>선택한 문구:</strong> {selectedText}</p>

      <input type="file" accept="image/*" onChange={handleFileChange} className="mt-4" />
      <button onClick={handleCompose} className="block w-full mt-4 bg-blue-600 text-white p-2 rounded">
        이미지 합성하기
      </button>

      {resultUrl && (
        <div className="mt-6">
          <img src={resultUrl} alt="합성 결과" className="rounded border" />
          <a href={resultUrl} download="composite.png" className="block mt-2 text-blue-500">이미지 다운로드</a>
        </div>
      )}
    </div>
  );
}
