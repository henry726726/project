import React from 'react';

function ChatBubble({ message, isUser }) {
    return (
      <div style={{ textAlign: isUser ? 'right' : 'left', margin: '10px' }}>
        <div
          style={{
            display: 'inline-block',
            padding: '10px',
            borderRadius: '10px',
            backgroundColor: isUser ? '#add8e6' : '#ddd',
            maxWidth: '60%',
          }}
        >
          {message}
        </div>
      </div>
    );
  }
  
  export default ChatBubble;
  