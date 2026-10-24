// API Client with automatic fallback to full local mock simulation
const isMockEnv = import.meta.env.VITE_USE_MOCK === 'true';

// Mock initial accounts
let mockAccounts = [
  {
    id: "a1000000-0000-0000-0000-000000000001",
    ownerName: "Rahul Sharma",
    balance: 10000.00,
    currency: "INR",
    status: "ACTIVE",
    createdAt: new Date().toISOString()
  },
  {
    id: "a2000000-0000-0000-0000-000000000002",
    ownerName: "Priya Patel",
    balance: 5000.00,
    currency: "INR",
    status: "ACTIVE",
    createdAt: new Date().toISOString()
  },
  {
    id: "a3000000-0000-0000-0000-000000000003",
    ownerName: "Karan Singh",
    balance: 2500.00,
    currency: "INR",
    status: "ACTIVE",
    createdAt: new Date().toISOString()
  }
];

// Mock ledger entries
let mockLedgerEntries = [
  {
    id: "l1000000-0000-0000-0000-000000000001",
    accountId: "a1000000-0000-0000-0000-000000000001",
    entryType: "CREDIT",
    amount: 10000.00,
    balanceAfter: 10000.00,
    referenceId: "INITIAL_DEPOSIT",
    description: "Initial account opening balance",
    createdAt: new Date(Date.now() - 86400000).toISOString()
  },
  {
    id: "l2000000-0000-0000-0000-000000000002",
    accountId: "a2000000-0000-0000-0000-000000000002",
    entryType: "CREDIT",
    amount: 5000.00,
    balanceAfter: 5000.00,
    referenceId: "INITIAL_DEPOSIT",
    description: "Initial account opening balance",
    createdAt: new Date(Date.now() - 86400000).toISOString()
  }
];

let mockPayments = [
  {
    id: "p1000000-0000-0000-0000-000000000001",
    idempotencyKey: "demo-key-settled-1",
    senderAccountId: "a1000000-0000-0000-0000-000000000001",
    receiverAccountId: "a2000000-0000-0000-0000-000000000002",
    amount: 500.00,
    currency: "INR",
    status: "SETTLED",
    failureReason: null,
    createdAt: new Date(Date.now() - 3600000).toISOString(),
    updatedAt: new Date(Date.now() - 3590000).toISOString(),
    timeline: [
      { id: "t1", status: "INITIATED", timestamp: new Date(Date.now() - 3600000).toISOString(), failureReason: null },
      { id: "t2", status: "VALIDATED", timestamp: new Date(Date.now() - 3595000).toISOString(), failureReason: null },
      { id: "t3", status: "SETTLED", timestamp: new Date(Date.now() - 3590000).toISOString(), failureReason: null }
    ]
  }
];

let mockAuditLogs = [
  {
    id: "audit-1",
    eventId: "evt-01",
    eventType: "PaymentSettled",
    aggregateId: "p1000000-0000-0000-0000-000000000001",
    timestamp: new Date(Date.now() - 3590000).toISOString(),
    payload: JSON.stringify({ amount: 500, sender: "Rahul Sharma", receiver: "Priya Patel" }),
    correlationId: "corr-demo-12345"
  }
];

let useMock = isMockEnv;

export const setMockMode = (enabled) => {
  useMock = enabled;
  localStorage.setItem('use_mock_mode', enabled ? 'true' : 'false');
};

export const getMockMode = () => {
  const stored = localStorage.getItem('use_mock_mode');
  if (stored !== null) return stored === 'true';
  return useMock;
};

const getAuthHeaders = (token) => ({
  'Content-Type': 'application/json',
  ...(token ? { 'Authorization': `Bearer ${token}` } : {})
});

