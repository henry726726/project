// src/components/ErrorBoundary.jsx

import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  // React 16부터 도입된 정적 메서드. 에러가 발생하면 호출되어 state를 업데이트
  static getDerivedStateFromError(error) {
    // 렌더링 폴백 UI가 보이도록 상태를 업데이트합니다.
    return { hasError: true };
  }

  // 에러와 에러 정보를 로깅하는 라이프사이클 메서드
  componentDidCatch(error, errorInfo) {
    // 여기에 에러 로깅 서비스(예: Sentry, Bugsnag)로 에러를 보낼 수 있습니다.
    console.error("Caught an error by ErrorBoundary:", error, errorInfo);
    this.setState({ error, errorInfo });
  }

  render() {
    if (this.state.hasError) {
      // 폴백(fallback) UI. 에러가 발생했을 때 보여줄 UI
      return (
        <div style={{ 
            padding: '20px', 
            margin: '50px auto', 
            maxWidth: '600px', 
            border: '1px solid #ffcc00', // 경고 색상
            borderRadius: '10px', 
            backgroundColor: '#fff8e1', // 연한 노란색 배경
            boxShadow: '0 4px 15px rgba(0,0,0,0.2)',
            textAlign: 'center'
        }}>
          <h2 style={{ color: '#e65100', marginBottom: '15px' }}>🚨 앗! 뭔가 잘못되었어요! 🚨</h2>
          <p style={{ fontSize: '1.1em', color: '#333' }}>죄송합니다. 앱을 표시하는 중에 오류가 발생했습니다.</p>
          <p style={{ fontSize: '0.9em', color: '#666' }}>잠시 후 다시 시도하시거나, 문제가 지속되면 개발자에게 문의해주세요.</p>
          
          {/* 개발 모드에서만 상세 에러 정보 표시 (프로덕션에서는 숨겨야 합니다) */}
          {process.env.NODE_ENV === 'development' && this.state.errorInfo && (
            <details style={{ whiteSpace: 'pre-wrap', marginTop: '20px', fontSize: '0.75em', color: '#555', textAlign: 'left' }}>
              <summary style={{ cursor: 'pointer', fontWeight: 'bold' }}>자세한 에러 정보 (개발 모드에서만 보임)</summary>
              <div style={{ backgroundColor: '#f0f0f0', padding: '10px', borderRadius: '5px', marginTop: '10px' }}>
                <p><strong>오류 메시지:</strong> {this.state.error && this.state.error.toString()}</p>
                <p><strong>컴포넌트 스택:</strong></p>
                <pre>{this.state.errorInfo.componentStack}</pre>
              </div>
            </details>
          )}
        </div>
      );
    }

    return this.props.children; // 에러가 없으면 자식 컴포넌트를 그대로 렌더링
  }
}

export default ErrorBoundary;