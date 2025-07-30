// src/components/LoginSignup/LoginSignup.jsx

import React, { useState } from 'react';
import './LoginSignup.css';
import { jwtDecode } from 'jwt-decode'; // 💡 수정: 중괄호 {}를 사용해서 임포트합니다!
import axios from 'axios'; // 💡 추가: axios 임포트 (fetch 대신 axios 사용하는 경우 대비)
import { useNavigate } from 'react-router-dom'; // 💡 추가: 로그인 성공 후 페이지 이동

import user_icon from '../Assets/person.png';
import email_icon from '../Assets/email.png';
import password_icon from '../Assets/password.png';
import ForgotPassword from './ForgotPassword';

const LoginSignup = ({ onLogin }) => {
  const navigate = useNavigate(); // 💡 useNavigate 훅 초기화
  const [action, setAction] = useState("Sign Up"); // 현재 폼의 액션 ('Sign Up' 또는 'Login')
  const [formData, setFormData] = useState({
    nickname: '',
    email: '',
    password: ''
  });
  const [showForgot, setShowForgot] = useState(false); // 비밀번호 찾기 폼 표시 여부
  const [loading, setLoading] = useState(false); // API 요청 로딩 상태
  const [error, setError] = useState(''); // 에러 메시지

  // 입력 필드 값 변경 핸들러
  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
    setError(''); // 입력 시 에러 메시지 초기화
  };

  // 폼 제출 핸들러
  const handleSubmit = async () => {
    setLoading(true);
    setError('');

    try {
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080';
      const endpoint = action === "Login" ? '/auth/login' : '/auth/signup';

      const requestData = action === "Login"
        ? { email: formData.email, password: formData.password }
        : { nickname: formData.nickname, email: formData.email, password: formData.password };

      // 💡 fetch 대신 axios 사용 권장 (편의성, 인터셉터 등)
      const response = await axios.post(`${apiUrl}${endpoint}`, requestData, {
        headers: {
          'Content-Type': 'application/json',
        }
      });
      
      const data = response.data; // axios는 응답 객체 안에 data 속성으로 JSON을 파싱해 줌

      if (action === "Login") {
        // 💡 로그인 성공 시 JWT 토큰 처리
        const token = data.token; // 백엔드 LoginResponse에서 'token' 필드를 받아옴
        if (token) {
          localStorage.setItem('jwtToken', token); // 로컬 스토리지에 토큰 저장
          const decoded = jwtDecode(token); // 토큰 디코딩
          const user = {
            email: decoded.sub, // 'sub' 필드에 이메일 저장된다고 가정
            roles: decoded.auth, // 'auth' 필드에 권한 저장된다고 가정
          };
          onLogin(user); // App.js로 사용자 정보 전달 및 로그인 상태 업데이트
          alert("로그인 성공! 환영합니다!"); // 성공 알림
          navigate('/'); // 로그인 성공 후 마이페이지로 이동
        } else {
          // 토큰이 없으면 로그인 실패로 간주
          throw new Error('로그인에 실패했습니다. 토큰을 받지 못했습니다.');
        }
      } else { // 'Sign Up' 액션
        alert("회원가입 성공! 로그인 해주세요.");
        setAction("Login"); // 회원가입 성공 후 로그인 폼으로 전환
        setFormData({ nickname: '', email: '', password: '' }); // 폼 데이터 초기화
        navigate('/auth/login'); // 로그인 페이지로 이동 (명시적)
      }

    } catch (err) {
      // 💡 에러 처리 개선: axios 에러 객체에서 정보 추출
      const errorMessage = err.response?.data?.message // 백엔드에서 보낸 에러 메시지
                         || err.message                 // 일반 네트워크 에러 또는 기타 메시지
                         || '알 수 없는 오류가 발생했습니다.';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // 비밀번호 찾기 폼 표시 여부 핸들러
  if (showForgot) {
    return <ForgotPassword onBackToLogin={() => setShowForgot(false)} />;
  }

  return (
    <div className='container'>
      <div className="header">
        <div className="text">{action}</div> {/* 'Sign Up' 또는 'Login' */}
        <div className="underline"></div>
      </div>

      {error && ( // 에러 메시지가 있을 경우 표시
        <div className="error-message" style={{
          color: '#ff6b6b',
          backgroundColor: 'rgba(255, 107, 107, 0.1)',
          padding: '10px',
          borderRadius: '8px',
          marginBottom: '20px',
          fontSize: '14px'
        }}>
          {error}
        </div>
      )}

      <div className="inputs">
        {/* 'Sign Up' 모드일 때만 닉네임 입력 필드 표시 */}
        {action !== "Login" && (
          <div className="input">
            <img src={user_icon} alt="User Icon" />
            <input
              type="text"
              placeholder='Nickname'
              value={formData.nickname}
              onChange={(e) => handleInputChange('nickname', e.target.value)}
              disabled={loading}
              required
            />
          </div>
        )}
        <div className="input">
          <img src={email_icon} alt="Email Icon" />
          <input
            type="email"
            placeholder='Email Id'
            value={formData.email}
            onChange={(e) => handleInputChange('email', e.target.value)}
            disabled={loading}
            required
          />
        </div>
        <div className="input">
          <img src={password_icon} alt="Password Icon" />
          <input
            type="password"
            placeholder='Password'
            value={formData.password}
            onChange={(e) => handleInputChange('password', e.target.value)}
            disabled={loading}
            required
          />
        </div>
      </div>

      {/* 'Login' 모드일 때만 비밀번호 찾기 옵션 표시 */}
      {action === "Login" && (
        <div className="forgot-password">
          Lost Password?<span onClick={() => setShowForgot(true)}> Click here!</span>
        </div>
      )}

      <div className="submit-container">
        {/* 'Sign Up' 버튼 */}
        <div className={action === "Login" ? "submit gray" : "submit"}
          onClick={() => {
            if (action === "Sign Up") { // 현재 액션이 'Sign Up'이면 제출 처리
              handleSubmit();
            } else { // 현재 액션이 'Login'이면 'Sign Up' 모드로 전환
              setAction("Sign Up");
              setError(''); // 폼 전환 시 에러 메시지 초기화
            }
          }}
          style={{ opacity: loading ? 0.7 : 1, cursor: loading ? 'not-allowed' : 'pointer' }}
        >
          {loading && action === "Sign Up" ? 'Processing...' : 'Sign Up'} {/* 로딩 상태에 따른 텍스트 */}
        </div>
        {/* 'Login' 버튼 */}
        <div className={action === "Sign Up" ? "submit gray" : "submit"}
          onClick={() => {
            if (action === "Login") { // 현재 액션이 'Login'이면 제출 처리
              handleSubmit();
            } else { // 현재 액션이 'Sign Up'이면 'Login' 모드로 전환
              setAction("Login");
              setError(''); // 폼 전환 시 에러 메시지 초기화
            }
          }}
          style={{ opacity: loading ? 0.7 : 1, cursor: loading ? 'not-allowed' : 'pointer' }}
        >
          {loading && action === "Login" ? 'Processing...' : 'Login'} {/* 로딩 상태에 따른 텍스트 */}
        </div>
      </div>
    </div>
  );
};

export default LoginSignup;