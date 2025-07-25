import React, { useState } from 'react';
import './LoginSignup.css';
import email_icon from '../Assets/email.png';
import password_icon from '../Assets/password.png';

const ForgotPassword = ({ onBackToLogin }) => {
    const [email, setEmail] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [success, setSuccess] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleReset = async () => {
        if (!email || !newPassword) {
            setError('Please fill in all fields');
            return;
        }

        setLoading(true);
        setError('');

        const apiUrl = process.env.REACT_APP_API_URL;
        try {
            const response = await fetch(`${apiUrl}/api/reset-password`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({ email, newPassword })
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || 'Something went wrong');
            }

            setSuccess(true);
        } catch (error) {
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className='container'>
            <div className="header">
                <div className="text">Reset Password</div>
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
            
            {!success ? (
                <>
                    <div className="inputs">
                        <div className="input">
                            <img src={email_icon} alt="" />
                            <input
                                type="email"
                                placeholder="Email Id"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                                disabled={loading}
                            />
                        </div>
                        <div className="input">
                            <img src={password_icon} alt="" />
                            <input
                                type="password"
                                placeholder="New Password"
                                value={newPassword}
                                onChange={e => setNewPassword(e.target.value)}
                                disabled={loading}
                            />
                        </div>
                    </div>
                    <div className="submit-container">
                        <div className="submit" 
                            onClick={handleReset}
                            style={{ opacity: loading ? 0.7 : 1, cursor: loading ? 'not-allowed' : 'pointer' }}
                        >
                            {loading ? 'Processing...' : 'Reset Password'}
                        </div>
                    </div>
                    <div className="submit-container">
                        <div className="submit gray" onClick={onBackToLogin}>
                            Back to Login
                        </div>
                    </div>
                </>
            ) : (
                <>
                    <div style={{ color: '#2C5530', fontWeight: 600, margin: '30px 0' }}>
                        Password reset successful! Please log in with your new password.
                    </div>
                    <div className="submit-container">
                        <div className="submit" onClick={onBackToLogin}>
                            Back to Login
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default ForgotPassword; 