import React, { useState, useEffect, useRef } from 'react'
import './MyPage.css'

import user_icon from '../Assets/person.png'
import email_icon from '../Assets/email.png'
import edit_icon from '../Assets/password.png' // Using password icon as edit icon for now

const AUTO_LOGOUT_MINUTES = 90;
const AUTO_LOGOUT_MS = AUTO_LOGOUT_MINUTES * 60 * 1000;

const MyPage = ({ userData, onLogout }) => {
    const [userInfo, setUserInfo] = useState(userData || {
        nickname: 'User',
        email: 'user@example.com'
    });

    const [isEditing, setIsEditing] = useState(false);
    const [editInfo, setEditInfo] = useState(userInfo);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [remaining, setRemaining] = useState(AUTO_LOGOUT_MS);
    const timerRef = useRef();
    const lastActivityRef = useRef(Date.now());

    // Reset timer on user activity
    useEffect(() => {
        const resetTimer = () => {
            lastActivityRef.current = Date.now();
            setRemaining(AUTO_LOGOUT_MS);
        };
        const events = ['mousemove', 'keydown', 'mousedown', 'touchstart'];
        events.forEach(event => window.addEventListener(event, resetTimer));
        return () => {
            events.forEach(event => window.removeEventListener(event, resetTimer));
        };
    }, []);

    // Countdown timer
    useEffect(() => {
        timerRef.current = setInterval(() => {
            const elapsed = Date.now() - lastActivityRef.current;
            const timeLeft = AUTO_LOGOUT_MS - elapsed;
            setRemaining(timeLeft);
            if (timeLeft <= 0) {
                clearInterval(timerRef.current);
                handleAutoLogout();
            }
        }, 1000);
        return () => clearInterval(timerRef.current);
    }, []);

    const handleAutoLogout = async () => {
        try {
            const apiUrl = process.env.REACT_APP_API_URL;
            await fetch(`${apiUrl}/api/logout`, {
                method: 'POST',
                credentials: 'include'
            });
        } catch (error) {
            // ignore
        }
        onLogout();
        alert('You have been logged out due to inactivity.');
        window.location.reload();
    };

    const handleEdit = () => {
        setIsEditing(true);
        setEditInfo(userInfo);
        setError('');
    };

    const handleSave = async () => {
        setLoading(true);
        setError('');

        try {
            const apiUrl = process.env.REACT_APP_API_URL;
            const response = await fetch(`${apiUrl}/api/profile`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({
                    nickname: editInfo.nickname,
                    email: editInfo.email,
                    bio: editInfo.bio
                })
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || 'Something went wrong');
            }

            setUserInfo(editInfo);
            setIsEditing(false);
        } catch (error) {
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = () => {
        setIsEditing(false);
        setEditInfo(userInfo);
        setError('');
    };

    const handleInputChange = (field, value) => {
        setEditInfo(prev => ({
            ...prev,
            [field]: value
        }));
    };

    const handleLogout = async () => {
        try {
            const apiUrl = process.env.REACT_APP_API_URL;
            await fetch(`${apiUrl}/api/logout`, {
                method: 'POST',
                credentials: 'include'
            });
            onLogout();
            window.location.reload(); // Force redirect to login page after logout
        } catch (error) {
            console.error('Logout error:', error);
            onLogout();
            window.location.reload();
        }
    };

    // 🔴 PDF 리포트 생성 버튼 클릭 이벤트 핸들러 추가!
    const handleGeneratePdf = async () => {
        setLoading(true);
        setError('');
        try {
            const apiUrl = process.env.REACT_APP_API_URL || '';
            const response = await fetch(`${apiUrl}/api/report/pdf`, {
                method: 'GET',
                credentials: 'include', // 인증이 필요하면 유지
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`PDF 생성 실패: ${response.status} - ${errorText}`);
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);

            // 새 탭에서 PDF 열기 (기본 동작)
            window.open(url, '_blank');

            // 만약 바로 다운로드하고 싶으면 아래 주석 해제 (새 탭에서 열리지 않고 바로 다운로드)
            /*
            const link = document.createElement('a');
            link.href = url;
            link.download = 'Ad_Performance_Report.pdf';
            document.body.appendChild(link);
            link.click();
            link.remove();
            */

        } catch (err) {
            console.error('PDF 생성 중 오류:', err); // 디버깅을 위해 콘솔에 에러 출력
            setError('PDF 생성 중 오류가 발생했습니다: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className='mypage-container'>
            <div className="mypage-header">
                <div className="mypage-title">My Profile</div>
                <div className="mypage-underline"></div>
                <div style={{marginTop: 10, color: '#2C5530', fontWeight: 600, fontSize: 16}}>
                    Auto logout in: {Math.max(0, Math.floor(remaining / 60000))}m {Math.max(0, Math.floor((remaining % 60000) / 1000))}s
                </div>
            </div>
            
            {error && (
                <div className="error-message" style={{
                    color: '#ff6b6b',
                    backgroundColor: 'rgba(255, 107, 107, 0.1)',
                    padding: '10px',
                    borderRadius: '8px',
                    marginBottom: '20px',
                    fontSize: '14px',
                    textAlign: 'center'
                }}>
                    {error}
                </div>
            )}
            
            <div className="profile-section">
                <div className="profile-avatar">
                    <img src={user_icon} alt="Profile" />
                </div>
                
                <div className="profile-info">
                    {!isEditing ? (
                        <div className="info-display">
                            <div className="info-item">
                                <img src={user_icon} alt="" />
                                <span className="label">Nickname:</span>
                                <span className="value">{userInfo.nickname}</span>
                            </div>
                            <div className="info-item">
                                <img src={email_icon} alt="" />
                                <span className="label">Email:</span>
                                <span className="value">{userInfo.email}</span>
                            </div>
                            <div className="info-item">
                                <span className="label">Member Since:</span>
                                <span className="value">{userInfo.joinDate}</span>
                            </div>
                            <div className="bio-item">
                                <span className="label">Bio:</span>
                                <span className="value">{userInfo.bio}</span>
                            </div>
                        </div>
                    ) : (
                        <div className="info-edit">
                            <div className="edit-item">
                                <img src={user_icon} alt="" />
                                <input 
                                    type="text" 
                                    value={editInfo.nickname}
                                    onChange={(e) => handleInputChange('nickname', e.target.value)}
                                    placeholder="Nickname"
                                    disabled={loading}
                                />
                            </div>
                            <div className="edit-item">
                                <img src={email_icon} alt="" />
                                <input 
                                    type="email" 
                                    value={editInfo.email}
                                    onChange={(e) => handleInputChange('email', e.target.value)}
                                    placeholder="Email"
                                    disabled={loading}
                                />
                            </div>
                            <div className="edit-item">
                                <textarea 
                                    value={editInfo.bio}
                                    onChange={(e) => handleInputChange('bio', e.target.value)}
                                    placeholder="Bio"
                                    rows="3"
                                    disabled={loading}
                                />
                            </div>
                        </div>
                    )}
                </div>
            </div>

            <div className="action-buttons">
                {!isEditing ? (
                    <div className="button-group">
                        <div className="action-btn edit-btn" onClick={handleEdit}>
                            Edit Profile
                        </div>
                        {/* 🔴 PDF 리포트 생성 버튼 추가된 부분! */}
                        <button 
                            onClick={handleGeneratePdf}
                            disabled={loading}
                            style={{
                                marginLeft: '10px', // 기존 버튼과의 간격 조절
                                padding: '8px 16px',
                                backgroundColor: '#4CAF50', // 녹색 계열 (예시)
                                color: 'white',
                                border: 'none',
                                borderRadius: '5px',
                                cursor: loading ? 'not-allowed' : 'pointer',
                                fontWeight: 'bold',
                                opacity: loading ? 0.7 : 1, // 로딩 중일 때 투명도 조절
                            }}
                        >
                            {loading ? '생성 중...' : 'PDF 리포트 생성'}
                        </button>
                        <div className="action-btn logout-btn" onClick={handleLogout}>
                            Logout
                        </div>
                    </div>
                ) : (
                    <div className="button-group">
                        <div className="action-btn save-btn" 
                            onClick={handleSave}
                            style={{ opacity: loading ? 0.7 : 1, cursor: loading ? 'not-allowed' : 'pointer' }}
                        >
                            {loading ? 'Saving...' : 'Save Changes'}
                        </div>
                        <div className="action-btn cancel-btn" onClick={handleCancel}>
                            Cancel
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default MyPage;