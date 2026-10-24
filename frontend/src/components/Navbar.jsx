import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { logout, setMockModeState } from '../store/authSlice';
import { setMockMode } from '../services/api';
import { ShieldCheck, LogOut, Radio, Server, Layers, Sun, Moon, Database } from 'lucide-react';

export default function Navbar({ onOpenArch, onOpenAudit }) {
  const dispatch = useDispatch();
  const { username, isMockMode } = useSelector(state => state.auth);
  const [theme, setTheme] = useState(localStorage.getItem('theme') || 'dark');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => (prev === 'dark' ? 'light' : 'dark'));
  };

  const toggleMock = () => {
    const next = !isMockMode;
    setMockMode(next);
    dispatch(setMockModeState(next));
  };

  return (
    <header style={{
      borderBottom: '1px solid var(--border-color)',
      background: 'rgba(10, 14, 23, 0.8)',
      backdropFilter: 'blur(12px)',
      position: 'sticky',
      top: 0,
      zIndex: 40
    }}>
      <div style={{
        maxWidth: 1200,
        margin: '0 auto',
        padding: '14px 20px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 12
      }}>
        {/* Brand */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            width: 38,
            height: 38,
            borderRadius: 10,
            background: 'linear-gradient(135deg, #6366f1 0%, #06b6d4 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 20px rgba(99, 102, 241, 0.4)'
          }}>
            <ShieldCheck size={22} color="#ffffff" />
          </div>
          <div>
            <h1 style={{ fontSize: '1.2rem', fontWeight: 800, letterSpacing: '-0.02em', margin: 0 }}>
              Apex<span style={{ color: 'var(--cyan)' }}>Pay</span>
            </h1>
            <p style={{ fontSize: '0.7rem', color: 'var(--text-dim)', margin: 0 }}>
              Event-Driven Payment Microservices
            </p>
          </div>
        </div>

        {/* Right actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
          {/* Audit Logs button */}
          <button
            onClick={onOpenAudit}
            className="btn btn-secondary"
            style={{ padding: '6px 12px', fontSize: '0.8rem' }}
            title="MongoDB Event Audit Trail Viewer"
          >
            <Database size={14} color="var(--cyan)" /> Audit Trail
          </button>

          {/* Architecture trigger */}
          <button
            onClick={onOpenArch}
            className="btn btn-secondary"
            style={{ padding: '6px 12px', fontSize: '0.8rem' }}
          >
            <Layers size={14} /> Architecture
          </button>

          {/* Theme toggle */}
          <button
            onClick={toggleTheme}
            className="btn btn-secondary"
            style={{ padding: '6px 10px' }}
            title={`Switch to ${theme === 'dark' ? 'Light' : 'Dark'} mode`}
            aria-label="Toggle theme"
          >
            {theme === 'dark' ? <Sun size={15} color="var(--warning)" /> : <Moon size={15} color="var(--primary)" />}
          </button>

          {/* Mode switch badge */}
          <button
            onClick={toggleMock}
            title="Click to toggle between real backend & mock simulation"
            className="btn"
            style={{
              padding: '6px 12px',
              fontSize: '0.75rem',
              background: isMockMode ? 'rgba(245, 158, 11, 0.15)' : 'rgba(16, 185, 129, 0.15)',
              border: `1px solid ${isMockMode ? 'rgba(245, 158, 11, 0.4)' : 'rgba(16, 185, 129, 0.4)'}`,
              color: isMockMode ? 'var(--warning)' : 'var(--success)'
            }}
          >
            {isMockMode ? <Radio size={14} className="animate-pulse" /> : <Server size={14} />}
            {isMockMode ? 'Mock Mode (GitHub Pages Safe)' : 'Live Microservices'}
          </button>

          {/* User profile & Logout */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              👤 <strong style={{ color: 'var(--text-main)' }}>{username}</strong>
            </span>
            <button
              onClick={() => dispatch(logout())}
              className="btn btn-secondary"
              style={{ padding: '6px 10px' }}
              title="Logout"
              aria-label="Logout"
            >
              <LogOut size={14} />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
