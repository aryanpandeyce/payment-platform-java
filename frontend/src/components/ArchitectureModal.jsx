import React from 'react';
import { X, Server, ArrowRight, ShieldCheck, Database, Layers } from 'lucide-react';

export default function ArchitectureModal({ onClose }) {
  const steps = [
    { title: "1. Client Request", desc: "React form generates unique Idempotency-Key UUID and submits to API Gateway (Port 8090).", icon: "🌐" },
    { title: "2. API Gateway & JWT", desc: "Gateway validates Bearer JWT token, injects X-User-Id header, and routes to Payment Service (Port 8086).", icon: "🛡️" },
    { title: "3. Outbox Pattern", desc: "Payment saved in PostgreSQL with status INITIATED and event stored in outbox_events in SAME transaction.", icon: "📦" },
    { title: "4. Kafka Dispatch", desc: "Scheduled poller reads outbox table and publishes to topic 'payment-initiated' on Kafka broker (Port 9092).", icon: "⚡" },
    { title: "5. Fraud Check (FR-8, 9)", desc: "Fraud Service (Port 8087) validates amount <= ₹50,000 and <= 5 transfers/min, publishing 'payment-validated'.", icon: "🔍" },
    { title: "6. Atomic Settlement", desc: "Account Service (Port 8085) atomically debits sender and credits receiver in ONE DB transaction, publishing 'payment-settled'.", icon: "💰" },
    { title: "7. Audit & Alerts", desc: "Audit logs appended to MongoDB (Port 27017) and SMS/Email notification logged to console.", icon: "📜" }
  ];

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0,0,0,0.75)',
      backdropFilter: 'blur(8px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 100,
      padding: 20
    }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: 680, maxHeight: '90vh', overflowY: 'auto', padding: 28 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <Layers size={22} color="var(--primary)" />
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0 }}>
              Event-Driven Microservices Architecture
            </h3>
          </div>
          <button
            onClick={onClose}
            className="btn btn-secondary"
            style={{ padding: '6px 10px' }}
          >
            <X size={16} />
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {steps.map((s, idx) => (
            <div
              key={idx}
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: 14,
                padding: '12px 14px',
                background: 'rgba(255, 255, 255, 0.02)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-color)'
              }}
            >
              <div style={{ fontSize: '1.4rem' }}>{s.icon}</div>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-main)' }}>
                  {s.title}
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 2 }}>
                  {s.desc}
                </div>
              </div>
            </div>
          ))}
        </div>

        <div style={{ marginTop: 20, textAlign: 'center' }}>
          <button onClick={onClose} className="btn btn-primary" style={{ padding: '8px 24px' }}>
            Got it, return to Dashboard
          </button>
        </div>
      </div>
    </div>
  );
}
