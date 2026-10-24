import React, { useState } from 'react';
import { useSelector } from 'react-redux';
import { Clock, CheckCircle2, XCircle, AlertTriangle, ArrowUpRight, Search, Eye } from 'lucide-react';

export default function TransactionList({ onOpenStepper }) {
  const { list: payments } = useSelector(state => state.payments);
  const { list: accounts } = useSelector(state => state.accounts);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const getAccountName = (id) => {
    const found = accounts.find(a => a.id === id);
    return found ? found.ownerName : (id ? id.substring(0, 8) + '...' : 'Unknown');
  };

  const filtered = payments.filter(p => {
    if (statusFilter !== 'ALL' && p.status !== statusFilter) return false;
    const sender = getAccountName(p.senderAccountId).toLowerCase();
    const receiver = getAccountName(p.receiverAccountId).toLowerCase();
    const query = searchTerm.toLowerCase();
    return p.id.toLowerCase().includes(query) ||
           p.idempotencyKey.toLowerCase().includes(query) ||
           sender.includes(query) ||
           receiver.includes(query) ||
           p.status.toLowerCase().includes(query);
  });

  const renderStatusBadge = (status) => {
    switch (status) {
      case 'INITIATED':
        return (
          <span className="badge badge-INITIATED">
            <Clock size={12} className="animate-spin" /> Initiated
          </span>
        );
      case 'VALIDATED':
        return (
          <span className="badge badge-VALIDATED">
            <span className="animate-pulse">🛡️</span> Validated
          </span>
        );
      case 'SETTLED':
        return (
          <span className="badge badge-SETTLED">
            <CheckCircle2 size={12} /> Settled
          </span>
        );
      case 'FAILED':
        return (
          <span className="badge badge-FAILED">
            <XCircle size={12} /> Failed
          </span>
        );
      default:
        return <span className="badge">{status}</span>;
    }
  };

  return (
    <div className="glass-panel" style={{ padding: 24, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14, flexWrap: 'wrap', gap: 10 }}>
        <div>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 700, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <ArrowUpRight size={18} color="var(--primary)" /> Live Payment Transactions
          </h2>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-dim)', margin: 0 }}>
            FR-16: Live Polling (2s interval) &bull; Click row to view Live Status Stepper
          </p>
        </div>

        {/* Search input */}
        <div style={{ position: 'relative', width: 220 }}>
          <Search size={14} style={{ position: 'absolute', left: 10, top: 11, color: 'var(--text-dim)' }} />
          <input
            type="text"
            className="form-control"
            style={{ paddingLeft: 32, paddingRight: 10, paddingTop: 6, paddingBottom: 6, fontSize: '0.8rem' }}
            placeholder="Search payments..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Filter Tabs */}
      <div style={{ display: 'flex', gap: 6, marginBottom: 14, overflowX: 'auto', paddingBottom: 4 }}>
        {['ALL', 'INITIATED', 'VALIDATED', 'SETTLED', 'FAILED'].map(st => (
          <button
            key={st}
            onClick={() => setStatusFilter(st)}
            style={{
              padding: '4px 10px',
              borderRadius: 'var(--radius-sm)',
              fontSize: '0.75rem',
              fontWeight: 600,
              cursor: 'pointer',
              background: statusFilter === st ? 'var(--primary)' : 'rgba(255, 255, 255, 0.05)',
              border: `1px solid ${statusFilter === st ? 'var(--primary)' : 'var(--border-color)'}`,
              color: statusFilter === st ? '#fff' : 'var(--text-muted)',
              transition: 'all 0.15s ease'
            }}
          >
            {st}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div style={{
          textAlign: 'center',
          padding: '48px 20px',
          color: 'var(--text-dim)',
          background: 'rgba(0,0,0,0.2)',
          borderRadius: 'var(--radius-md)',
          border: '1px dashed var(--border-color)'
        }}>
          <p style={{ margin: 0, fontSize: '0.875rem' }}>No payment transactions found.</p>
          <span style={{ fontSize: '0.75rem' }}>Initiate a transfer using the form on the left!</span>
        </div>
      ) : (
        <div style={{ overflowX: 'auto', flex: 1 }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.8125rem' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-dim)', textTransform: 'uppercase', fontSize: '0.7rem' }}>
                <th style={{ padding: '10px 8px' }}>Payment ID / Time</th>
                <th style={{ padding: '10px 8px' }}>Transfer Route</th>
                <th style={{ padding: '10px 8px' }}>Amount</th>
                <th style={{ padding: '10px 8px' }}>Status</th>
                <th style={{ padding: '10px 8px' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(p => (
                <tr
                  key={p.id}
                  onClick={() => onOpenStepper && onOpenStepper(p.id)}
                  style={{
                    borderBottom: '1px solid rgba(255, 255, 255, 0.04)',
                    cursor: 'pointer',
                    transition: 'background 0.15s ease'
                  }}
                  onMouseEnter={e => e.currentTarget.style.background = 'rgba(255, 255, 255, 0.04)'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                >
                  {/* ID / Time */}
                  <td style={{ padding: '12px 8px' }}>
                    <div className="mono" style={{ fontWeight: 600, color: 'var(--text-main)' }}>
                      #{p.id.substring(0, 8)}
                    </div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', marginTop: 2 }}>
                      {new Date(p.createdAt).toLocaleTimeString()}
                    </div>
                  </td>

                  {/* Route */}
                  <td style={{ padding: '12px 8px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600 }}>
                      <span style={{ color: 'var(--text-muted)' }}>{getAccountName(p.senderAccountId)}</span>
                      <span style={{ color: 'var(--text-dim)' }}>➔</span>
                      <span style={{ color: 'var(--cyan)' }}>{getAccountName(p.receiverAccountId)}</span>
                    </div>
                  </td>

                  {/* Amount */}
                  <td style={{ padding: '12px 8px' }}>
                    <span className="mono" style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-main)' }}>
                      ₹{Number(p.amount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </span>
                  </td>

                  {/* Status */}
                  <td style={{ padding: '12px 8px' }}>
                    {renderStatusBadge(p.status)}
                    {p.failureReason && (
                      <div style={{ fontSize: '0.7rem', color: 'var(--danger)', marginTop: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
                        <AlertTriangle size={10} /> {p.failureReason}
                      </div>
                    )}
                  </td>

                  {/* Stepper click trigger */}
                  <td style={{ padding: '12px 8px' }}>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        if (onOpenStepper) onOpenStepper(p.id);
                      }}
                      className="btn btn-secondary"
                      style={{ padding: '4px 8px', fontSize: '0.7rem', gap: 4 }}
                      title="Inspect lifecycle stepper"
                    >
                      <Eye size={12} color="var(--primary)" /> Stepper
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
