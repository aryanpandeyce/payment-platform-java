import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { api } from '../services/api';
import { X, Download, BookOpen, ArrowDownLeft, ArrowUpRight } from 'lucide-react';

export default function AccountLedgerModal({ account, onClose, onToast }) {
  const { token } = useSelector(state => state.auth);
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  useEffect(() => {
    if (!account) return;
    const fetchLedger = async () => {
      setLoading(true);
      try {
        const data = await api.getTransactions(token, account.id);
        setEntries(data);
      } catch (err) {
        console.warn("Failed to fetch ledger entries:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchLedger();
  }, [account, token]);

  const handleDownloadCsv = async () => {
    try {
      const csv = await api.exportStatementCsv(token, account.id);
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.setAttribute('href', url);
      link.setAttribute('download', `statement_${account.ownerName.replace(/\s+/g, '_')}_${account.id.substring(0, 8)}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      if (onToast) onToast(`Statement CSV exported successfully for ${account.ownerName}!`);
    } catch (err) {
      alert("Failed to download CSV: " + err.message);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="ledger-title"
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.75)',
        backdropFilter: 'blur(8px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 60,
        padding: 20
      }}
    >
      <div className="glass-panel" style={{ width: '100%', maxWidth: 640, padding: 26, position: 'relative', maxHeight: '85vh', display: 'flex', flexDirection: 'column' }}>
        <button
          onClick={onClose}
          aria-label="Close modal"
          style={{
            position: 'absolute',
            top: 18,
            right: 18,
            background: 'transparent',
            border: 'none',
            color: 'var(--text-dim)',
            cursor: 'pointer'
          }}
        >
          <X size={20} />
        </button>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingRight: 30, marginBottom: 16 }}>
          <div>
            <h3 id="ledger-title" style={{ fontSize: '1.2rem', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
              <BookOpen size={18} color="var(--primary)" /> {account.ownerName}'s Ledger
            </h3>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-dim)', margin: '2px 0 0 0' }}>
              Account ID: <span className="mono">{account.id}</span>
            </p>
          </div>
          <button
            onClick={handleDownloadCsv}
            className="btn btn-secondary"
            style={{ fontSize: '0.75rem', padding: '6px 12px' }}
          >
            <Download size={14} /> Export CSV
          </button>
        </div>

        {/* Ledger Entries Table */}
        <div style={{ flex: 1, overflowY: 'auto', marginTop: 10 }}>
          {loading ? (
            <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-dim)' }}>
              Loading ledger entries...
            </div>
          ) : entries.length === 0 ? (
            <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-dim)' }}>
              No ledger entries recorded for this account.
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.8rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-dim)', fontSize: '0.7rem', textTransform: 'uppercase' }}>
                  <th style={{ padding: '8px 6px' }}>Type</th>
                  <th style={{ padding: '8px 6px' }}>Amount</th>
                  <th style={{ padding: '8px 6px' }}>Balance After</th>
                  <th style={{ padding: '8px 6px' }}>Description / Ref</th>
                  <th style={{ padding: '8px 6px' }}>Time</th>
                </tr>
              </thead>
              <tbody>
                {entries.map(e => (
                  <tr key={e.id} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.04)' }}>
                    <td style={{ padding: '10px 6px' }}>
                      <span style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 4,
                        color: e.entryType === 'CREDIT' ? 'var(--success)' : 'var(--danger)',
                        fontWeight: 700,
                        fontSize: '0.75rem'
                      }}>
                        {e.entryType === 'CREDIT' ? <ArrowDownLeft size={12} /> : <ArrowUpRight size={12} />}
                        {e.entryType}
                      </span>
                    </td>
                    <td style={{ padding: '10px 6px', fontWeight: 600 }}>
                      ₹{Number(e.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td style={{ padding: '10px 6px', fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>
                      ₹{Number(e.balanceAfter).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td style={{ padding: '10px 6px', color: 'var(--text-dim)' }}>
                      <div>{e.description || 'Transfer'}</div>
                      {e.referenceId && <span className="mono" style={{ fontSize: '0.65rem' }}>Ref: {e.referenceId.substring(0, 8)}...</span>}
                    </td>
                    <td style={{ padding: '10px 6px', color: 'var(--text-dim)', fontSize: '0.7rem' }}>
                      {new Date(e.createdAt).toLocaleTimeString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
