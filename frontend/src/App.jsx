import { useState } from "react";
import "./App.css";

const API = "https://localhost:8443";

function App() {
  const [showLogin, setShowLogin] = useState(true);

  const [isLoggedIn, setIsLoggedIn] = useState(
    !!localStorage.getItem("accessToken")
  );

  const [revokedToken, setRevokedToken] = useState("");

  const [registerData, setRegisterData] = useState({
    username: "",
    email: "",
    password: ""
  });

  const [loginData, setLoginData] = useState({
    username: "",
    password: ""
  });

  const [mfaCode, setMfaCode] = useState("");

  const [mfaRequired, setMfaRequired] = useState(false);

  const [message, setMessage] = useState("");

  const [mfaSecret, setMfaSecret] = useState("");

  const [otpAuthUrl, setOtpAuthUrl] = useState("");

  const [mfaSetupStarted, setMfaSetupStarted] = useState(false);

  const [forgotPassword, setForgotPassword] = useState(false);

  const [resetData, setResetData] = useState({
    username: "",
    otp: "",
    newPassword: ""
  });

  const [showLoginPassword, setShowLoginPassword] = useState(false);
  const [showRegisterPassword, setShowRegisterPassword] = useState(false);
  const [showResetPassword, setShowResetPassword] = useState(false);

  const [lastLogin, setLastLogin] = useState(
    localStorage.getItem("lastLogin") || ""
  );

  const [mfaEnabled, setMfaEnabled] = useState(
    localStorage.getItem("mfaEnabled") === "true"
  );

  // -----------------------------------
  // PASSWORD STRENGTH
  // -----------------------------------

  const getPasswordStrength = (password) => {
    if (!password) {
      return {
        text: "",
        className: ""
      };
    }

    let score = 0;

    if (password.length >= 8) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[a-z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;

    if (score <= 2) {
      return {
        text: "Weak password",
        className: "weak"
      };
    }

    if (score <= 4) {
      return {
        text: "Medium password",
        className: "medium"
      };
    }

    return {
      text: "Strong password",
      className: "strong"
    };
  };

  const registerPasswordStrength =
    getPasswordStrength(registerData.password);

  const resetPasswordStrength =
    getPasswordStrength(resetData.newPassword);

  // -----------------------------------
  // REGISTER
  // -----------------------------------

  const handleRegister = async (e) => {
    e.preventDefault();

    setMessage("Creating account...");

    try {
      const response = await fetch(
        `${API}/api/auth/register`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify(registerData)
        }
      );

      const data = await response.text();

      if (response.ok) {
        setMessage(
          "Registration successful! You can now login."
        );

        setRegisterData({
          username: "",
          email: "",
          password: ""
        });

        setShowLogin(true);
      } else {
        setMessage(
          data || "Registration failed."
        );
      }
    } catch (error) {
      console.error(error);

      setMessage(
        "Unable to connect to IAM Server."
      );
    }
  };

  // -----------------------------------
  // LOGIN
  // -----------------------------------

  const handleLogin = async (e) => {
    e.preventDefault();

    setMessage("Logging in...");

    try {
      const response = await fetch(
        `${API}/api/auth/login`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify(loginData)
        }
      );

      const contentType =
        response.headers.get("content-type");

      let data;

      if (
        contentType &&
        contentType.includes("application/json")
      ) {
        data = await response.json();
      } else {
        data = await response.text();
      }

      console.log(
        "Login Response:",
        data
      );

      if (response.ok) {
        if (data.mfaRequired) {
          setMfaRequired(true);

          setMessage(
            "MFA verification required."
          );

          return;
        }

        if (data.token) {
          localStorage.setItem(
            "accessToken",
            data.token
          );

          const loginTime =
            new Date().toLocaleString();

          localStorage.setItem(
            "lastLogin",
            loginTime
          );

          setLastLogin(loginTime);

          setIsLoggedIn(true);

          setMessage("");
        }
      } else {
        setMessage(
          typeof data === "string"
            ? data
            : data.message ||
              data.error ||
              "Login failed."
        );
      }
    } catch (error) {
      console.error(error);

      setMessage(
        "Unable to connect to IAM Server."
      );
    }
  };

  // -----------------------------------
  // MFA LOGIN
  // -----------------------------------

  const handleMfaLogin = async (e) => {
    e.preventDefault();

    setMessage("Verifying MFA...");

    try {
      const response = await fetch(
        `${API}/api/auth/login/mfa?username=${encodeURIComponent(
          loginData.username
        )}&code=${encodeURIComponent(
          mfaCode
        )}`,
        {
          method: "POST"
        }
      );

      const contentType =
        response.headers.get("content-type");

      let data;

      if (
        contentType &&
        contentType.includes("application/json")
      ) {
        data = await response.json();
      } else {
        data = await response.text();
      }

      console.log(
        "MFA Login Response:",
        data
      );

      if (response.ok && data.token) {
        localStorage.setItem(
          "accessToken",
          data.token
        );

        const loginTime =
          new Date().toLocaleString();

        localStorage.setItem(
          "lastLogin",
          loginTime
        );

        setLastLogin(loginTime);

        setIsLoggedIn(true);

        setMfaRequired(false);

        setMfaCode("");

        setMessage("");
      } else {
        setMessage(
          typeof data === "string"
            ? data
            : data.message ||
              "Invalid MFA code."
        );
      }
    } catch (error) {
      console.error(error);

      setMessage(
        "Unable to connect to IAM Server."
      );
    }
  };

  // -----------------------------------
  // MFA SETUP
  // -----------------------------------

  const handleSetupMfa = async () => {
    setMessage("Setting up MFA...");

    const token =
      localStorage.getItem("accessToken");

    try {
      const response = await fetch(
        `${API}/api/mfa/setup`,
        {
          method: "POST",
          headers: {
            Authorization:
              `Bearer ${token}`
          }
        }
      );

      const contentType =
        response.headers.get("content-type");

      let data;

      if (
        contentType &&
        contentType.includes("application/json")
      ) {
        data = await response.json();
      } else {
        data = await response.text();
      }

      if (response.ok) {
        setMfaSecret(data.secret);

        setOtpAuthUrl(data.otpAuthUrl);

        setMfaSetupStarted(true);

        setMessage(
          "MFA setup created. Add the account to your authenticator app."
        );
      } else {
        setMessage(
          typeof data === "string"
            ? data
            : "MFA setup failed."
        );
      }
    } catch (error) {
      console.error(error);

      setMessage(
        "Unable to connect to MFA service."
      );
    }
  };

  // -----------------------------------
  // MFA VERIFY
  // -----------------------------------

  const handleVerifyMfa = async () => {
    setMessage("Verifying MFA setup...");

    const token =
      localStorage.getItem("accessToken");

    try {
      const response = await fetch(
        `${API}/api/mfa/verify?code=${encodeURIComponent(
          mfaCode
        )}`,
        {
          method: "POST",
          headers: {
            Authorization:
              `Bearer ${token}`
          }
        }
      );

      const data =
        await response.text();

      if (response.ok) {
        setMessage(
          "✅ MFA enabled successfully!"
        );

        setMfaEnabled(true);

        localStorage.setItem(
          "mfaEnabled",
          "true"
        );

        setMfaCode("");
      } else {
        setMessage(
          data || "Invalid MFA code."
        );
      }
    } catch (error) {
      console.error(error);

      setMessage(
        "Unable to connect to MFA service."
      );
    }
  };

  // -----------------------------------
  // PROTECTED API
  // -----------------------------------

  const handleProtectedTest = async () => {
    setMessage(
      "Checking protected API..."
    );

    const token =
      localStorage.getItem("accessToken");

    if (!token) {
      setMessage(
        "❌ No access token found."
      );

      return;
    }

    try {
      const response = await fetch(
        `${API}/api/auth/read-test`,
        {
          method: "GET",
          headers: {
            Authorization:
              `Bearer ${token}`
          }
        }
      );

      const data =
        await response.text();

      console.log(
        "Protected API Response:",
        data
      );

      if (response.ok) {
        setMessage(
          "✅ Protected API access successful!"
        );
      } else {
        setMessage(
          `❌ Access denied (${response.status})`
        );
      }
    } catch (error) {
      console.error(error);

      setMessage(
        "Unable to connect to protected API."
      );
    }
  };

  // -----------------------------------
  // LOGOUT
  // -----------------------------------

  const handleLogout = async () => {
    const token =
      localStorage.getItem("accessToken");

    setRevokedToken(token || "");

    try {
      const response = await fetch(
        `${API}/api/auth/logout`,
        {
          method: "POST",
          headers: {
            Authorization:
              `Bearer ${token}`
          }
        }
      );

      const data =
        await response.text();

      console.log(
        "Logout Response:",
        data
      );
    } catch (error) {
      console.error(
        "Logout request failed:",
        error
      );
    } finally {
      localStorage.removeItem(
        "accessToken"
      );

      setIsLoggedIn(false);

      setLoginData({
        username: "",
        password: ""
      });

      setMessage(
        "Logged out successfully."
      );
    }
  };

  // -----------------------------------
  // REVOKED TOKEN TEST
  // -----------------------------------

  const handleRevokedTokenTest = async () => {
    if (!revokedToken) {
      setMessage(
        "❌ No revoked token available."
      );

      return;
    }

    setMessage(
      "Testing revoked token..."
    );

    try {
      const response = await fetch(
        `${API}/api/auth/read-test`,
        {
          method: "GET",
          headers: {
            Authorization:
              `Bearer ${revokedToken}`
          }
        }
      );

      if (response.ok) {
        setMessage(
          "⚠️ Old token is still accepted. Revocation needs checking."
        );
      } else {
        setMessage(
          `✅ Token revoked successfully! Protected API denied the old token (${response.status}).`
        );
      }
    } catch (error) {
      console.error(error);

      setMessage(
        "Unable to test revoked token."
      );
    }
  };

  // -----------------------------------
  // SEND RESET OTP
  // -----------------------------------

  const handleGenerateResetOtp = async () => {
    if (!resetData.username) {
      setMessage(
        "Please enter your username."
      );

      return;
    }

    setMessage(
      "Sending OTP..."
    );

    try {
      const response = await fetch(
        `${API}/api/otp/generate?username=${encodeURIComponent(
          resetData.username
        )}`,
        {
          method: "POST"
        }
      );

      const data =
        await response.text();

      if (response.ok) {
        setMessage(
          "OTP sent to your registered email."
        );
      } else {
        setMessage(
          data || "Failed to send OTP."
        );
      }
    } catch (error) {
      console.error(error);

      setMessage(
        "Unable to connect to IAM Server."
      );
    }
  };

  // -----------------------------------
  // RESET PASSWORD
  // -----------------------------------

  const handleResetPassword = async (e) => {
    e.preventDefault();

    setMessage(
      "Resetting password..."
    );

    try {
      const response = await fetch(
        `${API}/api/auth/reset-password`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            username:
              resetData.username,
            otp:
              resetData.otp,
            newPassword:
              resetData.newPassword
          })
        }
      );

      const data =
        await response.text();

      if (response.ok) {
        setMessage(
          "✅ Password reset successfully. You can now login."
        );

        setResetData({
          username: "",
          otp: "",
          newPassword: ""
        });

        setForgotPassword(false);

        setShowLogin(true);
      } else {
        setMessage(
          data || "Password reset failed."
        );
      }
    } catch (error) {
      console.error(error);

      setMessage(
        "Unable to connect to IAM Server."
      );
    }
  };

  // -----------------------------------
  // DASHBOARD
  // -----------------------------------

  if (isLoggedIn) {
    return (
      <div className="app">

        <div className="dashboard-card">

          {/* HEADER */}

          <div className="dashboard-header">

  <div className="dashboard-brand">

    <div className="dashboard-logo">
      🔐
    </div>

    <h1>
      Zaalima IAM
    </h1>

    <p className="subtitle">
      Identity & Access Management Dashboard
    </p>

  </div>

  <div className="online-status">
    <span className="status-dot"></span>
    Secure Session
  </div>

</div>


         {/* ACCOUNT OVERVIEW */}

<div className="dashboard-section account-overview">

  <div className="section-heading">
    <div>
      <h3>Account Overview</h3>
      <p className="section-description">
        Your account and session information
      </p>
    </div>
  </div>

  <div className="account-profile">

    <div className="profile-avatar">
      {(loginData.username || "U").charAt(0).toUpperCase()}
    </div>

    <div className="profile-details">

      <h2>
        {loginData.username || "Authenticated User"}
      </h2>

      <p>
        IAM authenticated account
      </p>

      <span className="account-active-badge">
        <span className="active-dot"></span>
        Active Account
      </span>

    </div>

  </div>


  <div className="account-details-grid">

    <div className="account-detail">

      <span className="detail-label">
        USERNAME
      </span>

      <strong>
        {loginData.username || "Authenticated User"}
      </strong>

    </div>


    <div className="account-detail">

      <span className="detail-label">
        ACCOUNT STATUS
      </span>

      <strong className="status-active">
        Active
      </strong>

    </div>


    <div className="account-detail">

      <span className="detail-label">
        MFA PROTECTION
      </span>

      <strong className={mfaEnabled ? "status-active" : "status-warning"}>
        {mfaEnabled ? "Enabled" : "Not Enabled"}
      </strong>

    </div>


    <div className="account-detail">

      <span className="detail-label">
        LAST LOGIN
      </span>

      <strong>
        {lastLogin || "Current session"}
      </strong>

    </div>

  </div>

</div>

          {/* SECURITY STATUS */}

          <div className="dashboard-section">

            <h3>
              🛡️ Security Overview
            </h3>

            <div className="security-grid">

              <div className="security-card">

                <div className="security-icon">
                  🔑
                </div>

                <h4>
                  OAuth 2.0
                </h4>

                <p>
                  Authorization enabled
                </p>

                <span className="feature-status">
                  Active
                </span>

              </div>


              <div className="security-card">

                <div className="security-icon">
                  🆔
                </div>

                <h4>
                  OpenID Connect
                </h4>

                <p>
                  Identity authentication
                </p>

                <span className="feature-status">
                  Active
                </span>

              </div>


              <div className="security-card">

                <div className="security-icon">
                  🎫
                </div>

                <h4>
                  JWT
                </h4>

                <p>
                  Secure access tokens
                </p>

                <span className="feature-status">
                  Active
                </span>

              </div>


              <div className="security-card">

                <div className="security-icon">
                  📱
                </div>

                <h4>
                  MFA / TOTP
                </h4>

                <p>
                  Multi-factor protection
                </p>

                <span className="feature-status">
                  {mfaEnabled
                    ? "Enabled"
                    : "Available"}
                </span>

              </div>

            </div>

          </div>


          {/* QUICK ACTIONS */}

          <div className="dashboard-section">

            <h3>
              ⚡ Quick Security Actions
            </h3>

            <div className="quick-actions">

              <button
                className="action-button"
                onClick={handleProtectedTest}
              >
                <span>
                  🔒
                </span>

                <div>
                  <strong>
                    Protected API
                  </strong>

                  <small>
                    Test authorization
                  </small>
                </div>

              </button>


              <button
                className="action-button"
                onClick={() => {
                  setMessage(
                    lastLogin
                      ? `Last login: ${lastLogin}`
                      : "This is your current authenticated session."
                  );
                }}
              >
                <span>
                  🕐
                </span>

                <div>
                  <strong>
                    Session Info
                  </strong>

                  <small>
                    View login details
                  </small>
                </div>

              </button>


              <button
                className="action-button"
                onClick={() => {
                  setMessage(
                    mfaEnabled
                      ? "MFA is already enabled for this account."
                      : "Use the MFA section below to enable protection."
                  );
                }}
              >
                <span>
                  🛡️
                </span>

                <div>
                  <strong>
                    Security Check
                  </strong>

                  <small>
                    Check MFA protection
                  </small>
                </div>

              </button>

            </div>

          </div>


          {/* MFA */}

          <div className="dashboard-section">

            <div className="section-heading">

              <div>
                <h3>
                  📱 Multi-Factor Authentication
                </h3>

                <p>
                  Protect your account using an authenticator app.
                </p>
              </div>

              <span
                className={
                  mfaEnabled
                    ? "badge enabled"
                    : "badge"
                }
              >
                {mfaEnabled
                  ? "✓ Enabled"
                  : "Not Enabled"}
              </span>

            </div>


            {!mfaSetupStarted ? (

              <button
                className="main-button"
                onClick={handleSetupMfa}
              >
                🔐 Setup MFA
              </button>

            ) : (

              <div className="mfa-setup-box">

                <p>
                  <strong>
                    Secret Key:
                  </strong>
                </p>

                <div className="secret-box">
                  {mfaSecret}
                </div>

                <p>
                  Add this account to your
                  authenticator app and enter
                  the generated 6-digit code.
                </p>

                <input
                  type="text"
                  placeholder="Enter 6-digit MFA code"
                  value={mfaCode}
                  onChange={(e) =>
                    setMfaCode(
                      e.target.value
                    )
                  }
                  maxLength="6"
                />

                <button
                  className="main-button"
                  onClick={handleVerifyMfa}
                >
                  ✅ Verify & Enable MFA
                </button>

                <p className="otp-url">
                  {otpAuthUrl}
                </p>

              </div>

            )}

          </div>


          {/* MESSAGE */}

          {message && (
            <div className="message">
              {message}
            </div>
          )}


          {/* LOGOUT */}

          <button
            className="logout-button"
            onClick={handleLogout}
          >
            🚪 Logout
          </button>

        </div>

      </div>
    );
  }

  // -----------------------------------
  // AUTHENTICATION PAGE
  // -----------------------------------

  return (
    <div className="app">

      <div className="auth-card">

        <div className="logo">
          🔐
        </div>

        <h1>
          Zaalima IAM
        </h1>

        <p className="subtitle">
          Identity & Access Management Server
        </p>


        {!mfaRequired && (

          <div className="tabs">

            <button
              className={
                showLogin
                  ? "active"
                  : ""
              }
              onClick={() => {

                setShowLogin(true);

                setForgotPassword(false);

                setMessage("");

              }}
            >
              Login
            </button>


            <button
              className={
                !showLogin
                  ? "active"
                  : ""
              }
              onClick={() => {

                setShowLogin(false);

                setForgotPassword(false);

                setMessage("");

              }}
            >
              Register
            </button>

          </div>

        )}


        {/* MFA LOGIN */}

        {mfaRequired ? (

          <form onSubmit={handleMfaLogin}>

            <h2>
              🔐 MFA Verification
            </h2>

            <p className="subtitle">
              Enter the 6-digit code from your authenticator app.
            </p>

            <label>
              MFA Code
            </label>

            <input
              type="text"
              placeholder="Enter 6-digit code"
              value={mfaCode}
              onChange={(e) =>
                setMfaCode(
                  e.target.value
                )
              }
              maxLength="6"
              required
            />

            <button
              type="submit"
              className="main-button"
            >
              Verify MFA
            </button>

          </form>


        ) : forgotPassword ? (

          /* FORGOT PASSWORD */

          <form onSubmit={handleResetPassword}>

            <h2>
              🔑 Reset Password
            </h2>

            <p className="subtitle">
              Enter your username and verify OTP to reset your password.
            </p>

            <label>
              Username
            </label>

            <input
              type="text"
              placeholder="Enter username"
              value={resetData.username}
              onChange={(e) =>
                setResetData({
                  ...resetData,
                  username:
                    e.target.value
                })
              }
              required
            />


            <button
              type="button"
              className="main-button"
              onClick={
                handleGenerateResetOtp
              }
            >
              📧 Send OTP
            </button>


            <label>
              OTP
            </label>

            <input
              type="text"
              placeholder="Enter OTP"
              maxLength="6"
              value={resetData.otp}
              onChange={(e) =>
                setResetData({
                  ...resetData,
                  otp: e.target.value
                })
              }
              required
            />


            <label>
              New Password
            </label>

            <div className="password-wrapper">

              <input
                type={
                  showResetPassword
                    ? "text"
                    : "password"
                }
                placeholder="Enter new password"
                value={
                  resetData.newPassword
                }
                onChange={(e) =>
                  setResetData({
                    ...resetData,
                    newPassword:
                      e.target.value
                  })
                }
                required
              />

              <button
                type="button"
                className="eye-button"
                onClick={() =>
                  setShowResetPassword(
                    !showResetPassword
                  )
                }
              >
                {showResetPassword
                  ? "🙈"
                  : "👁️"}
              </button>

            </div>


            {resetPasswordStrength.text && (

              <div
                className={`password-strength ${resetPasswordStrength.className}`}
              >
                {resetPasswordStrength.text}
              </div>

            )}


            <button
              type="submit"
              className="main-button"
            >
              🔐 Reset Password
            </button>


            <button
              type="button"
              className="link-button"
              onClick={() => {

                setForgotPassword(false);

                setMessage("");

              }}
            >
              Back to Login
            </button>

          </form>


        ) : showLogin ? (

          /* LOGIN */

          <form onSubmit={handleLogin}>

            <label>
              Username
            </label>

            <input
              type="text"
              placeholder="Enter username"
              value={
                loginData.username
              }
              onChange={(e) =>
                setLoginData({
                  ...loginData,
                  username:
                    e.target.value
                })
              }
              required
            />


            <label>
              Password
            </label>

            <div className="password-wrapper">

              <input
                type={
                  showLoginPassword
                    ? "text"
                    : "password"
                }
                placeholder="Enter password"
                value={
                  loginData.password
                }
                onChange={(e) =>
                  setLoginData({
                    ...loginData,
                    password:
                      e.target.value
                  })
                }
                required
              />

              <button
                type="button"
                className="eye-button"
                onClick={() =>
                  setShowLoginPassword(
                    !showLoginPassword
                  )
                }
              >
                {showLoginPassword
                  ? "🙈"
                  : "👁️"}
              </button>

            </div>


            <button
              type="submit"
              className="main-button"
            >
              🔐 Login
            </button>


            <button
              type="button"
              className="link-button"
              onClick={() => {

                setForgotPassword(true);

                setMessage("");

              }}
            >
              Forgot Password?
            </button>

          </form>


        ) : (

          /* REGISTER */

          <form onSubmit={handleRegister}>

            <label>
              Username
            </label>

            <input
              type="text"
              placeholder="Choose username"
              value={
                registerData.username
              }
              onChange={(e) =>
                setRegisterData({
                  ...registerData,
                  username:
                    e.target.value
                })
              }
              required
            />


            <label>
              Email
            </label>

            <input
              type="email"
              placeholder="Enter email"
              value={
                registerData.email
              }
              onChange={(e) =>
                setRegisterData({
                  ...registerData,
                  email:
                    e.target.value
                })
              }
              required
            />


            <label>
              Password
            </label>

            <div className="password-wrapper">

              <input
                type={
                  showRegisterPassword
                    ? "text"
                    : "password"
                }
                placeholder="Create password"
                value={
                  registerData.password
                }
                onChange={(e) =>
                  setRegisterData({
                    ...registerData,
                    password:
                      e.target.value
                  })
                }
                required
              />

              <button
                type="button"
                className="eye-button"
                onClick={() =>
                  setShowRegisterPassword(
                    !showRegisterPassword
                  )
                }
              >
                {showRegisterPassword
                  ? "🙈"
                  : "👁️"}
              </button>

            </div>


            {registerPasswordStrength.text && (

              <div
                className={`password-strength ${registerPasswordStrength.className}`}
              >
                {registerPasswordStrength.text}
              </div>

            )}


            <button
              type="submit"
              className="main-button"
            >
              🚀 Create Account
            </button>

          </form>

        )}


        {/* MESSAGE */}

        {message && (

          <div className="message">
            {message}
          </div>

        )}


        {/* REVOKED TOKEN */}

        {revokedToken &&
          !isLoggedIn &&
          !mfaRequired &&
          !forgotPassword && (

            <button
              type="button"
              className="main-button"
              onClick={
                handleRevokedTokenTest
              }
              style={{
                marginTop: "15px"
              }}
            >
              🧪 Test Revoked Token
            </button>

          )}


        {/* SECURITY FOOTER */}

        <div className="security">

          🔒 Secure Authentication

          <span>•</span>

          🛡️ OAuth 2.0 / OIDC

          <span>•</span>

          🔑 MFA

        </div>

      </div>

    </div>
  );
}

export default App;