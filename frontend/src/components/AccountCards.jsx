import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { addAccount } from '../store/accountsSlice';
import { api } from '../services/api';
import { Wallet, Copy, Check, Plus, UserPlus } from 'lucide-react';

export default function AccountCards() {
  const dispatch = useDispatch();
  const { list: accounts } = useSelector(state => state.accounts);
  const { token } = useSelector(state => state.auth);

  const [copiedId, setCopiedId] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [newOwner, setNewOwner] = useState('');
  const [newBalance, setNewBalance] = useState('1000.00');
  const [loading, setLoading] = useState(false);

  const handleCopy = (id) => {
    navigator.clipboard.writeText(id);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const created = await api.createAccount(token, {
        ownerName: newOwner,
        initialBalance: newBalance,
        currency: 'INR'
      });
      dispatch(addAccount(created));
      setShowModal(false);
      setNewOwner('');
      setNewBalance('1000.00');
    } catch (err) {
      alert(err.message || 'Failed to create account');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section style={{ marginBottom: 28 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
        <div>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 700, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Wallet size={18} color="var(--primary)" /> Bank Accounts Ledger
          </h2>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-dim)', margin: 0 }}>
            PostgreSQL <code className="mono">accounts</code> table &bull; Atomic Debit / Credit
          </p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="btn btn-secondary"
          style={{ padding: '6px 12px', fontSize: '0.8rem' }}
        >
          <Plus size={14} /> New Account
        </button>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
        gap: 16
      }}>
        {accounts.map(acc => (
          <div key={acc.id} className="glass-panel" style={{ padding: 18, position: 'relative', overflow: 'hidden' }}>
            {/* Top row */}
            <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 10 }}>
              <div>
                <span style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--text-main)' }}>
                  {acc.ownerName}
                </span>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 2 }}>
                  <span className="badge badge-SETTLED" style={{ fontSize: '0.65rem', padding: '2px 8px' }}>
                    {acc.status || 'ACTIVE'}
                  </span>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-dim)' }}>{acc.currency}</span>
                </div>
              </div>

              <div style={{
                width: 34,
                height: 34,
                borderRadius: '50%',
                background: 'rgba(99, 102, 241, 0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--primary)',
                fontWeight: 800,
                fontSize: '0.875rem'
              }}>
                {acc.ownerName.charAt(0)}
              </div>
            </div>

            {/* Balance */}
            <div style={{ margin: '14px 0 10px 0' }}>
              <span style={{ fontSize: '0.7rem', textTransform: 'uppercase', color: 'var(--text-dim)', letterSpacing: '0.05em' }}>
                Available Balance
              </span>
              <div style={{
                fontSize: '1.65rem',
                fontWeight: 800,
                fontFamily: 'var(--font-mono)',
                color: 'var(--text-main)',
                letterSpacing: '-0.02em'
              }}>
                ₹{Number(acc.balance).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </div>
            </div>

            {/* Account ID row with copy */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '6px 10px',
              background: 'rgba(0, 0, 0, 0.25)',
              borderRadius: 'var(--radius-sm)',
              fontSize: '0.7rem',
              color: 'var(--text-dim)'
            }}>
              <span className="mono" title={acc.id}>
                {acc.id.substring(0, 16)}...
              </span>
              <button
                onClick={() => handleCopy(acc.id)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: copiedId === acc.id ? 'var(--success)' : 'var(--text-muted)',
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 4,
                  fontSize: '0.7rem'
                }}
              >
                {copiedId === acc.id ? <Check size={12} /> : <Copy size={12} />}
                {copiedId === acc.id ? 'Copied' : 'Copy'}
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Modal for new account */}
      {showModal && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.7)',
          backdropFilter: 'blur(6px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 50,
          padding: 20
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: 400, padding: 24 }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
              <UserPlus size={18} color="var(--primary)" /> Open New Account
            </h3>
            <form onSubmit={handleCreate}>
              <div className="form-group">
                <label className="form-label">Customer / Owner Name</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="e.g. Aryan Pandey"
                  value={newOwner}
                  onChange={e => setNewOwner(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Initial Balance (INR)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  className="form-control mono"
                  value={newBalance}
                  onChange={e => setNewBalance(e.target.value)}
                  required
                />
              </div>

              <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="btn btn-secondary"
                  style={{ flex: 1 }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="btn btn-primary"
                  style={{ flex: 1 }}
                >
                  {loading ? 'Creating...' : 'Create Account'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}
