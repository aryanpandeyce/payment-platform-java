import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import { loginSuccess } from '../store/authSlice';
import { api } from '../services/api';
import { ShieldCheck, KeyRound, Sparkles, ArrowRight, UserPlus, Lock, Mail, User, ShieldAlert } from 'lucide-react';

export default function LoginView() {
  const dispatch = useDispatch();
  const [isRegister, setIsRegister] = useState(false);
  
  // Login form state
  const [username, setUsername] = useState('aryan.pandey');
  const [password, setPassword] = useState('Admin@ApexPay2026!');
  
  // Register form state
  const [regUsername, setRegUsername] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regFullName, setRegFullName] = useState('');
  const [regPassword, setRegPassword] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  const handleLogin = async (e) => {
    e?.preventDefault();
    setLoading(true);
    setError(null);
    setSuccessMsg(null);
    try {
      const data = await api.login(username, password);
      dispatch(loginSuccess(data));
    } catch (err) {
      setError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e) => {
    e?.preventDefault();
    setLoading(true);
    setError(null);
    setSuccessMsg(null);

    // Password validation: min 8 chars, 1 uppercase, 1 lowercase, 1 digit
    const hasUpper = /[A-Z]/.test(regPassword);
    const hasLower = /[a-z]/.test(regPassword);
    const hasDigit = /[0-9]/.test(regPassword);
    if (regPassword.length < 8 || !hasUpper || !hasLower || !hasDigit) {
      setError("Password must be at least 8 characters and include uppercase, lowercase, and a number.");
      setLoading(false);
      return;
    }

    try {
      const data = await api.register({
        username: regUsername,
        email: regEmail,
        fullName: regFullName,
        password: regPassword
      });
      dispatch(loginSuccess(data));
    } catch (err) {
      setError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  const selectDemoUser = (userType) => {
    setError(null);
    setIsRegister(false);
    if (userType === 'ADMIN') {
      setUsername('aryan.pandey');
      setPassword('Admin@ApexPay2026!');
    } else if (userType === 'RAHUL') {
      setUsername('rahul.sharma');
      setPassword('User@ApexPay2026!');
    } else if (userType === 'PRIYA') {
      setUsername('priya.nair');
      setPassword('User@ApexPay2026!');
    } else if (userType === 'VIKRAM') {
      setUsername('vikram.singh');
      setPassword('User@ApexPay2026!');
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px 16px'
    }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: 460, padding: '36px 32px' }}>
        {/* Brand header */}
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{
            width: 56,
            height: 56,
            borderRadius: 16,
            background: 'linear-gradient(135deg, #6366f1 0%, #06b6d4 100%)',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 30px rgba(99, 102, 241, 0.4)',
            marginBottom: 14
          }}>
            <ShieldCheck size={32} color="#ffffff" />
          </div>
          <h2 style={{ fontSize: '1.6rem', fontWeight: 800, letterSpacing: '-0.02em', margin: 0 }}>
            Apex<span style={{ color: 'var(--cyan)' }}>Pay</span> Identity
          </h2>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: 4 }}>
            Microservices Identity &amp; Role-Based Access
          </p>
        </div>

        {/* Auth Mode Toggle */}
        <div style={{
          display: 'flex',
          background: 'rgba(255, 255, 255, 0.04)',
          borderRadius: 'var(--radius-md)',
          padding: 4,
          marginBottom: 20
        }}>
          <button
            type="button"
            onClick={() => { setIsRegister(false); setError(null); }}
            style={{
              flex: 1,
              padding: '8px 12px',
              border: 'none',
              borderRadius: 'var(--radius-sm)',
              background: !isRegister ? 'var(--primary)' : 'transparent',
              color: !isRegister ? '#fff' : 'var(--text-muted)',
              fontWeight: 600,
              fontSize: '0.85rem',
              cursor: 'pointer',
              transition: 'all 0.2s ease'
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => { setIsRegister(true); setError(null); }}
            style={{
              flex: 1,
              padding: '8px 12px',
              border: 'none',
              borderRadius: 'var(--radius-sm)',
              background: isRegister ? 'var(--primary)' : 'transparent',
              color: isRegister ? '#fff' : 'var(--text-muted)',
              fontWeight: 600,
              fontSize: '0.85rem',
              cursor: 'pointer',
              transition: 'all 0.2s ease'
            }}
          >
            Register (USER)
          </button>
        </div>

        {error && (
          <div style={{
            background: 'var(--danger-bg)',
            border: '1px solid rgba(244, 63, 94, 0.3)',
            color: 'var(--danger)',
            padding: '10px 14px',
            borderRadius: 'var(--radius-md)',
            fontSize: '0.8125rem',
            marginBottom: 18,
            display: 'flex',
            alignItems: 'center',
            gap: 8
          }}>
            <ShieldAlert size={16} /> {error}
          </div>
        )}

        {!isRegister ? (
          /* Sign In Form */
          <form onSubmit={handleLogin}>
            <div className="form-group">
              <label className="form-label">Username</label>
              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  className="form-control"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="e.g. aryan.pandey or rahul.sharma"
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Password</label>
              <input
                type="password"
                className="form-control"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', marginTop: 6 }}
            >
              {loading ? (
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
                  <span className="animate-spin">⏳</span> Authenticating...
                </span>
              ) : (
                <>Sign In <ArrowRight size={16} /></>
              )}
            </button>
          </form>
        ) : (
          /* Register Form */
          <form onSubmit={handleRegister}>
            <div className="form-group">
              <label className="form-label">Full Name</label>
              <input
                type="text"
                className="form-control"
                value={regFullName}
                onChange={(e) => setRegFullName(e.target.value)}
                placeholder="e.g. Anjali Deshmukh"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Username</label>
              <input
                type="text"
                className="form-control"
                value={regUsername}
                onChange={(e) => setRegUsername(e.target.value)}
                placeholder="e.g. anjali.deshmukh"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Email</label>
              <input
                type="email"
                className="form-control"
                value={regEmail}
                onChange={(e) => setRegEmail(e.target.value)}
                placeholder="e.g. anjali@apexpay.demo"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Password</label>
              <input
                type="password"
                className="form-control"
                value={regPassword}
                onChange={(e) => setRegPassword(e.target.value)}
                placeholder="Min 8 chars, 1 upper, 1 lower, 1 digit"
                required
              />
              <p style={{ fontSize: '0.7rem', color: 'var(--text-dim)', marginTop: 4 }}>
                Enforced by Bean Validation &amp; PasswordPolicy (BCrypt strength 12).
              </p>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', marginTop: 6 }}
            >
              {loading ? (
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
                  <span className="animate-spin">⏳</span> Registering User...
                </span>
              ) : (
                <>Create User Account <UserPlus size={16} /></>
              )}
            </button>
          </form>
        )}

        {/* Demo Fast Login Selector */}
        <div style={{ margin: '20px 0 14px 0', display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ flex: 1, height: 1, background: 'var(--border-color)' }} />
          <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Quick Demo Personas</span>
          <div style={{ flex: 1, height: 1, background: 'var(--border-color)' }} />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8 }}>
          <button
            type="button"
            onClick={() => selectDemoUser('ADMIN')}
            className="btn btn-secondary"
            style={{ fontSize: '0.75rem', padding: '8px 10px', textAlign: 'left', display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}
          >
            <span style={{ fontWeight: 700, color: '#c084fc' }}>👑 Aryan Pandey</span>
            <span style={{ fontSize: '0.68rem', color: 'var(--text-dim)' }}>Role: ADMIN (Full Audit)</span>
          </button>

          <button
            type="button"
            onClick={() => selectDemoUser('RAHUL')}
            className="btn btn-secondary"
            style={{ fontSize: '0.75rem', padding: '8px 10px', textAlign: 'left', display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}
          >
            <span style={{ fontWeight: 700, color: 'var(--cyan)' }}>👤 Rahul Sharma</span>
            <span style={{ fontSize: '0.68rem', color: 'var(--text-dim)' }}>Role: USER (₹100,000)</span>
          </button>

          <button
            type="button"
            onClick={() => selectDemoUser('PRIYA')}
            className="btn btn-secondary"
            style={{ fontSize: '0.75rem', padding: '8px 10px', textAlign: 'left', display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}
          >
            <span style={{ fontWeight: 700, color: 'var(--cyan)' }}>👤 Priya Nair</span>
            <span style={{ fontSize: '0.68rem', color: 'var(--text-dim)' }}>Role: USER (₹120,000)</span>
          </button>

          <button
            type="button"
            onClick={() => selectDemoUser('VIKRAM')}
            className="btn btn-secondary"
            style={{ fontSize: '0.75rem', padding: '8px 10px', textAlign: 'left', display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}
            title="Demonstrates Account Lockout after failed attempts"
          >
            <span style={{ fontWeight: 700, color: 'var(--danger)' }}>🔒 Vikram Singh</span>
            <span style={{ fontSize: '0.68rem', color: 'var(--text-dim)' }}>Status: LOCKED (Demo)</span>
          </button>
        </div>

        {/* Security architecture footer notice */}
        <div style={{
          marginTop: 20,
          padding: 10,
          background: 'rgba(255,255,255,0.02)',
          borderRadius: 'var(--radius-sm)',
          border: '1px solid var(--border-color)',
          textAlign: 'center'
        }}>
          <p style={{ fontSize: '0.72rem', color: 'var(--text-dim)', margin: 0 }}>
            🔒 <strong>DevSecOps Guardrails:</strong> In-memory JWT access token (15 min) + single-use refresh token rotation. Gateway Rate Limited at 15 req/min.
          </p>
        </div>
      </div>
    </div>
  );
}
