import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

// 💡💡💡 최종 수정: logo.svg 경로 정확히 지정!
// MyPage.jsx는 'src/components/MyPage'에 있고, logo.svg는 'src'에 있으므로
// 'src/components/MyPage' -> 'src/components' (..) -> 'src' (..) -> logo.svg
import profile_icon from '../../logo.svg'; // 💡💡💡 '../../logo.svg'로 두 단계 위로 이동합니다.

function MyPage({ userData, onLogout }) {
    const navigate = useNavigate();
    const [editMode, setEditMode] = useState(false);
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [passwordError, setPasswordError] = useState('');
    const [nickname, setNickname] = useState(userData ? userData.nickname : '');
    const [nicknameError, setNicknameError] = useState('');
    const [isSaving, setIsSaving] = useState(false);

    const handleAutoLogout = useCallback(() => {
        const token = localStorage.getItem('jwtToken');
        if (!token) {
            alert('세션이 만료되어 자동으로 로그아웃됩니다.');
            onLogout();
            navigate('/auth/login');
            return;
        }
    }, [onLogout, navigate]);

    useEffect(() => {
        const timerId = setTimeout(handleAutoLogout, 1 * 60 * 1000); // 1분 후 자동 로그아웃
        return () => {
            clearTimeout(timerId);
        };
    }, [handleAutoLogout]);

    const handlePasswordChange = async (e) => {
        e.preventDefault();
        setPasswordError('');

        if (newPassword !== confirmPassword) {
            setPasswordError("새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return;
        }
        if (newPassword.length < 6) {
            setPasswordError("새 비밀번호는 6자 이상이어야 합니다.");
            return;
        }

        setIsSaving(true);
        try {
            const token = localStorage.getItem('jwtToken');
            await fetch('/api/user/password-change', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ currentPassword, newPassword })
            });
            alert('비밀번호가 성공적으로 변경되었습니다.');
            setCurrentPassword('');
            setNewPassword('');
            setConfirmPassword('');
            setEditMode(false);
        } catch (error) {
            console.error('비밀번호 변경 오류:', error);
            setPasswordError('비밀번호 변경 중 오류가 발생했습니다.');
        } finally {
            setIsSaving(false);
        }
    };

    const handleNicknameChange = async (e) => {
        e.preventDefault();
        setNicknameError('');
        if (!nickname.trim()) {
            setNicknameError("닉네임을 입력해주세요.");
            return;
        }

        setIsSaving(true);
        try {
            const token = localStorage.getItem('jwtToken');
            await fetch('/api/user/nickname-change', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ nickname })
            });
            alert('닉네임이 성공적으로 변경되었습니다.');
            setEditMode(false);
        } catch (error) {
            console.error('닉네임 변경 오류:', error);
            setNicknameError('닉네임 변경 중 오류가 발생했습니다.');
        } finally {
            setIsSaving(false);
        }
    };

    const handleLogoutClick = () => {
        onLogout();
        localStorage.removeItem('jwtToken');
        alert('성공적으로 로그아웃되었습니다.');
        navigate('/auth/login');
    };

    if (!userData) {
        return <div className="mypage-container">사용자 정보를 불러오는 중...</div>;
    }

    return (
        <div className="mypage-container">
            <h2 className="mypage-header">내 정보</h2>
            <div className="profile-section">
                <img src={profile_icon} alt="Profile Icon" className="profile-icon" />
                <p className="user-email">{userData.email}</p>
                <p className="user-nickname">{nickname}</p>
            </div>

            <div className="edit-section">
                <button className="edit-btn" onClick={() => setEditMode(!editMode)}>
                    {editMode ? '편집 모드 종료' : '정보 수정'}
                </button>

                {editMode && (
                    <div className="edit-forms">
                        <h3>비밀번호 변경</h3>
                        <form onSubmit={handlePasswordChange}>
                            <input type="password" placeholder="현재 비밀번호" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required />
                            <input type="password" placeholder="새 비밀번호 (6자 이상)" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
                            <input type="password" placeholder="새 비밀번호 확인" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />
                            {passwordError && <p className="error-message">{passwordError}</p>}
                            <button type="submit" disabled={isSaving}>비밀번호 변경 {isSaving && '중...'}</button>
                        </form>

                        <h3>닉네임 변경</h3>
                        <form onSubmit={handleNicknameChange}>
                            <input type="text" placeholder="새 닉네임" value={nickname} onChange={(e) => setNickname(e.target.value)} required />
                            {nicknameError && <p className="error-message">{nicknameError}</p>}
                            <button type="submit" disabled={isSaving}>닉네임 변경 {isSaving && '중...'}</button>
                        </form>
                    </div>
                )}
            </div>
            
            <button className="logout-button" onClick={handleLogoutClick}>로그아웃</button>
        </div>
    );
}

export default MyPage;