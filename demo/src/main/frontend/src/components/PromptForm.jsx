import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

export default function PromptForm() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    product: '',
    target: '',
    purpose: '',
    keyword: '',
    duration: '',
  });
  const [adTexts, setAdTexts] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setAdTexts([]);

    try {
      const res = await axios.post('http://localhost:8080/api/generate', form);
      setAdTexts(res.data.adTexts || []);
    } catch (err) {
      console.error('❌ GPT 호출 오류:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectText = async (selectedText) => {
    try {
      await axios.post('http://localhost:8080/userdatainput/content', {
        userId: 'sql_test',                    // ✅ DTO와 맞춤
        name: '',                              // 선택사항
        caption: selectedText,
        imageUrl: '',                          // 선택사항
        product: form.product,
        target: form.target,
        purpose: form.purpose,
        keyword: form.keyword,
        duration: form.duration,
      });

      // 다음 페이지로 이동하며 문구 전달
      navigate('/compose', { state: { selectedText } });
    } catch (err) {
      console.error('❌ 저장 또는 이동 오류:', err);
    }
  };

  return (
    <div className="max-w-xl mx-auto p-6 bg-white rounded shadow">
      <form onSubmit={handleSubmit} className="space-y-4">
        <input name="product" value={form.product} onChange={handleChange} placeholder="제품명" className="w-full p-2 border" />
        <input name="target" value={form.target} onChange={handleChange} placeholder="타겟 (예: 30대 여성)" className="w-full p-2 border" />
        <input name="purpose" value={form.purpose} onChange={handleChange} placeholder="목적 (예: 구매 유도)" className="w-full p-2 border" />
        <input name="keyword" value={form.keyword} onChange={handleChange} placeholder="강조 키워드 (예: 무방부제)" className="w-full p-2 border" />
        <input name="duration" value={form.duration} onChange={handleChange} placeholder="광고 기간 (예: 5일)" className="w-full p-2 border" />

        <button type="submit" className="w-full bg-blue-600 text-white p-2 rounded">
          {loading ? '문구 생성 중...' : '광고 문구 생성하기'}
        </button>
      </form>

      {adTexts.length > 0 && (
        <div className="mt-6">
          <h2 className="font-semibold">👇 문구를 선택하세요:</h2>
          {adTexts.map((text, idx) => (
            <button
              key={idx}
              onClick={() => handleSelectText(text)}
              className="block w-full text-left border rounded p-2 mt-2 hover:bg-gray-100"
            >
              {text}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
