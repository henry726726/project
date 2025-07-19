import React, { useState } from 'react'
import './MyPage.css'

import user_icon from '../Assets/person.png'
import email_icon from '../Assets/email.png'
import edit_icon from '../Assets/password.png' // Using password icon as edit icon for now

const MyPage = ({ userData, onLogout }) => {
    const [userInfo, setUserInfo] = useState(userData || {
        nickname: 'User',
        email: 'user@example.com',
        joinDate: 'January 2024',
        bio: 'Welcome to our platform!'
    });

    const [isEditing, setIsEditing] = useState(false);
    const [editInfo, setEditInfo] = useState(userInfo);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

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
        } catch (error) {
            console.error('Logout error:', error);
            onLogout(); // Still logout even if API call fails
        }
    };

    return (
        <div className='mypage-container'>
            <div className="mypage-header">
                <div className="mypage-title">My Profile</div>
                <div className="mypage-underline"></div>
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

export default MyPage 