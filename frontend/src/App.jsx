import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { setAccounts } from './store/accountsSlice';
import { setPayments } from './store/paymentsSlice';
import { api } from './services/api';
import Navbar from './components/Navbar';
import LoginView from './components/LoginView';
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
  const { isAuthenticated, token } = useSelector(state => state.auth);
  const [showArch, setShowArch] = useState(false);
  const [showAudit, setShowAudit] = useState(false);
  const [stepperPaymentId, setStepperPaymentId] = useState(null);
  const [toasts, setToasts] = useState([]);

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

  // Poll accounts and transactions every 2 seconds for live status
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
    const timer = setInterval(fetchData, 2000); // 2 second poll

    return () => clearInterval(timer);
  }, [isAuthenticated, token, dispatch]);

  if (!isAuthenticated) {
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
