import React from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

export default function Toast({ toasts, onDismiss }) {
  if (!toasts || toasts.length === 0) return null;

  return (
    <div className="toast-container" aria-live="polite">
      {toasts.map(toast => (
        <div key={toast.id} className="toast-item">
          {toast.type === 'error' ? (
            <AlertCircle size={18} color="var(--danger)" />
          ) : toast.type === 'info' ? (
            <Info size={18} color="var(--cyan)" />
          ) : (
            <CheckCircle2 size={18} color="var(--success)" />
          )}
          <span style={{ flex: 1, fontSize: '0.8125rem' }}>{toast.message}</span>
          <button
            onClick={() => onDismiss(toast.id)}
            style={{ background: 'transparent', border: 'none', color: 'var(--text-dim)', cursor: 'pointer', display: 'flex' }}
          >
            <X size={14} />
          </button>
        </div>
      ))}
    </div>
  );
}
