import logo from './logo.svg';
import './App.css';
import LoginSignup from './components/LoginSignup/LoginSignup';
import MyPage from './components/MyPage/MyPage';
import { useState } from 'react';

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userData, setUserData] = useState(null);

  const handleLogin = (userInfo) => {
    // Simulate successful login
    setUserData(userInfo);
    setIsLoggedIn(true);
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    setUserData(null);
  };

  return (
    <div className="App">
      {!isLoggedIn ? (
        <LoginSignup onLogin={handleLogin} />
      ) : (
        <div>
          <div className="navigation">
            <button 
              className="nav-btn logout-btn"
              onClick={handleLogout}
            >
              Logout
            </button>
          </div>
          <MyPage userData={userData} onLogout={handleLogout} />
        </div>
      )}
    </div>
  );
}

export default App;