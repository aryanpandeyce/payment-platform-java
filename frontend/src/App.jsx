import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { setAccounts } from './store/accountsSlice';
import { setPayments } from './store/paymentsSlice';
import { logout } from './store/authSlice';
import { api, setAuthCallback } from './services/api';
import Navbar from './components/Navbar';
import LoginView from './components/LoginView';
import AdminDashboard from './components/AdminDashboard';
import AccountCards from './components/AccountCards';
import PaymentForm from './components/PaymentForm';
import TransactionList from './components/TransactionList';
import SpendingChart from './components/SpendingChart';
import StatusStepperModal from './components/StatusStepperModal';
import AuditLogViewerModal from './components/AuditLogViewerModal';
import ArchitectureModal from './components/ArchitectureModal';
import Toast from './components/Toast';

export default function App() {
  const dispatch = useDispatch();
  const { isAuthenticated, user, token } = useSelector(state => state.auth);
  const [showArch, setShowArch] = useState(false);
  const [showAudit, setShowAudit] = useState(false);
  const [stepperPaymentId, setStepperPaymentId] = useState(null);
  const [toasts, setToasts] = useState([]);

  // Register session expiration callback
  useEffect(() => {
    setAuthCallback(() => {
      dispatch(logout());
      addToast("Session expired or token invalid. Please log in again.", "error");
    });
  }, [dispatch]);

  const addToast = (message, type = 'success') => {
    const id = crypto.randomUUID();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 3500);
  };

  const dismissToast = (id) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  };

  // Poll accounts and transactions every 3 seconds for live balance & payment state
  useEffect(() => {
    if (!isAuthenticated) return;

    const fetchData = async () => {
      try {
        const [accs, pmts] = await Promise.all([
          api.getAccounts(token),
          api.getPayments(token)
        ]);
        dispatch(setAccounts(accs));
        dispatch(setPayments(pmts));
      } catch (err) {
        console.warn("Polling error:", err.message);
      }
    };

    fetchData(); // initial fetch
    const timer = setInterval(fetchData, 3000); // 3 second poll

    return () => clearInterval(timer);
  }, [isAuthenticated, token, user, dispatch]);

  if (!isAuthenticated || !user) {
    return <LoginView />;
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar
        onOpenArch={() => setShowArch(true)}
        onOpenAudit={() => setShowAudit(true)}
      />

      <main style={{
        flex: 1,
        maxWidth: 1240,
        width: '100%',
        margin: '0 auto',
        padding: '24px 20px 48px 20px'
      }}>
        {user.role === 'ADMIN' ? (
          /* ============================================================
             ADMIN VIEW (Aryan Pandey): System-wide oversight, User
             Management, Platform metrics, Observability links, Audit
             ============================================================ */
          <AdminDashboard
            onOpenAudit={() => setShowAudit(true)}
            onOpenArch={() => setShowArch(true)}
            onOpenStepper={id => setStepperPaymentId(id)}
            onToast={(msg, type) => addToast(msg, type)}
          />
        ) : (
          /* ============================================================
             USER VIEW: Own Accounts, Spending Chart, Transfer Form,
             Live Transaction Stepper
             ============================================================ */
          <div>
            {/* Top: Accounts ledger balances */}
            <AccountCards onToast={msg => addToast(msg, 'success')} />

            {/* Middle: Spending Chart */}
            <SpendingChart />

            {/* Bottom split: Payment Initiation Form & Live Transactions */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
              gap: 20,
              alignItems: 'start'
            }}>
              <PaymentForm
                onOpenStepper={id => setStepperPaymentId(id)}
                onToast={msg => addToast(msg, 'info')}
              />
              <TransactionList
                onOpenStepper={id => setStepperPaymentId(id)}
              />
            </div>
          </div>
        )}
      </main>

      {/* Live Status Stepper Modal */}
      {stepperPaymentId && (
        <StatusStepperModal
          paymentId={stepperPaymentId}
          onClose={() => setStepperPaymentId(null)}
        />
      )}

      {/* MongoDB Audit Log Viewer Modal */}
      {showAudit && (
        <AuditLogViewerModal
          onClose={() => setShowAudit(false)}
        />
      )}

      {/* Architecture Explainer Modal */}
      {showArch && <ArchitectureModal onClose={() => setShowArch(false)} />}

      {/* Floating Toast Notifications */}
      <Toast toasts={toasts} onDismiss={dismissToast} />
    </div>
  );
}
