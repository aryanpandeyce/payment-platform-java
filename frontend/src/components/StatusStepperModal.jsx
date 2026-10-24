import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { api } from '../services/api';
import { X, CheckCircle2, XCircle, Clock, ShieldCheck, ArrowRight, RefreshCw } from 'lucide-react';

export default function StatusStepperModal({ paymentId, onClose }) {
  const { token } = useSelector(state => state.auth);
  const { list: payments } = useSelector(state => state.payments);
  const [timeline, setTimeline] = useState([]);
  const [currentPayment, setCurrentPayment] = useState(null);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  useEffect(() => {
    const found = payments.find(p => p.id === paymentId);
    if (found) setCurrentPayment(found);

    const fetchTimeline = async () => {
      try {
        const tl = await api.getPaymentTimeline(token, paymentId);
        setTimeline(tl);
      } catch (err) {
        console.warn("Timeline fetch error:", err);
      }
    };

    fetchTimeline();
    // Poll while not settled or failed
    const interval = setInterval(fetchTimeline, 1500);
    return () => clearInterval(interval);
  }, [paymentId, payments, token]);

  const status = currentPayment?.status || 'INITIATED';

  const isStepDone = (step) => {
    if (step === 'INITIATED') return true;
    if (step === 'VALIDATED') return status === 'VALIDATED' || status === 'SETTLED' || (status === 'FAILED' && timeline.some(t => t.status === 'VALIDATED'));
    if (step === 'SETTLED') return status === 'SETTLED';
    return false;
  };

  const isStepFailed = (step) => {
    if (status !== 'FAILED') return false;
    if (step === 'VALIDATED' && !timeline.some(t => t.status === 'VALIDATED')) return true;
    if (step === 'SETTLED' && status === 'FAILED') return true;
    return false;
  };

  const getTimeFor = (stepStatus) => {
    const entry = timeline.find(t => t.status === stepStatus);
    return entry ? new Date(entry.timestamp).toLocaleTimeString() : null;
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="stepper-title"
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
      <div className="glass-panel" style={{ width: '100%', maxWidth: 480, padding: 26, position: 'relative' }}>
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

        <h3 id="stepper-title" style={{ fontSize: '1.2rem', fontWeight: 800, marginBottom: 4, display: 'flex', alignItems: 'center', gap: 8 }}>
          <RefreshCw size={18} color="var(--primary)" className={status !== 'SETTLED' && status !== 'FAILED' ? 'animate-spin' : ''} />
          Payment Lifecycle Stepper
        </h3>
        <p style={{ fontSize: '0.75rem', color: 'var(--text-dim)', margin: '0 0 20px 0' }}>
          Payment ID: <span className="mono" style={{ color: 'var(--cyan)' }}>{paymentId}</span>
        </p>

        {/* Amount Card */}
        {currentPayment && (
          <div style={{
            background: 'rgba(0,0,0,0.25)',
            padding: '12px 16px',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--border-color)',
            marginBottom: 20,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <div>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Amount</div>
              <div className="mono" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-main)' }}>
                ₹{Number(currentPayment.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Current State</div>
              <span className={`badge badge-${status}`}>{status}</span>
            </div>
          </div>
        )}

        {/* Stepper Steps */}
        <div className="stepper-container">
          {/* Step 1: INITIATED */}
          <div className="stepper-step">
            <div className="stepper-icon-wrap">
              <div className="stepper-circle completed">
                <CheckCircle2 size={18} />
              </div>
              <div className={`stepper-line ${isStepDone('VALIDATED') ? 'completed' : ''}`} />
            </div>
            <div style={{ flex: 1, paddingBottom: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <strong style={{ fontSize: '0.875rem' }}>1. INITIATED</strong>
                <span className="mono" style={{ fontSize: '0.7rem', color: 'var(--text-dim)' }}>
                  {getTimeFor('INITIATED') || 'Logged'}
                </span>
              </div>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', margin: '2px 0 0 0' }}>
                Idempotency key locked, payment record saved, Outbox event published to Kafka <code className="mono">payment-initiated</code>.
              </p>
            </div>
          </div>

          {/* Step 2: VALIDATED */}
          <div className="stepper-step">
            <div className="stepper-icon-wrap">
              <div className={`stepper-circle ${
                isStepFailed('VALIDATED') ? 'failed' : (isStepDone('VALIDATED') ? 'completed' : (status === 'INITIATED' ? 'active' : 'pending'))
              }`}>
                {isStepFailed('VALIDATED') ? <XCircle size={18} /> : (isStepDone('VALIDATED') ? <CheckCircle2 size={18} /> : <ShieldCheck size={18} />)}
              </div>
              <div className={`stepper-line ${isStepDone('SETTLED') ? 'completed' : ''}`} />
            </div>
            <div style={{ flex: 1, paddingBottom: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <strong style={{ fontSize: '0.875rem' }}>2. VALIDATED</strong>
                <span className="mono" style={{ fontSize: '0.7rem', color: 'var(--text-dim)' }}>
                  {getTimeFor('VALIDATED') || (status === 'INITIATED' ? 'Evaluating...' : '')}
                </span>
              </div>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', margin: '2px 0 0 0' }}>
                Fraud rules verified: amount threshold (&le; ₹50,000) &amp; 1-minute velocity window (&le; 5 txns).
              </p>
              {isStepFailed('VALIDATED') && currentPayment?.failureReason && (
                <div style={{ marginTop: 6, fontSize: '0.75rem', color: 'var(--danger)', fontWeight: 600 }}>
                  Rejection Reason: {currentPayment.failureReason}
                </div>
              )}
            </div>
          </div>

          {/* Step 3: SETTLED / FAILED */}
          <div className="stepper-step">
            <div className="stepper-icon-wrap">
              <div className={`stepper-circle ${
                status === 'SETTLED' ? 'completed' : (status === 'FAILED' ? 'failed' : 'pending')
              }`}>
                {status === 'SETTLED' ? <CheckCircle2 size={18} /> : (status === 'FAILED' ? <XCircle size={18} /> : <Clock size={18} />)}
              </div>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <strong style={{ fontSize: '0.875rem' }}>
                  {status === 'FAILED' ? '3. FAILED' : '3. SETTLED'}
                </strong>
                <span className="mono" style={{ fontSize: '0.7rem', color: 'var(--text-dim)' }}>
                  {getTimeFor('SETTLED') || getTimeFor('FAILED') || (status !== 'FAILED' && status !== 'SETTLED' ? 'Pending...' : '')}
                </span>
              </div>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', margin: '2px 0 0 0' }}>
                {status === 'SETTLED'
                  ? 'Double-entry ledger settled: sender debited, receiver credited atomically in single database transaction.'
                  : (status === 'FAILED'
                      ? `Settlement aborted: ${currentPayment?.failureReason || 'Failed financial invariants'}.`
                      : 'Awaiting atomic settlement across account-service.')}
              </p>
            </div>
          </div>
        </div>

        <button
          onClick={onClose}
          className="btn btn-primary"
          style={{ width: '100%', marginTop: 14 }}
        >
          Done
        </button>
      </div>
    </div>
  );
}
