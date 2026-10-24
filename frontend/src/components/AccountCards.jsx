import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { addAccount } from '../store/accountsSlice';
import { api } from '../services/api';
import { Wallet, Copy, Check, Plus, UserPlus, BookOpen } from 'lucide-react';
import AccountLedgerModal from './AccountLedgerModal';

export default function AccountCards({ onToast }) {
  const dispatch = useDispatch();
  const { list: accounts } = useSelector(state => state.accounts);
  const { token } = useSelector(state => state.auth);

  const [copiedId, setCopiedId] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [selectedLedgerAccount, setSelectedLedgerAccount] = useState(null);
  const [newOwner, setNewOwner] = useState('');
  const [newBalance, setNewBalance] = useState('1000.00');
  const [loading, setLoading] = useState(false);

  const handleCopy = (id) => {
    navigator.clipboard.writeText(id);
    setCopiedId(id);
    if (onToast) onToast("Account ID copied to clipboard!");
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
      if (onToast) onToast(`Account created successfully for ${created.ownerName}!`);
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
            PostgreSQL <code className="mono">accounts</code> &amp; <code className="mono">ledger_entries</code> &bull; Atomic Debit / Credit
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

            {/* Account ID row with copy & Ledger trigger */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '6px 10px',
              background: 'rgba(0, 0, 0, 0.25)',
              borderRadius: 'var(--radius-sm)',
              fontSize: '0.7rem',
              color: 'var(--text-dim)',
              marginBottom: 8
            }}>
              <span className="mono" title={acc.id}>
                {acc.id.substring(0, 14)}...
              </span>
              <button
                onClick={() => handleCopy(acc.id)}
                aria-label={`Copy account ID for ${acc.ownerName}`}
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

            {/* View Ledger & Statement Button */}
            <button
              onClick={() => setSelectedLedgerAccount(acc)}
              className="btn btn-secondary"
              style={{ width: '100%', padding: '6px 10px', fontSize: '0.75rem', justifyContent: 'center' }}
            >
              <BookOpen size={13} color="var(--primary)" /> View Ledger &amp; Statement
            </button>
          </div>
        ))}
      </div>

      {/* Account Ledger Modal */}
      {selectedLedgerAccount && (
        <AccountLedgerModal
          account={selectedLedgerAccount}
          onClose={() => setSelectedLedgerAccount(null)}
          onToast={onToast}
        />
      )}

      {/* Modal for new account */}
      {showModal && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="new-account-title"
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.7)',
            backdropFilter: 'blur(6px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
            padding: 20
          }}
        >
          <div className="glass-panel" style={{ width: '100%', maxWidth: 400, padding: 24 }}>
            <h3 id="new-account-title" style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
              <UserPlus size={18} color="var(--primary)" /> Open New Account
            </h3>
            <form onSubmit={handleCreate}>
              <div className="form-group">
                <label htmlFor="owner-name-input" className="form-label">Customer / Owner Name</label>
                <input
                  id="owner-name-input"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Vikram Mehta"
                  value={newOwner}
                  onChange={e => setNewOwner(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="balance-input" className="form-label">Initial Balance (INR)</label>
                <input
                  id="balance-input"
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
