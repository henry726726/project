// src/ErrorBoundary.jsx - 스타일만 변경된 코드 (SyntaxError 해결)

import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    console.error("ErrorBoundary caught an error", error, errorInfo);
    this.setState({
      error: error,
      errorInfo: errorInfo
    });
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          background: 'linear-gradient(135deg, #1a0f3d 0%, #3e1b6a 100%)', // ✅ 에러 페이지 배경
          color: '#e0e0ff', // ✅ 기본 텍스트 색상 밝게
          // 🔥🔥🔥🔥🔥 딱 여기만 수정했어! 🔥🔥🔥🔥🔥
          fontFamily: 'Arial, sans-serif', // ✅ 올바른 fontFamily 문법으로 수정!
          // 🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥
          padding: '20px',
          textAlign: 'center'
        }}>
          <h1 style={{
            fontSize: '3em',
            color: '#ff6b6b', // ✅ 에러 제목: 붉은색 계열로 강조
            marginBottom: '20px',
            textShadow: '0 0 10px rgba(255,107,107,0.5)' // ✅ 제목 그림자 효과
          }}>
            🚫 아 이런, 문제가 발생했어요!
          </h1>
          <p style={{
            fontSize: '1.2em',
            marginBottom: '30px',
            color: '#d1c4e9' // ✅ 설명 텍스트 색상: 부드러운 보라색 톤
          }}>
            죄송합니다. 페이지를 표시하는 중에 오류가 발생했습니다.<br/>
            잠시 후 다시 시도해주시거나 관리자에게 문의해주세요.
          </p>
          {this.state.error && (
            <details style={{
              marginTop: '20px',
              padding: '15px',
              backgroundColor: 'rgba(0,0,0,0.3)', // ✅ 상세 정보 배경: 반투명 어둡게
              border: '1px solid #7c4dff', // ✅ 테두리 색상: 밝은 보라색
              borderRadius: '8px',
              maxWidth: '80%',
              overflow: 'auto',
              textAlign: 'left',
              // color 속성은 제거된 상태 (최대한 보수적 접근)
            }}>
              <summary style={{
                cursor: 'pointer',
                fontWeight: 'bold',
                color: '#bb86fc' // ✅ 요약 텍스트 색상: 퍼플 톤
              }}>
                자세한 오류 정보 보기
              </summary>
              <pre style={{
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                fontSize: '0.9em',
                // color 속성은 제거된 상태 (최대한 보수적 접근)
              }}>
                {this.state.error && this.state.error.toString()}
                <br />
                {this.state.errorInfo && this.state.errorInfo.componentStack}
              </pre>
            </details>
          )}
          <button
            onClick={() => window.location.reload()}
            style={{
              marginTop: '40px',
              padding: '12px 25px',
              border: 'none',
              borderRadius: '25px',
              background: 'linear-gradient(45deg, #a8e6cf, #88d8a3)', // ✅ 버튼 배경: 밝은 그린 계열 그라디언트
              color: '#1a0f3d', // ✅ 버튼 텍스트 색상: 어두운 톤으로 대비
              fontSize: '1.1em',
              fontWeight: 'bold',
              cursor: 'pointer',
              transition: 'all 0.3s ease', // ✅ 기존 transition 유지
              boxShadow: '0 4px 15px rgba(168,230,207,0.4)' // ✅ 버튼 그림자 효과
            }}
          >
            새로고침
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;