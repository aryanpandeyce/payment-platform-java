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
import ArchitectureModal from './components/ArchitectureModal';

export default function App() {
  const dispatch = useDispatch();
  const { isAuthenticated, token } = useSelector(state => state.auth);
  const [showArch, setShowArch] = useState(false);

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
      <Navbar onOpenArch={() => setShowArch(true)} />

      <main style={{
        flex: 1,
        maxWidth: 1200,
        width: '100%',
        margin: '0 auto',
        padding: '24px 20px 48px 20px'
      }}>
        {/* Top: Accounts ledger balances */}
        <AccountCards />

        {/* Bottom split: Payment Initiation Form & Live Transactions */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(320px, 420px) 1fr',
          gap: 20,
          alignItems: 'start'
        }}>
          <PaymentForm />
          <TransactionList />
        </div>
      </main>

      {/* Architecture Explainer Modal */}
      {showArch && <ArchitectureModal onClose={() => setShowArch(false)} />}
    </div>
  );
}