export const api = {
  // Login
  login: async (username, password) => {
    if (getMockMode()) {
      await new Promise(r => setTimeout(r, 300));
      return {
        token: "mock-jwt-token-demo-xyz",
        tokenType: "Bearer",
        expiresIn: 86400,
        username: username || "admin"
      };
    }

    try {
      const res = await fetch('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error(error.message || 'Login failed');
      }
      return await res.json();
    } catch (e) {
      console.warn("Backend login failed, fallback to mock login:", e.message);
      setMockMode(true);
      return api.login(username, password);
    }
  },

  // Fetch Accounts
  getAccounts: async (token) => {
    if (getMockMode()) {
      return [...mockAccounts];
    }
    try {
      const res = await fetch('/api/accounts', {
        headers: getAuthHeaders(token)
      });
      if (!res.ok) throw new Error('Failed to fetch accounts');
      return await res.json();
    } catch (e) {
      console.warn("Backend getAccounts failed, fallback to mock:", e.message);
      return [...mockAccounts];
    }
  },

  // Create Account
  createAccount: async (token, { ownerName, initialBalance, currency = 'INR' }) => {
    if (getMockMode()) {
      await new Promise(r => setTimeout(r, 300));
      const parsedBalance = parseFloat(initialBalance);
      const newAcc = {
        id: crypto.randomUUID(),
        ownerName,
        balance: parsedBalance,
        currency,
        status: 'ACTIVE',
        createdAt: new Date().toISOString()
      };
      mockAccounts.push(newAcc);
      if (parsedBalance > 0) {
        mockLedgerEntries.push({
          id: crypto.randomUUID(),
          accountId: newAcc.id,
          entryType: "CREDIT",
          amount: parsedBalance,
          balanceAfter: parsedBalance,
          referenceId: "INITIAL_DEPOSIT",
          description: "Initial account opening deposit",
          createdAt: new Date().toISOString()
        });
      }
      return newAcc;
    }

    const res = await fetch('/api/accounts', {
      method: 'POST',
      headers: getAuthHeaders(token),
      body: JSON.stringify({ ownerName, initialBalance, currency })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to create account');
    }
    return await res.json();
  },

  // Get Account Transactions (Ledger Entries)
  getTransactions: async (token, accountId) => {
    if (getMockMode()) {
      return mockLedgerEntries
        .filter(l => l.accountId === accountId)
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }
    try {
      const res = await fetch(`/api/accounts/${accountId}/transactions`, {
        headers: getAuthHeaders(token)
      });
      if (!res.ok) throw new Error('Failed to fetch transactions');
      return await res.json();
    } catch (e) {
      console.warn("Backend getTransactions failed, fallback to mock:", e.message);
      return mockLedgerEntries.filter(l => l.accountId === accountId);
    }
  },

  // Export Account Statement CSV
  exportStatementCsv: async (token, accountId) => {
    if (getMockMode()) {
      const acc = mockAccounts.find(a => a.id === accountId) || { ownerName: "Account" };
      const entries = mockLedgerEntries.filter(l => l.accountId === accountId);
      let csv = "TransactionId,AccountId,OwnerName,Timestamp,Type,Amount,BalanceAfter,ReferenceId,Description\n";
      for (const e of entries) {
        csv += `${e.id},${e.accountId},"${acc.ownerName}",${e.createdAt},${e.entryType},${e.amount},${e.balanceAfter},"${e.referenceId || ''}","${e.description || ''}"\n`;
      }
      return csv;
    }

    const res = await fetch(`/api/accounts/${accountId}/statement/csv`, {
      headers: getAuthHeaders(token)
    });
    if (!res.ok) throw new Error('Failed to download statement');
    return await res.text();
  },

  // Fetch Payments
  getPayments: async (token, params = {}) => {
    if (getMockMode()) {
      let filtered = [...mockPayments];
      if (params.status) {
        filtered = filtered.filter(p => p.status === params.status);
      }
      if (params.search) {
        const q = params.search.toLowerCase();
        filtered = filtered.filter(p => p.id.toLowerCase().includes(q) || p.idempotencyKey.toLowerCase().includes(q));
      }
      return filtered.reverse();
    }
    try {
      const query = new URLSearchParams();
      if (params.status) query.append('status', params.status);
      if (params.search) query.append('search', params.search);
      if (params.page !== undefined) query.append('page', params.page);
      if (params.size !== undefined) query.append('size', params.size);

      const qs = query.toString() ? `?${query.toString()}` : '';
      const res = await fetch(`/api/payments${qs}`, {
        headers: getAuthHeaders(token)
      });
      if (!res.ok) throw new Error('Failed to fetch payments');
      const data = await res.json();
      return Array.isArray(data) ? data.reverse() : (data.content ? data.content : []);
    } catch (e) {
      console.warn("Backend getPayments failed, fallback to mock:", e.message);
      return [...mockPayments].reverse();
    }
  },

  // Get Payment Timeline
  getPaymentTimeline: async (token, paymentId) => {
    if (getMockMode()) {
      const p = mockPayments.find(item => item.id === paymentId);
      return p ? (p.timeline || []) : [];
    }
    try {
      const res = await fetch(`/api/payments/${paymentId}/timeline`, {
        headers: getAuthHeaders(token)
      });
      if (!res.ok) throw new Error('Failed to fetch payment timeline');
      return await res.json();
    } catch (e) {
      console.warn("Backend getPaymentTimeline failed:", e.message);
      const p = mockPayments.find(item => item.id === paymentId);
      return p ? (p.timeline || []) : [];
    }
  },

  // Initiate Payment
  initiatePayment: async (token, { idempotencyKey, senderAccountId, receiverAccountId, amount, currency = 'INR' }) => {
    if (getMockMode()) {
      await new Promise(r => setTimeout(r, 400));

      const parsedAmount = parseFloat(amount);
      const sender = mockAccounts.find(a => a.id === senderAccountId);
      const receiver = mockAccounts.find(a => a.id === receiverAccountId);

      const payment = {
        id: crypto.randomUUID(),
        idempotencyKey,
        senderAccountId,
        receiverAccountId,
        amount: parsedAmount,
        currency,
        status: 'INITIATED',
        failureReason: null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        timeline: [
          { id: crypto.randomUUID(), status: 'INITIATED', timestamp: new Date().toISOString(), failureReason: null }
        ]
      };

      mockPayments.push(payment);

      mockAuditLogs.push({
        id: crypto.randomUUID(),
        eventId: crypto.randomUUID(),
        eventType: 'PaymentInitiated',
        aggregateId: payment.id,
        timestamp: new Date().toISOString(),
        payload: JSON.stringify({ amount: parsedAmount, senderAccountId, receiverAccountId }),
        correlationId: crypto.randomUUID()
      });

      // Async lifecycle simulation (Kafka event choreography):
      setTimeout(() => {
        // Step 1: Fraud Check
        if (parsedAmount > 50000) {
          payment.status = 'FAILED';
          payment.failureReason = 'EXCEEDS_MAX_AMOUNT_LIMIT';
          payment.updatedAt = new Date().toISOString();
          payment.timeline.push({ id: crypto.randomUUID(), status: 'FAILED', timestamp: new Date().toISOString(), failureReason: payment.failureReason });
        } else {
          payment.status = 'VALIDATED';
          payment.updatedAt = new Date().toISOString();
          payment.timeline.push({ id: crypto.randomUUID(), status: 'VALIDATED', timestamp: new Date().toISOString(), failureReason: null });

          // Step 2: Settlement
          setTimeout(() => {
            if (!sender || sender.balance < parsedAmount) {
              payment.status = 'FAILED';
              payment.failureReason = 'INSUFFICIENT_FUNDS';
              payment.updatedAt = new Date().toISOString();
              payment.timeline.push({ id: crypto.randomUUID(), status: 'FAILED', timestamp: new Date().toISOString(), failureReason: payment.failureReason });
            } else if (!receiver) {
              payment.status = 'FAILED';
              payment.failureReason = 'RECEIVER_CREDIT_FAILED_REFUNDED';
              payment.updatedAt = new Date().toISOString();
              payment.timeline.push({ id: crypto.randomUUID(), status: 'FAILED', timestamp: new Date().toISOString(), failureReason: payment.failureReason });
            } else {
              sender.balance -= parsedAmount;
              receiver.balance += parsedAmount;
              payment.status = 'SETTLED';
              payment.updatedAt = new Date().toISOString();
              payment.timeline.push({ id: crypto.randomUUID(), status: 'SETTLED', timestamp: new Date().toISOString(), failureReason: null });

              // Record ledger entries
              mockLedgerEntries.push({
                id: crypto.randomUUID(),
                accountId: sender.id,
                entryType: "DEBIT",
                amount: parsedAmount,
                balanceAfter: sender.balance,
                referenceId: payment.id,
                description: `Transfer to ${receiver.ownerName}`,
                createdAt: new Date().toISOString()
              });
              mockLedgerEntries.push({
                id: crypto.randomUUID(),
                accountId: receiver.id,
                entryType: "CREDIT",
                amount: parsedAmount,
                balanceAfter: receiver.balance,
                referenceId: payment.id,
                description: `Transfer from ${sender.ownerName}`,
                createdAt: new Date().toISOString()
              });
            }
          }, 1500);
        }
      }, 1000);

      return payment;
    }

    const res = await fetch('/api/payments', {
      method: 'POST',
      headers: {
        ...getAuthHeaders(token),
        'Idempotency-Key': idempotencyKey
      },
      body: JSON.stringify({ senderAccountId, receiverAccountId, amount, currency })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Payment initiation rejected');
    }
    return await res.json();
  },

  // Get Audit Logs (MongoDB)
  getAuditLogs: async (token, params = {}) => {
    if (getMockMode()) {
      let filtered = [...mockAuditLogs];
      if (params.eventType) {
        filtered = filtered.filter(a => a.eventType === params.eventType);
      }
      if (params.aggregateId) {
        filtered = filtered.filter(a => a.aggregateId === params.aggregateId);
      }
      return {
        content: filtered.reverse(),
        totalElements: filtered.length,
        totalPages: 1
      };
    }
    try {
      const query = new URLSearchParams();
      if (params.eventType) query.append('eventType', params.eventType);
      if (params.correlationId) query.append('correlationId', params.correlationId);
      if (params.aggregateId) query.append('aggregateId', params.aggregateId);
      if (params.page !== undefined) query.append('page', params.page);
      if (params.size !== undefined) query.append('size', params.size);

      const qs = query.toString() ? `?${query.toString()}` : '';
      const res = await fetch(`/api/audit-logs${qs}`, {
        headers: getAuthHeaders(token)
      });
      if (!res.ok) throw new Error('Failed to fetch audit logs');
      return await res.json();
    } catch (e) {
      console.warn("Backend getAuditLogs failed, fallback to mock:", e.message);
      return { content: mockAuditLogs, totalElements: mockAuditLogs.length, totalPages: 1 };
    }
  }
};
