import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { api } from '../services/api';
import { X, Database, Search, RefreshCw, Filter } from 'lucide-react';

export default function AuditLogViewerModal({ onClose }) {
  const { token } = useSelector(state => state.auth);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [eventTypeFilter, setEventTypeFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const data = await api.getAuditLogs(token, {
        eventType: eventTypeFilter || undefined,
        aggregateId: searchTerm || undefined
      });
      setLogs(data.content || []);
    } catch (err) {
      console.warn("Audit logs query error:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [eventTypeFilter, token]);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="audit-title"
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.8)',
        backdropFilter: 'blur(8px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 60,
        padding: 20
      }}
    >
      <div className="glass-panel" style={{ width: '100%', maxWidth: 840, padding: 26, position: 'relative', maxHeight: '88vh', display: 'flex', flexDirection: 'column' }}>
        <button
          onClick={onClose}
          aria-label="Close audit log viewer"
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

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div>
            <h3 id="audit-title" style={{ fontSize: '1.2rem', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
              <Database size={18} color="var(--cyan)" /> MongoDB Immutable Audit Log Trail
            </h3>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-dim)', margin: '2px 0 0 0' }}>
              Collection: <code className="mono">audit_logs</code> &bull; All Kafka Domain Events &amp; Distributed Tracing
            </p>
          </div>
          <button
            onClick={fetchLogs}
            className="btn btn-secondary"
            style={{ fontSize: '0.75rem', padding: '6px 12px' }}
          >
            <RefreshCw size={13} className={loading ? 'animate-spin' : ''} /> Refresh
          </button>
        </div>

        {/* Filter bar */}
        <div style={{ display: 'flex', gap: 10, marginBottom: 14, flexWrap: 'wrap' }}>
          <select
            className="form-control"
            style={{ width: 180, padding: '8px 10px', fontSize: '0.8rem' }}
            value={eventTypeFilter}
            onChange={e => setEventTypeFilter(e.target.value)}
          >
            <option value="">All Event Types</option>
            <option value="PaymentInitiated">PaymentInitiated</option>
            <option value="PaymentValidated">PaymentValidated</option>
            <option value="PaymentSettled">PaymentSettled</option>
            <option value="PaymentFailed">PaymentFailed</option>
          </select>

          <div style={{ flex: 1, position: 'relative' }}>
            <Search size={14} style={{ position: 'absolute', left: 10, top: 11, color: 'var(--text-dim)' }} />
            <input
              type="text"
              className="form-control"
              style={{ paddingLeft: 32, paddingRight: 10, paddingTop: 7, paddingBottom: 7, fontSize: '0.8rem' }}
              placeholder="Filter by Aggregate / Payment ID..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && fetchLogs()}
            />
          </div>
        </div>

        {/* Logs Table */}
        <div style={{ flex: 1, overflowY: 'auto' }}>
          {loading ? (
            <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-dim)' }}>
              Querying MongoDB audit documents...
            </div>
          ) : logs.length === 0 ? (
            <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-dim)' }}>
              No audit records found matching criteria.
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.75rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-dim)', textTransform: 'uppercase' }}>
                  <th style={{ padding: '8px 6px' }}>Event Type</th>
                  <th style={{ padding: '8px 6px' }}>Aggregate / Payment ID</th>
                  <th style={{ padding: '8px 6px' }}>Correlation ID</th>
                  <th style={{ padding: '8px 6px' }}>Timestamp</th>
                  <th style={{ padding: '8px 6px' }}>Payload</th>
                </tr>
              </thead>
              <tbody>
                {logs.map(log => (
                  <tr key={log.id} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.04)' }}>
                    <td style={{ padding: '8px 6px' }}>
                      <span className="badge" style={{
                        background: 'rgba(99, 102, 241, 0.15)',
                        color: 'var(--primary)',
                        border: '1px solid rgba(99, 102, 241, 0.3)',
                        fontSize: '0.65rem'
                      }}>
                        {log.eventType}
                      </span>
                    </td>
                    <td style={{ padding: '8px 6px' }}>
                      <span className="mono" title={log.aggregateId}>
                        {log.aggregateId ? log.aggregateId.substring(0, 14) + '...' : '-'}
                      </span>
                    </td>
                    <td style={{ padding: '8px 6px' }}>
                      <span className="mono" style={{ color: 'var(--cyan)' }} title={log.correlationId}>
                        {log.correlationId ? log.correlationId.substring(0, 12) + '...' : '-'}
                      </span>
                    </td>
                    <td style={{ padding: '8px 6px', color: 'var(--text-dim)' }}>
                      {new Date(log.timestamp).toLocaleTimeString()}
                    </td>
                    <td style={{ padding: '8px 6px', maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      <code className="mono" style={{ color: 'var(--text-muted)', fontSize: '0.7rem' }} title={log.payload}>
                        {log.payload}
                      </code>
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
