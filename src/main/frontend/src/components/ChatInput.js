import React, { useState } from 'react';

function ChatInput({ onSend }) {
  const [text, setText] = useState('');

  const handleSend = () => {
    if (text.trim()) {
      onSend(text);
      setText('');
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault(); // 폼 제출 막기
      handleSend();
    }
  };

  return (
    <div style={{ padding: '10px', backgroundColor: '#ddd' }}>
      <input
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="입력 후 Enter를 눌러보세요"
        style={{ width: '80%', padding: '8px' }}
      />
    </div>
  );
}

export default ChatInput;
