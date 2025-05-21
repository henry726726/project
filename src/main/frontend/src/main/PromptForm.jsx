import React from 'react';
import { useState } from 'react';
import axios from 'axios';

export default function PromptForm() {
  const [product, setProduct] = useState('');
  const [style, setStyle] = useState('친근한');
  const [adTexts, setAdTexts] = useState([]);
  const [selectedText, setSelectedText] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [loading, setLoading] = useState(false);

  const handleGenerateTexts = async (e) => {
    e.preventDefault();
    setLoading(true);
    setAdTexts([]);
    setSelectedText('');
    setImageUrl('');

    try {
      const res = await axios.post('http://localhost:8080/api/generate', {
        product,
        style,
      });

      console.log('✅ GPT 응답:', res.data);
      setAdTexts(res.data.adTexts || []);
    } catch (err) {
      console.error('❌ GPT 호출 오류:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleImageGenerate = async (text) => {
  setSelectedText(text);
  setLoading(true);
  setImageUrl('');

  try {
    // 🔹 1. 이미지 생성 요청
    const res = await axios.post('http://localhost:8080/api/image', {
      product,
      text,
    });

    const imageUrl = res.data.imageUrl;
    setImageUrl(imageUrl);

    console.log('✅ DALL·E 응답:', res.data);

    // 🔹 2. DB 저장 요청
    await axios.post('http://localhost:8080/userdatainput/content', {
      userdatainputId: 'test-001', // 추후에 로그인 아이디 받아올 예정정
      caption: text,
      imageUrl: imageUrl,
    });

    console.log('✅ DB 저장 완료');

  } catch (err) {
    console.error('❌ 이미지 생성 또는 저장 오류:', err);
  } finally {
    setLoading(false);
  }
};

  return (
    <div className="max-w-xl mx-auto bg-white p-6 rounded shadow">
      <h1 className="text-2xl font-bold mb-4">광고 문구 & 이미지 생성기</h1>

      <form onSubmit={handleGenerateTexts} className="space-y-4">
        <input
          className="w-full border p-2"
          type="text"
          placeholder="상품명을 입력하세요"
          value={product}
          onChange={(e) => setProduct(e.target.value)}
        />
        <select
          className="w-full border p-2"
          value={style}
          onChange={(e) => setStyle(e.target.value)}
        >
          <option value="친근한">친근한</option>
          <option value="전문가적">전문가적</option>
          <option value="유쾌한">유쾌한</option>
          <option value="고급스러운">고급스러운</option>
          <option value="절제된">절제된</option>
        </select>
        <button
          type="submit"
          className="bg-blue-600 text-white px-4 py-2 rounded w-full"
          disabled={loading}
        >
          {loading ? '문구 생성 중...' : '광고 문구 생성하기'}
        </button>
      </form>

      {adTexts.length > 0 && (
        <div className="mt-6">
          <h2 className="font-semibold mb-2">👇 광고 문구를 선택하세요:</h2>
          {adTexts.map((text, idx) => (
            <button
              key={idx}
              onClick={() => handleImageGenerate(text)}
              className="block w-full border rounded p-2 mb-2 hover:bg-gray-100 text-left"
            >
              {text}
            </button>
          ))}
        </div>
      )}

      {selectedText && (
        <div className="mt-6">
          <p><strong>선택한 문구:</strong> {selectedText}</p>
        </div>
      )}

      {imageUrl && (
        <div className="mt-4">
          <img
            src={imageUrl}
            alt="생성된 광고 이미지"
            className="rounded border shadow-md max-w-full"
          />
        </div>
      )}
    </div>
  );
}
