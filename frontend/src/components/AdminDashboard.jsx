import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import {
  Users,
  ShieldAlert,
  ShieldCheck,
  Activity,
  DollarSign,
  TrendingUp,
  AlertTriangle,
  Lock,
  Unlock,
  ExternalLink,
  Search,
  Filter,
  RefreshCw,
  Database,
  BarChart3,
  Layers
} from 'lucide-react';

export default function AdminDashboard({ onOpenAudit, onOpenArch, onOpenStepper, onToast }) {
  const [users, setUsers] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('users'); // 'users' | 'transactions' | 'observability'
  
  // Filters
  const [userSearch, setUserSearch] = useState('');
  const [paymentStatusFilter, setPaymentStatusFilter] = useState('');
  const [paymentSearch, setPaymentSearch] = useState('');

  const loadAdminData = async () => {
    try {
      const [uList, pList] = await Promise.all([
        api.getUsers(),
        api.getPayments()
      ]);
      setUsers(uList);
      setPayments(pList);
    } catch (err) {
      console.warn("Failed to load admin data:", err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAdminData();
    const interval = setInterval(loadAdminData, 4000);
    return () => clearInterval(interval);
  }, []);

  const handleToggleUserStatus = async (user) => {
    const nextStatus = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    try {
      await api.updateUserStatus(user.id, nextStatus);
      if (onToast) onToast(`User ${user.username} is now ${nextStatus}!`);
      loadAdminData();
    } catch (err) {
      if (onToast) onToast(err.message || 'Failed to update user status', 'error');
    }
  };

  // Platform Metrics Calculations
  const settledPayments = payments.filter(p => p.status === 'SETTLED');
  const failedPayments = payments.filter(p => p.status === 'FAILED');
  const totalVolume = settledPayments.reduce((sum, p) => sum + (parseFloat(p.amount) || 0), 0);
  const successRate = payments.length > 0
    ? ((settledPayments.length / payments.length) * 100).toFixed(1)
    : '100.0';

  // Failure reasons breakdown
  const failureReasons = failedPayments.reduce((acc, p) => {
    const r = p.failureReason || 'UNKNOWN';
    acc[r] = (acc[r] || 0) + 1;
    return acc;
  }, {});

  const activeUsersCount = users.filter(u => u.status === 'ACTIVE').length;
  const lockedUsersCount = users.filter(u => u.status === 'LOCKED').length;

  const filteredUsers = users.filter(u =>
    u.fullName.toLowerCase().includes(userSearch.toLowerCase()) ||
    u.username.toLowerCase().includes(userSearch.toLowerCase()) ||
    u.email.toLowerCase().includes(userSearch.toLowerCase())
  );

  const filteredPayments = payments.filter(p => {
    if (paymentStatusFilter && p.status !== paymentStatusFilter) return false;
    if (paymentSearch) {
      const q = paymentSearch.toLowerCase();
      return p.id.toLowerCase().includes(q) || (p.idempotencyKey && p.idempotencyKey.toLowerCase().includes(q));
    }
    return true;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Top Banner: Admin Profile & Separation of Duties Info */}
      <div className="glass-panel" style={{
        padding: '18px 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 16,
        background: 'linear-gradient(135deg, rgba(168, 85, 247, 0.1) 0%, rgba(99, 102, 241, 0.1) 100%)',
        borderColor: 'rgba(168, 85, 247, 0.3)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <div style={{
            width: 44,
            height: 44,
            borderRadius: 12,
            background: 'linear-gradient(135deg, #a855f7 0%, #6366f1 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 20px rgba(168, 85, 247, 0.4)'
          }}>
            <ShieldCheck size={24} color="#fff" />
          </div>
          <div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: 0 }}>
              Aryan Pandey <span className="badge badge-ADMIN">ADMINISTRATOR</span>
            </h2>
            <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', margin: 0 }}>
              System-wide oversight &bull; Role-Based Separation of Duties (transfers disabled for admins)
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <button
            onClick={onOpenAudit}
            className="btn btn-secondary"
            style={{ fontSize: '0.8rem', padding: '8px 14px' }}
          >
            <Database size={15} color="var(--cyan)" /> MongoDB Audit Logs
          </button>
          <button
            onClick={onOpenArch}
            className="btn btn-secondary"
            style={{ fontSize: '0.8rem', padding: '8px 14px' }}
          >
            <Layers size={15} /> System Architecture
          </button>
        </div>
      </div>

      {/* KPI Stat Cards */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
        gap: 16
      }}>
        <div className="glass-panel" style={{ padding: 20 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Total Settled Volume
            </span>
            <DollarSign size={18} color="var(--success)" />
          </div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: 10, color: 'var(--success)' }}>
            ₹ {totalVolume.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <p style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: 4 }}>
            {settledPayments.length} settled across 30 days
          </p>
        </div>

        <div className="glass-panel" style={{ padding: 20 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Settlement Rate
            </span>
            <TrendingUp size={18} color="var(--cyan)" />
          </div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: 10, color: 'var(--cyan)' }}>
            {successRate}%
          </div>
          <p style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: 4 }}>
            {settledPayments.length} success / {failedPayments.length} failed
          </p>
        </div>

        <div className="glass-panel" style={{ padding: 20 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Identity Security
            </span>
            <Users size={18} color="#c084fc" />
          </div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: 10 }}>
            {users.length} Users
          </div>
          <p style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: 4 }}>
            {activeUsersCount} Active &bull; <span style={{ color: 'var(--danger)' }}>{lockedUsersCount} Locked</span>
          </p>
        </div>

        <div className="glass-panel" style={{ padding: 20 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Fraud &amp; Failures
            </span>
            <AlertTriangle size={18} color="var(--danger)" />
          </div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: 10, color: 'var(--danger)' }}>
            {failedPayments.length} Rejections
          </div>
          <p style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: 4 }}>
            Velocity, Limit &amp; NSF rules
          </p>
        </div>
      </div>

      {/* Observability & DevSecOps Live Hub */}
      <div className="glass-panel" style={{ padding: '16px 20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
          <span style={{ fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-muted)' }}>
            DevSecOps &amp; Observability Endpoints
          </span>
          <span style={{ fontSize: '0.72rem', color: 'var(--cyan)' }}>Running via docker-compose.observability.yml</span>
        </div>
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: 12
        }}>
          <a
            href="http://localhost:3000"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-secondary"
            style={{ justifyContent: 'space-between', padding: '10px 14px', textDecoration: 'none' }}
          >
            <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              📊 <strong>Grafana</strong> (3000)
            </span>
            <ExternalLink size={14} />
          </a>

          <a
            href="http://localhost:5601"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-secondary"
            style={{ justifyContent: 'space-between', padding: '10px 14px', textDecoration: 'none' }}
          >
            <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              🔍 <strong>Kibana</strong> (5601)
            </span>
            <ExternalLink size={14} />
          </a>

          <a
            href="http://localhost:9000"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-secondary"
            style={{ justifyContent: 'space-between', padding: '10px 14px', textDecoration: 'none' }}
          >
            <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              🛡️ <strong>SonarQube</strong> (9000)
            </span>
            <ExternalLink size={14} />
          </a>

          <a
            href="http://localhost:9090"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-secondary"
            style={{ justifyContent: 'space-between', padding: '10px 14px', textDecoration: 'none' }}
          >
            <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              📈 <strong>Prometheus</strong> (9090)
            </span>
            <ExternalLink size={14} />
          </a>
        </div>
      </div>

      {/* Tabs Navigation */}
      <div style={{ display: 'flex', borderBottom: '1px solid var(--border-color)', gap: 16 }}>
        <button
          onClick={() => setActiveTab('users')}
          style={{
            padding: '10px 16px',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'users' ? '2px solid var(--primary)' : '2px solid transparent',
            color: activeTab === 'users' ? 'var(--text-main)' : 'var(--text-muted)',
            fontWeight: 700,
            fontSize: '0.9rem',
            cursor: 'pointer'
          }}
        >
          User Identities ({users.length})
        </button>
        <button
          onClick={() => setActiveTab('transactions')}
          style={{
            padding: '10px 16px',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'transactions' ? '2px solid var(--primary)' : '2px solid transparent',
            color: activeTab === 'transactions' ? 'var(--text-main)' : 'var(--text-muted)',
            fontWeight: 700,
            fontSize: '0.9rem',
            cursor: 'pointer'
          }}
        >
          System-Wide Transactions ({payments.length})
        </button>
        <button
          onClick={() => setActiveTab('failures')}
          style={{
            padding: '10px 16px',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'failures' ? '2px solid var(--primary)' : '2px solid transparent',
            color: activeTab === 'failures' ? 'var(--text-main)' : 'var(--text-muted)',
            fontWeight: 700,
            fontSize: '0.9rem',
            cursor: 'pointer'
          }}
        >
          Fraud &amp; Failure Breakdown
        </button>
      </div>

      {/* TAB 1: User Management Table */}
      {activeTab === 'users' && (
        <div className="glass-panel" style={{ padding: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18, flexWrap: 'wrap', gap: 12 }}>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
              <Users size={18} color="var(--primary)" /> Managed User Identities (identity_db)
            </h3>
            <div style={{ position: 'relative', width: 280 }}>
              <Search size={15} style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-dim)' }} />
              <input
                type="text"
                placeholder="Search username, name, email..."
                value={userSearch}
                onChange={e => setUserSearch(e.target.value)}
                className="form-control"
                style={{ paddingLeft: 34, fontSize: '0.85rem' }}
              />
            </div>
          </div>

          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>User / Full Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Failed Logins</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map(u => (
                  <tr key={u.id}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{u.fullName}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>@{u.username}</div>
                    </td>
                    <td className="mono" style={{ fontSize: '0.8rem' }}>{u.email}</td>
                    <td>
                      <span className={`badge badge-${u.role}`}>{u.role}</span>
                    </td>
                    <td>
                      <span className={`badge badge-${u.status}`}>{u.status}</span>
                    </td>
                    <td className="mono" style={{ textAlign: 'center' }}>{u.failedLoginAttempts || 0}</td>
                    <td>
                      {u.role === 'ADMIN' ? (
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>Immutable</span>
                      ) : (
                        <button
                          onClick={() => handleToggleUserStatus(u)}
                          className="btn btn-secondary"
                          style={{
                            padding: '4px 10px',
                            fontSize: '0.75rem',
                            color: u.status === 'ACTIVE' ? 'var(--danger)' : 'var(--success)'
                          }}
                        >
                          {u.status === 'ACTIVE' ? (
                            <><Lock size={12} /> Lock User</>
                          ) : (
                            <><Unlock size={12} /> Unlock User</>
                          )}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 2: System-Wide Transactions */}
      {activeTab === 'transactions' && (
        <div className="glass-panel" style={{ padding: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18, flexWrap: 'wrap', gap: 12 }}>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
              <Activity size={18} color="var(--cyan)" /> Platform Payment Ledger (payment_db)
            </h3>
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              <select
                value={paymentStatusFilter}
                onChange={e => setPaymentStatusFilter(e.target.value)}
                className="form-control"
                style={{ width: 140, fontSize: '0.85rem' }}
              >
                <option value="">All Statuses</option>
                <option value="SETTLED">SETTLED</option>
                <option value="FAILED">FAILED</option>
                <option value="VALIDATED">VALIDATED</option>
                <option value="INITIATED">INITIATED</option>
              </select>
              <div style={{ position: 'relative', width: 240 }}>
                <Search size={15} style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-dim)' }} />
                <input
                  type="text"
                  placeholder="Search payment ID / key..."
                  value={paymentSearch}
                  onChange={e => setPaymentSearch(e.target.value)}
                  className="form-control"
                  style={{ paddingLeft: 34, fontSize: '0.85rem' }}
                />
              </div>
            </div>
          </div>

          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Payment ID</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Reason / Details</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredPayments.map(p => (
                  <tr key={p.id}>
                    <td style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                      {new Date(p.createdAt).toLocaleDateString()} {new Date(p.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </td>
                    <td className="mono" style={{ fontSize: '0.8rem' }}>
                      {p.id.substring(0, 16)}...
                    </td>
                    <td style={{ fontWeight: 700 }}>
                      ₹ {parseFloat(p.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td>
                      <span className={`badge badge-${p.status}`}>{p.status}</span>
                    </td>
                    <td style={{ fontSize: '0.78rem', color: p.failureReason ? 'var(--danger)' : 'var(--text-dim)' }}>
                      {p.failureReason || 'Settlement Complete'}
                    </td>
                    <td>
                      <button
                        onClick={() => onOpenStepper(p.id)}
                        className="btn btn-secondary"
                        style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                      >
                        Inspect
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 3: Failure Reasons Breakdown */}
      {activeTab === 'failures' && (
        <div className="glass-panel" style={{ padding: 24 }}>
          <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
            <BarChart3 size={18} color="var(--danger)" /> Failure &amp; Fraud Rule Rejection Breakdown
          </h3>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: 16
          }}>
            {Object.entries(failureReasons).map(([reason, count]) => (
              <div key={reason} className="glass-panel" style={{ padding: 18, borderLeft: '4px solid var(--danger)' }}>
                <div style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--danger)' }}>
                  {reason}
                </div>
                <div style={{ fontSize: '1.8rem', fontWeight: 800, marginTop: 8 }}>
                  {count} occurrences
                </div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: 4 }}>
                  {((count / failedPayments.length) * 100).toFixed(1)}% of total rejections
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
