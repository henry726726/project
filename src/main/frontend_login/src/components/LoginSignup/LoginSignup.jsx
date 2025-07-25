// src/components/LoginSignup/LoginSignup.jsx

import React, { useState } from 'react';
import './LoginSignup.css';
import { jwtDecode } from 'jwt-decode';


import user_icon from '../Assets/person.png';
import email_icon from '../Assets/email.png';
import password_icon from '../Assets/password.png';
import ForgotPassword from './ForgotPassword';

const LoginSignup = ({ onLogin }) => {
  const [action, setAction] = useState("Sign Up");
  const [formData, setFormData] = useState({
    nickname: '',
    email: '',
    password: ''
  });
  const [showForgot, setShowForgot] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
    setError('');
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError('');

    try {
      const apiUrl = process.env.REACT_APP_API_URL;
      const endpoint = action === "Login" ? '/auth/login' : '/auth/signup';

      const requestData = action === "Login"
        ? { email: formData.email, password: formData.password }
        : { nickname: formData.nickname, email: formData.email, password: formData.password };

      const response = await fetch(`${apiUrl}${endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || data.error || 'Something went wrong');
      }

      if (action === "Login") {
        const decoded = jwtDecode(data.token);
        const user = {
          email: decoded.sub,
          roles: decoded.auth,
        };
        onLogin(user); // 💡 전달!
      } else {
        alert("회원가입 성공! 로그인 해주세요.");
        setAction("Login");
      }

    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (showForgot) {
    return <ForgotPassword onBackToLogin={() => setShowForgot(false)} />;
  }

  return (
    <div className='container'>
      <div className="header">
        <div className="text">{action}</div>
        <div className="underline"></div>
      </div>

      {error && (
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
        {action !== "Login" && (
          <div className="input">
            <img src={user_icon} alt="" />
            <input
              type="text"
              placeholder='Nickname'
              value={formData.nickname}
              onChange={(e) => handleInputChange('nickname', e.target.value)}
              disabled={loading}
            />
          </div>
        )}
        <div className="input">
          <img src={email_icon} alt="" />
          <input
            type="email"
            placeholder='Email Id'
            value={formData.email}
            onChange={(e) => handleInputChange('email', e.target.value)}
            disabled={loading}
          />
        </div>
        <div className="input">
          <img src={password_icon} alt="" />
          <input
            type="password"
            placeholder='Password'
            value={formData.password}
            onChange={(e) => handleInputChange('password', e.target.value)}
            disabled={loading}
          />
        </div>
      </div>

      {action === "Login" && (
        <div className="forgot-password">
          Lost Password?<span onClick={() => setShowForgot(true)}> Click here!</span>
        </div>
      )}

      <div className="submit-container">
        <div className={action === "Login" ? "submit gray" : "submit"}
          onClick={() => {
            if (action === "Sign Up") {
              handleSubmit();
            } else {
              setAction("Sign Up");
            }
          }}
          style={{ opacity: loading ? 0.7 : 1, cursor: loading ? 'not-allowed' : 'pointer' }}
        >
          {loading ? 'Processing...' : 'Sign Up'}
        </div>
        <div className={action === "Sign Up" ? "submit gray" : "submit"}
          onClick={() => {
            if (action === "Login") {
              handleSubmit();
            } else {
              setAction("Login");
            }
          }}
          style={{ opacity: loading ? 0.7 : 1, cursor: loading ? 'not-allowed' : 'pointer' }}
        >
          {loading ? 'Processing...' : 'Login'}
        </div>
      </div>
    </div>
  );
};

export default LoginSignup;
