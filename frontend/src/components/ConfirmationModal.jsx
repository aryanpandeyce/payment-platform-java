import React, { useEffect } from 'react';
import { Send, X, AlertTriangle, ShieldCheck } from 'lucide-react';

export default function ConfirmationModal({ details, onConfirm, onCancel, isSubmitting }) {
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !isSubmitting) onCancel();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onCancel, isSubmitting]);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-title"
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
      <div className="glass-panel" style={{ width: '100%', maxWidth: 440, padding: 26, position: 'relative' }}>
        <button
          onClick={onCancel}
          disabled={isSubmitting}
          aria-label="Cancel confirmation"
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

        <h3 id="confirm-title" style={{ fontSize: '1.15rem', fontWeight: 800, marginBottom: 6, display: 'flex', alignItems: 'center', gap: 8 }}>
          <ShieldCheck size={20} color="var(--primary)" /> Confirm Fund Transfer
        </h3>
        <p style={{ fontSize: '0.75rem', color: 'var(--text-dim)', margin: '0 0 18px 0' }}>
          Please review the transaction parameters before dispatching to the Outbox.
        </p>

        {/* Transfer Summary Box */}
        <div style={{
          background: 'rgba(0,0,0,0.3)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-color)',
          padding: '16px',
          marginBottom: 20
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>From (Sender):</span>
            <strong style={{ fontSize: '0.85rem', color: 'var(--text-main)' }}>{details.senderName}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>To (Beneficiary):</span>
            <strong style={{ fontSize: '0.85rem', color: 'var(--cyan)' }}>{details.receiverName}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>Amount:</span>
            <strong className="mono" style={{ fontSize: '1.2rem', color: 'var(--success)' }}>
              ₹{Number(details.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
            </strong>
          </div>
          <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: 10, marginTop: 10 }}>
            <span style={{ fontSize: '0.7rem', color: 'var(--text-dim)', display: 'block', marginBottom: 2 }}>
              Idempotency Key:
            </span>
            <span className="mono" style={{ fontSize: '0.7rem', color: 'var(--text-muted)', wordBreak: 'break-all' }}>
              {details.idempotencyKey}
            </span>
          </div>
        </div>

        {parseFloat(details.amount) > 50000 && (
          <div style={{
            background: 'var(--danger-bg)',
            border: '1px solid rgba(244, 63, 94, 0.3)',
            color: 'var(--danger)',
            padding: '8px 12px',
            borderRadius: 'var(--radius-sm)',
            fontSize: '0.75rem',
            marginBottom: 16,
            display: 'flex',
            alignItems: 'center',
            gap: 6
          }}>
            <AlertTriangle size={14} /> Amount exceeds ₹50,000 threshold and will trigger fraud rejection.
          </div>
        )}

        <div style={{ display: 'flex', gap: 10 }}>
          <button
            type="button"
            onClick={onCancel}
            disabled={isSubmitting}
            className="btn btn-secondary"
            style={{ flex: 1 }}
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isSubmitting}
            className="btn btn-primary"
            style={{ flex: 1 }}
          >
            {isSubmitting ? 'Transferring...' : 'Authorize & Transfer'}
          </button>
        </div>
      </div>
    </div>
  );
}
