import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { paymentInitiated, setSubmitting, setPaymentError, clearMessages } from '../store/paymentsSlice';
import { api } from '../services/api';
import { Send, Key, AlertCircle, CheckCircle2, RefreshCw, UserCheck } from 'lucide-react';
import ConfirmationModal from './ConfirmationModal';

export default function PaymentForm({ onOpenStepper, onToast }) {
  const dispatch = useDispatch();
  const { list: accounts } = useSelector(state => state.accounts);
  const { token } = useSelector(state => state.auth);
  const { isSubmitting, error, successMessage } = useSelector(state => state.payments);

  const [senderId, setSenderId] = useState('');
  const [receiverId, setReceiverId] = useState('');
  const [amount, setAmount] = useState('500.00');
  const [idempotencyKey, setIdempotencyKey] = useState(crypto.randomUUID());
  const [showConfirm, setShowConfirm] = useState(false);

  // Set default sender and receiver when accounts load
  useEffect(() => {
    if (accounts.length >= 2) {
      if (!senderId) setSenderId(accounts[0].id);
      if (!receiverId) setReceiverId(accounts[1].id);
    }
  }, [accounts]);

  const generateNewKey = () => {
    const newKey = crypto.randomUUID();
    setIdempotencyKey(newKey);
    if (onToast) onToast("Generated fresh Idempotency Key!");
  };

  const handlePreSubmit = (e) => {
    e.preventDefault();
    if (senderId === receiverId) {
      dispatch(setPaymentError("Sender and Receiver account cannot be the same."));
      return;
    }
    if (parseFloat(amount) <= 0) {
      dispatch(setPaymentError("Amount must be strictly greater than zero."));
      return;
    }
    setShowConfirm(true);
  };

  const handleConfirmedTransfer = async () => {
    setShowConfirm(false);
    dispatch(setSubmitting(true));
    try {
      const payment = await api.initiatePayment(token, {
        idempotencyKey,
        senderAccountId: senderId,
        receiverAccountId: receiverId,
        amount: parseFloat(amount),
        currency: 'INR'
      });

      dispatch(paymentInitiated(payment));
      if (onToast) onToast(`Payment initiated! Tracking in live stepper.`);
      if (onOpenStepper) onOpenStepper(payment.id);

      // Generate a new idempotency key ready for the next transfer
      generateNewKey();
    } catch (err) {
      dispatch(setPaymentError(err.message || 'Payment initiation failed'));
    }
  };

  const senderAccount = accounts.find(a => a.id === senderId);
  const receiverAccount = accounts.find(a => a.id === receiverId);

  return (
    <div className="glass-panel" style={{ padding: 24, height: '100%' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 18 }}>
        <div>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 700, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Send size={18} color="var(--cyan)" /> Initiate Fund Transfer
          </h2>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-dim)', margin: 0 }}>
            FR-15: Protected with Idempotency Key &amp; Double-Click Lock
          </p>
        </div>
      </div>

      {error && (
        <div style={{
          background: 'var(--danger-bg)',
          border: '1px solid rgba(244, 63, 94, 0.3)',
          color: 'var(--danger)',
          padding: '10px 14px',
          borderRadius: 'var(--radius-md)',
          fontSize: '0.8125rem',
          marginBottom: 16,
          display: 'flex',
          alignItems: 'center',
          gap: 8
        }}>
          <AlertCircle size={16} /> {error}
        </div>
      )}

      {successMessage && (
        <div style={{
          background: 'var(--success-bg)',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          color: 'var(--success)',
          padding: '10px 14px',
          borderRadius: 'var(--radius-md)',
          fontSize: '0.8125rem',
          marginBottom: 16,
          display: 'flex',
          alignItems: 'center',
          gap: 8
        }}>
          <CheckCircle2 size={16} /> {successMessage}
        </div>
      )}

      <form onSubmit={handlePreSubmit}>
        {/* Sender select & balance lookup */}
        <div className="form-group">
          <label htmlFor="sender-select" className="form-label">From Account (Sender)</label>
          <select
            id="sender-select"
            className="form-control"
            value={senderId}
            onChange={e => {
              setSenderId(e.target.value);
              dispatch(clearMessages());
            }}
            required
          >
            {accounts.map(acc => (
              <option key={acc.id} value={acc.id}>
                {acc.ownerName} &bull; Available: ₹{acc.balance.toFixed(2)}
              </option>
            ))}
          </select>
          {senderAccount && (
            <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', marginTop: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
              <UserCheck size={11} color="var(--success)" /> Verified Account &bull; Balance: ₹{senderAccount.balance.toFixed(2)}
            </div>
          )}
        </div>

        {/* Receiver select & lookup */}
        <div className="form-group">
          <label htmlFor="receiver-select" className="form-label">To Account (Beneficiary)</label>
          <select
            id="receiver-select"
            className="form-control"
            value={receiverId}
            onChange={e => {
              setReceiverId(e.target.value);
              dispatch(clearMessages());
            }}
            required
          >
            {accounts.map(acc => (
              <option key={acc.id} value={acc.id} disabled={acc.id === senderId}>
                {acc.ownerName} {acc.id === senderId ? '(Same account)' : ''}
              </option>
            ))}
          </select>
        </div>

        {/* Amount */}
        <div className="form-group">
          <label htmlFor="amount-input" className="form-label">Transfer Amount (INR)</label>
          <input
            id="amount-input"
            type="number"
            step="0.01"
            min="0.01"
            className="form-control mono"
            style={{ fontSize: '1.1rem', fontWeight: 700 }}
            value={amount}
            onChange={e => {
              setAmount(e.target.value);
              dispatch(clearMessages());
            }}
            required
          />

          {/* Quick preset amount chips */}
          <div style={{ display: 'flex', gap: 6, marginTop: 8, flexWrap: 'wrap' }}>
            {['500.00', '2000.00', '10000.00', '60000.00'].map(val => (
              <button
                key={val}
                type="button"
                onClick={() => setAmount(val)}
                style={{
                  background: val === '60000.00' ? 'var(--danger-bg)' : 'rgba(255, 255, 255, 0.05)',
                  border: `1px solid ${val === '60000.00' ? 'rgba(244, 63, 94, 0.3)' : 'var(--border-color)'}`,
                  color: val === '60000.00' ? 'var(--danger)' : 'var(--text-muted)',
                  fontSize: '0.7rem',
                  padding: '3px 8px',
                  borderRadius: 'var(--radius-sm)',
                  cursor: 'pointer'
                }}
              >
                ₹{parseFloat(val).toLocaleString()} {val === '60000.00' ? '(Fraud Test)' : ''}
              </button>
            ))}
          </div>
        </div>

        {/* Idempotency Key preview */}
        <div style={{
          padding: '10px 12px',
          background: 'rgba(0, 0, 0, 0.3)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-color)',
          marginBottom: 20
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
            <span style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase', display: 'flex', alignItems: 'center', gap: 4 }}>
              <Key size={12} color="var(--primary)" /> Idempotency-Key Header
            </span>
            <button
              type="button"
              onClick={generateNewKey}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--primary)',
                fontSize: '0.7rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: 4
              }}
            >
              <RefreshCw size={10} /> Regenerate
            </button>
          </div>
          <span className="mono" style={{ fontSize: '0.75rem', color: 'var(--cyan)' }}>
            {idempotencyKey}
          </span>
        </div>

        {/* Submit button - Double click lock! */}
        <button
          type="submit"
          disabled={isSubmitting || !senderId || !receiverId || senderId === receiverId}
          className="btn btn-primary"
          style={{ width: '100%', padding: '14px', fontSize: '0.95rem' }}
        >
          {isSubmitting ? (
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 10 }}>
              <span className="animate-spin">🌀</span> Submitting to Outbox &amp; Locking...
            </span>
          ) : (
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
              <Send size={16} /> Review &amp; Send ₹{parseFloat(amount || 0).toLocaleString()}
            </span>
          )}
        </button>
      </form>

      {/* Confirmation Modal */}
      {showConfirm && (
        <ConfirmationModal
          details={{
            senderName: senderAccount?.ownerName || 'Sender',
            receiverName: receiverAccount?.ownerName || 'Receiver',
            amount: parseFloat(amount),
            currency: 'INR',
            idempotencyKey
          }}
          isSubmitting={isSubmitting}
          onConfirm={handleConfirmedTransfer}
          onCancel={() => setShowConfirm(false)}
        />
      )}
    </div>
  );
}
