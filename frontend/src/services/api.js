// API Client with automatic fallback to full local mock simulation
const isMockEnv = import.meta.env.VITE_USE_MOCK === 'true';

// Mock initial data
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
    updatedAt: new Date(Date.now() - 3590000).toISOString()
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
      await new Promise(r => setTimeout(r, 400));
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
      // In real backend, gateway routes to /api/accounts
      // To get all accounts or check balance
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
      await new Promise(r => setTimeout(r, 400));
      const newAcc = {
        id: crypto.randomUUID(),
        ownerName,
        balance: parseFloat(initialBalance),
        currency,
        status: 'ACTIVE',
        createdAt: new Date().toISOString()
      };
      mockAccounts.push(newAcc);
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

  // Fetch Payments (FR-16 live list)
  getPayments: async (token) => {
    if (getMockMode()) {
      return [...mockPayments].reverse();
    }
    try {
      const res = await fetch('/api/payments', {
        headers: getAuthHeaders(token)
      });
      if (!res.ok) throw new Error('Failed to fetch payments');
      const list = await res.json();
      return list.reverse();
    } catch (e) {
      console.warn("Backend getPayments failed, fallback to mock:", e.message);
      return [...mockPayments].reverse();
    }
  },

  // Initiate Payment (FR-15 double-click protected + idempotency)
  initiatePayment: async (token, { idempotencyKey, senderAccountId, receiverAccountId, amount, currency = 'INR' }) => {
    if (getMockMode()) {
      await new Promise(r => setTimeout(r, 500));

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
        updatedAt: new Date().toISOString()
      };

      mockPayments.push(payment);

      // Async lifecycle simulation (mimicking Kafka event flow):
      setTimeout(() => {
        // Step 1: Fraud Check
        if (parsedAmount > 50000) {
          payment.status = 'FAILED';
          payment.failureReason = 'EXCEEDS_MAX_AMOUNT_LIMIT';
          payment.updatedAt = new Date().toISOString();
        } else {
          payment.status = 'VALIDATED';
          payment.updatedAt = new Date().toISOString();

          // Step 2: Settlement
          setTimeout(() => {
            if (!sender || sender.balance < parsedAmount) {
              payment.status = 'FAILED';
              payment.failureReason = 'INSUFFICIENT_FUNDS';
              payment.updatedAt = new Date().toISOString();
            } else if (!receiver) {
              payment.status = 'FAILED';
              payment.failureReason = 'RECEIVER_CREDIT_FAILED_REFUNDED';
              payment.updatedAt = new Date().toISOString();
            } else {
              sender.balance -= parsedAmount;
              receiver.balance += parsedAmount;
              payment.status = 'SETTLED';
              payment.updatedAt = new Date().toISOString();
            }
          }, 1500);
        }
      }, 1200);

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
  }
};
