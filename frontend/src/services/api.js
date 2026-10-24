// API Client with in-memory token management, auto-refresh on 401, and mock fallback
const isMockEnv = import.meta.env.VITE_USE_MOCK === 'true';

// In-memory token storage (NEVER saved in localStorage to prevent XSS exfiltration)
let inMemoryTokens = {
  accessToken: null,
  refreshToken: null
};

let currentAuthenticatedUser = null;
let onAuthExpiredCallback = null;

export const setAuthCallback = (callback) => {
  onAuthExpiredCallback = callback;
};

export const setTokens = (tokens) => {
  inMemoryTokens = { ...inMemoryTokens, ...tokens };
};

export const clearTokens = () => {
  inMemoryTokens = { accessToken: null, refreshToken: null };
  currentAuthenticatedUser = null;
};

export const getAccessToken = () => inMemoryTokens.accessToken;

// Mock mode toggle and persistence
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

// ==========================================
// MOCK DATA (Mirrors Backend Demo Seed Data)
// ==========================================

const ADMIN_ID = "11111111-1111-1111-1111-111111111111";
const RAHUL_ID = "22222222-2222-2222-2222-222222222221";
const PRIYA_ID = "22222222-2222-2222-2222-222222222222";
const ANKIT_ID = "22222222-2222-2222-2222-222222222223";
const SNEHA_ID = "22222222-2222-2222-2222-222222222224";
const FAIZ_ID = "22222222-2222-2222-2222-222222222225";
const KAVYA_ID = "22222222-2222-2222-2222-222222222226";
const VIKRAM_ID = "22222222-2222-2222-2222-222222222227";
const ANANYA_ID = "22222222-2222-2222-2222-222222222228";

let mockUsers = [
  {
    id: ADMIN_ID,
    username: "aryan.pandey",
    email: "aryan.pandey@apexpay.demo",
    fullName: "Aryan Pandey",
    role: "ADMIN",
    status: "ACTIVE",
    failedLoginAttempts: 0,
    createdAt: new Date(Date.now() - 30 * 86400000).toISOString(),
    lastLoginAt: new Date().toISOString()
  },
  {
    id: RAHUL_ID,
    username: "rahul.sharma",
    email: "rahul.sharma@apexpay.demo",
    fullName: "Rahul Sharma",
    role: "USER",
    status: "ACTIVE",
    failedLoginAttempts: 0,
    createdAt: new Date(Date.now() - 28 * 86400000).toISOString(),
    lastLoginAt: new Date(Date.now() - 3600000).toISOString()
  },
  {
    id: PRIYA_ID,
    username: "priya.nair",
    email: "priya.nair@apexpay.demo",
    fullName: "Priya Nair",
    role: "USER",
    status: "ACTIVE",
    failedLoginAttempts: 0,
    createdAt: new Date(Date.now() - 25 * 86400000).toISOString(),
    lastLoginAt: new Date(Date.now() - 7200000).toISOString()
  },
  {
    id: ANKIT_ID,
    username: "ankit.verma",
    email: "ankit.verma@apexpay.demo",
    fullName: "Ankit Verma",
    role: "USER",
    status: "ACTIVE",
    failedLoginAttempts: 0,
    createdAt: new Date(Date.now() - 20 * 86400000).toISOString(),
    lastLoginAt: new Date(Date.now() - 14400000).toISOString()
  },
  {
    id: SNEHA_ID,
    username: "sneha.iyer",
    email: "sneha.iyer@apexpay.demo",
    fullName: "Sneha Iyer",
    role: "USER",
    status: "ACTIVE",
    failedLoginAttempts: 0,
    createdAt: new Date(Date.now() - 18 * 86400000).toISOString(),
    lastLoginAt: new Date(Date.now() - 28800000).toISOString()
  },
  {
    id: FAIZ_ID,
    username: "mohammed.faiz",
    email: "mohammed.faiz@apexpay.demo",
    fullName: "Mohammed Faiz",
    role: "USER",
    status: "ACTIVE",
    failedLoginAttempts: 0,
    createdAt: new Date(Date.now() - 15 * 86400000).toISOString(),
    lastLoginAt: new Date(Date.now() - 43200000).toISOString()
  },
  {
    id: KAVYA_ID,
    username: "kavya.reddy",
    email: "kavya.reddy@apexpay.demo",
    fullName: "Kavya Reddy",
    role: "USER",
    status: "ACTIVE",
    failedLoginAttempts: 0,
    createdAt: new Date(Date.now() - 12 * 86400000).toISOString(),
    lastLoginAt: new Date(Date.now() - 86400000).toISOString()
  },
  {
    id: VIKRAM_ID,
    username: "vikram.singh",
    email: "vikram.singh@apexpay.demo",
    fullName: "Vikram Singh",
    role: "USER",
    status: "LOCKED",
    failedLoginAttempts: 5,
    createdAt: new Date(Date.now() - 10 * 86400000).toISOString(),
    lastLoginAt: new Date(Date.now() - 172800000).toISOString()
  },
  {
    id: ANANYA_ID,
    username: "ananya.gupta",
    email: "ananya.gupta@apexpay.demo",
    fullName: "Ananya Gupta",
    role: "USER",
    status: "ACTIVE",
    failedLoginAttempts: 0,
    createdAt: new Date(Date.now() - 7 * 86400000).toISOString(),
    lastLoginAt: new Date(Date.now() - 100000000).toISOString()
  }
];

let mockAccounts = [
  { id: "a1000000-0000-0000-0000-000000000001", ownerUserId: RAHUL_ID, ownerName: "Rahul Sharma (Primary)", balance: 85000.00, currency: "INR", status: "ACTIVE", createdAt: new Date(Date.now() - 28 * 86400000).toISOString() },
  { id: "a1000000-0000-0000-0000-000000000002", ownerUserId: RAHUL_ID, ownerName: "Rahul Sharma (Savings)", balance: 15000.00, currency: "INR", status: "ACTIVE", createdAt: new Date(Date.now() - 27 * 86400000).toISOString() },
  { id: "a2000000-0000-0000-0000-000000000001", ownerUserId: PRIYA_ID, ownerName: "Priya Nair", balance: 120000.00, currency: "INR", status: "ACTIVE", createdAt: new Date(Date.now() - 25 * 86400000).toISOString() },
  { id: "a3000000-0000-0000-0000-000000000001", ownerUserId: ANKIT_ID, ownerName: "Ankit Verma", balance: 45000.00, currency: "INR", status: "ACTIVE", createdAt: new Date(Date.now() - 20 * 86400000).toISOString() },
  { id: "a4000000-0000-0000-0000-000000000001", ownerUserId: SNEHA_ID, ownerName: "Sneha Iyer", balance: 92000.00, currency: "INR", status: "ACTIVE", createdAt: new Date(Date.now() - 18 * 86400000).toISOString() },
  { id: "a5000000-0000-0000-0000-000000000001", ownerUserId: FAIZ_ID, ownerName: "Mohammed Faiz (Current)", balance: 60000.00, currency: "INR", status: "ACTIVE", createdAt: new Date(Date.now() - 15 * 86400000).toISOString() },
  { id: "a5000000-0000-0000-0000-000000000002", ownerUserId: FAIZ_ID, ownerName: "Mohammed Faiz (Savings)", balance: 25000.00, currency: "INR", status: "ACTIVE", createdAt: new Date(Date.now() - 14 * 86400000).toISOString() },
  { id: "a6000000-0000-0000-0000-000000000001", ownerUserId: KAVYA_ID, ownerName: "Kavya Reddy", balance: 110000.00, currency: "INR", status: "ACTIVE", createdAt: new Date(Date.now() - 12 * 86400000).toISOString() },
  { id: "a7000000-0000-0000-0000-000000000001", ownerUserId: VIKRAM_ID, ownerName: "Vikram Singh (Locked)", balance: 5000.00, currency: "INR", status: "ACTIVE", createdAt: new Date(Date.now() - 10 * 86400000).toISOString() },
  { id: "a8000000-0000-0000-0000-000000000001", ownerUserId: ANANYA_ID, ownerName: "Ananya Gupta", balance: 78000.00, currency: "INR", status: "ACTIVE", createdAt: new Date(Date.now() - 7 * 86400000).toISOString() }
];

let mockLedgerEntries = [
  { id: "l1", accountId: "a1000000-0000-0000-0000-000000000001", entryType: "CREDIT", amount: 100000.00, balanceAfter: 100000.00, referenceId: "INITIAL_DEPOSIT", description: "Account opening deposit", createdAt: new Date(Date.now() - 28 * 86400000).toISOString() },
  { id: "l2", accountId: "a2000000-0000-0000-0000-000000000001", entryType: "CREDIT", amount: 120000.00, balanceAfter: 120000.00, referenceId: "INITIAL_DEPOSIT", description: "Account opening deposit", createdAt: new Date(Date.now() - 25 * 86400000).toISOString() }
];

// Generate 40 historical payments
let mockPayments = [];
const paymentSenders = [
  { userId: RAHUL_ID, accountId: "a1000000-0000-0000-0000-000000000001" },
  { userId: PRIYA_ID, accountId: "a2000000-0000-0000-0000-000000000001" },
  { userId: ANKIT_ID, accountId: "a3000000-0000-0000-0000-000000000001" },
  { userId: SNEHA_ID, accountId: "a4000000-0000-0000-0000-000000000001" },
  { userId: FAIZ_ID,  accountId: "a5000000-0000-0000-0000-000000000001" },
  { userId: KAVYA_ID, accountId: "a6000000-0000-0000-0000-000000000001" },
  { userId: ANANYA_ID, accountId: "a8000000-0000-0000-0000-000000000001" }
];

for (let i = 1; i <= 40; i++) {
  const senderObj = paymentSenders[i % paymentSenders.length];
  const receiverObj = paymentSenders[(i + 1) % paymentSenders.length];
  const isFailed = (i % 5 === 0);
  let status = "SETTLED";
  let failureReason = null;

  if (isFailed) {
    status = "FAILED";
    if (i % 3 === 0) failureReason = "EXCEEDS_MAX_AMOUNT_LIMIT";
    else if (i % 2 === 0) failureReason = "VELOCITY_LIMIT_EXCEEDED";
    else failureReason = "INSUFFICIENT_FUNDS";
  }

  const pmtTime = new Date(Date.now() - (30 - Math.floor(i * 0.7)) * 86400000 + i * 3600000).toISOString();
  const amount = isFailed && failureReason === "EXCEEDS_MAX_AMOUNT_LIMIT" ? 75000.00 : (200.00 + (i * 150.00));

  mockPayments.push({
    id: `pmt-hist-${String(i).padStart(4, '0')}`,
    userId: senderObj.userId,
    idempotencyKey: `idemp-demo-${i}`,
    senderAccountId: senderObj.accountId,
    receiverAccountId: receiverObj.accountId,
    amount,
    currency: "INR",
    status,
    failureReason,
    createdAt: pmtTime,
    updatedAt: pmtTime,
    timeline: [
      { id: `tl-${i}-1`, status: "INITIATED", timestamp: pmtTime, failureReason: null },
      { id: `tl-${i}-2`, status: isFailed && failureReason !== "INSUFFICIENT_FUNDS" ? "FAILED" : "VALIDATED", timestamp: pmtTime, failureReason: isFailed && failureReason !== "INSUFFICIENT_FUNDS" ? failureReason : null },
      ...(status === "SETTLED" ? [{ id: `tl-${i}-3`, status: "SETTLED", timestamp: pmtTime, failureReason: null }] : []),
      ...(isFailed && failureReason === "INSUFFICIENT_FUNDS" ? [{ id: `tl-${i}-3`, status: "FAILED", timestamp: pmtTime, failureReason }] : [])
    ]
  });
}

let mockAuditLogs = [
  {
    id: "audit-demo-1",
    eventId: "evt-01",
    eventType: "PaymentSettled",
    aggregateId: mockPayments[0].id,
    timestamp: mockPayments[0].createdAt,
    payload: JSON.stringify({ amount: mockPayments[0].amount, status: "SETTLED" }),
    correlationId: "corr-demo-001"
  }
];

// Helper: authenticated fetch with auto-refresh on 401
async function authFetch(url, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(inMemoryTokens.accessToken ? { 'Authorization': `Bearer ${inMemoryTokens.accessToken}` } : {}),
    ...(options.headers || {})
  };

  let response = await fetch(url, { ...options, headers });

  if (response.status === 401 && inMemoryTokens.refreshToken) {
    try {
      const refreshRes = await fetch('/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: inMemoryTokens.refreshToken })
      });
      if (refreshRes.ok) {
        const refreshData = await refreshRes.json();
        setTokens({
          accessToken: refreshData.accessToken,
          refreshToken: refreshData.refreshToken
        });
        headers['Authorization'] = `Bearer ${refreshData.accessToken}`;
        response = await fetch(url, { ...options, headers });
      } else {
        clearTokens();
        if (onAuthExpiredCallback) onAuthExpiredCallback();
      }
    } catch {
      clearTokens();
      if (onAuthExpiredCallback) onAuthExpiredCallback();
    }
  } else if (response.status === 401) {
    clearTokens();
    if (onAuthExpiredCallback) onAuthExpiredCallback();
  }

  return response;
}

// ==========================================
// EXPORTED API CLIENT
// ==========================================

export const api = {
  // Auth: Login
  login: async (username, password) => {
    if (getMockMode()) {
      await new Promise(r => setTimeout(r, 250));
      const user = mockUsers.find(u => u.username.toLowerCase() === (username || '').toLowerCase());
      if (!user) {
        throw new Error("Invalid username or password");
      }
      if (user.status === 'LOCKED') {
        throw new Error("Account is temporarily locked due to security policy or excessive failed login attempts.");
      }
      currentAuthenticatedUser = user;
      const payload = {
        accessToken: `mock-jwt-${user.role.toLowerCase()}-${user.id}`,
        refreshToken: crypto.randomUUID(),
        tokenType: "Bearer",
        expiresIn: 900,
        userId: user.id,
        username: user.username,
        email: user.email,
        fullName: user.fullName,
        role: user.role,
        status: user.status
      };
      setTokens({ accessToken: payload.accessToken, refreshToken: payload.refreshToken });
      return payload;
    }

    try {
      const res = await fetch('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Login failed');
      }
      const data = await res.json();
      currentAuthenticatedUser = {
        id: data.userId,
        username: data.username,
        email: data.email,
        fullName: data.fullName,
        role: data.role,
        status: data.status || 'ACTIVE'
      };
      setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
      return data;
    } catch (e) {
      console.warn("Backend login failed, fallback to mock login:", e.message);
      setMockMode(true);
      return api.login(username, password);
    }
  },

  // Auth: Register
  register: async ({ username, email, password, fullName }) => {
    if (getMockMode()) {
      await new Promise(r => setTimeout(r, 300));
      const exists = mockUsers.some(u => u.username.toLowerCase() === username.toLowerCase() || u.email.toLowerCase() === email.toLowerCase());
      if (exists) {
        throw new Error("Username or email already in use.");
      }
      const newUser = {
        id: crypto.randomUUID(),
        username,
        email,
        fullName,
        role: "USER",
        status: "ACTIVE",
        failedLoginAttempts: 0,
        createdAt: new Date().toISOString(),
        lastLoginAt: new Date().toISOString()
      };
      mockUsers.push(newUser);
      currentAuthenticatedUser = newUser;
      const payload = {
        accessToken: `mock-jwt-user-${newUser.id}`,
        refreshToken: crypto.randomUUID(),
        tokenType: "Bearer",
        expiresIn: 900,
        userId: newUser.id,
        username: newUser.username,
        email: newUser.email,
        fullName: newUser.fullName,
        role: newUser.role,
        status: newUser.status
      };
      setTokens({ accessToken: payload.accessToken, refreshToken: payload.refreshToken });
      return payload;
    }

    const res = await fetch('/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email, password, fullName })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Registration failed');
    }
    const data = await res.json();
    currentAuthenticatedUser = {
      id: data.userId,
      username: data.username,
      email: data.email,
      fullName: data.fullName,
      role: data.role,
      status: data.status || 'ACTIVE'
    };
    setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
    return data;
  },

  // Auth: Refresh
  refresh: async () => {
    if (getMockMode()) {
      const newAccess = `mock-refreshed-jwt-${Date.now()}`;
      const newRefresh = crypto.randomUUID();
      setTokens({ accessToken: newAccess, refreshToken: newRefresh });
      return { accessToken: newAccess, refreshToken: newRefresh };
    }
    if (!inMemoryTokens.refreshToken) throw new Error("No refresh token available");
    const res = await fetch('/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: inMemoryTokens.refreshToken })
    });
    if (!res.ok) throw new Error('Token refresh rejected');
    const data = await res.json();
    setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
    return data;
  },

  // Auth: Logout
  logout: async () => {
    if (!getMockMode() && inMemoryTokens.refreshToken) {
      try {
        await fetch('/auth/logout', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken: inMemoryTokens.refreshToken })
        });
      } catch {
        // Ignore network errors on logout
      }
    }
    clearTokens();
  },

  // Users: Get current profile
  getMe: async () => {
    if (getMockMode()) {
      return currentAuthenticatedUser || mockUsers[0];
    }
    const res = await authFetch('/api/users/me');
    if (!res.ok) throw new Error('Failed to fetch user profile');
    return await res.json();
  },

  // Users: Get all users (ADMIN only)
  getUsers: async () => {
    if (getMockMode()) {
      return [...mockUsers];
    }
    const res = await authFetch('/api/users');
    if (!res.ok) throw new Error('Failed to fetch users list');
    return await res.json();
  },

  // Users: Update status Lock/Unlock (ADMIN only)
  updateUserStatus: async (userId, status) => {
    if (getMockMode()) {
      await new Promise(r => setTimeout(r, 200));
      const u = mockUsers.find(item => item.id === userId);
      if (u) {
        u.status = status;
        if (status === 'ACTIVE') u.failedLoginAttempts = 0;
      }
      return u;
    }
    const res = await authFetch(`/api/users/${userId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to update user status');
    }
    return await res.json();
  },

  // Accounts: Fetch accounts
  getAccounts: async () => {
    if (getMockMode()) {
      if (currentAuthenticatedUser && currentAuthenticatedUser.role === 'USER') {
        return mockAccounts.filter(a => a.ownerUserId === currentAuthenticatedUser.id);
      }
      return [...mockAccounts];
    }
    try {
      const res = await authFetch('/api/accounts');
      if (!res.ok) throw new Error('Failed to fetch accounts');
      return await res.json();
    } catch (e) {
      console.warn("Backend getAccounts failed, fallback to mock:", e.message);
      if (currentAuthenticatedUser && currentAuthenticatedUser.role === 'USER') {
        return mockAccounts.filter(a => a.ownerUserId === currentAuthenticatedUser.id);
      }
      return [...mockAccounts];
    }
  },

  // Accounts: Create Account
  createAccount: async (arg1, arg2) => {
    const { ownerName, initialBalance, currency = 'INR' } = (arg2 !== undefined ? arg2 : arg1) || {};
    if (getMockMode()) {
      await new Promise(r => setTimeout(r, 300));
      const parsedBalance = parseFloat(initialBalance);
      const newAcc = {
        id: crypto.randomUUID(),
        ownerUserId: currentAuthenticatedUser ? currentAuthenticatedUser.id : RAHUL_ID,
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

    const res = await authFetch('/api/accounts', {
      method: 'POST',
      body: JSON.stringify({ ownerName, initialBalance, currency })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to create account');
    }
    return await res.json();
  },

  // Accounts: Get transactions / ledger entries
  getTransactions: async (arg1, arg2) => {
    const accountId = arg2 !== undefined ? arg2 : arg1;
    if (getMockMode()) {
      return mockLedgerEntries
        .filter(l => l.accountId === accountId)
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }
    try {
      const res = await authFetch(`/api/accounts/${accountId}/transactions`);
      if (!res.ok) throw new Error('Failed to fetch transactions');
      return await res.json();
    } catch (e) {
      console.warn("Backend getTransactions failed, fallback to mock:", e.message);
      return mockLedgerEntries.filter(l => l.accountId === accountId);
    }
  },

  // Accounts: Export statement CSV
  exportStatementCsv: async (arg1, arg2) => {
    const accountId = arg2 !== undefined ? arg2 : arg1;
    if (getMockMode()) {
      const acc = mockAccounts.find(a => a.id === accountId) || { ownerName: "Account" };
      const entries = mockLedgerEntries.filter(l => l.accountId === accountId);
      let csv = "TransactionId,AccountId,OwnerName,Timestamp,Type,Amount,BalanceAfter,ReferenceId,Description\n";
      for (const e of entries) {
        csv += `${e.id},${e.accountId},"${acc.ownerName}",${e.createdAt},${e.entryType},${e.amount},${e.balanceAfter},"${e.referenceId || ''}","${e.description || ''}"\n`;
      }
      return csv;
    }

    const res = await authFetch(`/api/accounts/${accountId}/statement/csv`);
    if (!res.ok) throw new Error('Failed to download statement');
    return await res.text();
  },

  // Payments: Fetch payments
  getPayments: async (arg1, arg2) => {
    const params = (typeof arg1 === 'object' && arg2 === undefined) ? arg1 : (arg2 || {});
    if (getMockMode()) {
      let filtered = [...mockPayments];
      if (currentAuthenticatedUser && currentAuthenticatedUser.role === 'USER') {
        filtered = filtered.filter(p => p.userId === currentAuthenticatedUser.id);
      }
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
      const res = await authFetch(`/api/payments${qs}`);
      if (!res.ok) throw new Error('Failed to fetch payments');
      const data = await res.json();
      return Array.isArray(data) ? data.reverse() : (data.content ? data.content : []);
    } catch (e) {
      console.warn("Backend getPayments failed, fallback to mock:", e.message);
      let filtered = [...mockPayments];
      if (currentAuthenticatedUser && currentAuthenticatedUser.role === 'USER') {
        filtered = filtered.filter(p => p.userId === currentAuthenticatedUser.id);
      }
      return filtered.reverse();
    }
  },

  // Payments: Get timeline
  getPaymentTimeline: async (arg1, arg2) => {
    const paymentId = arg2 !== undefined ? arg2 : arg1;
    if (getMockMode()) {
      const p = mockPayments.find(item => item.id === paymentId);
      return p ? (p.timeline || []) : [];
    }
    try {
      const res = await authFetch(`/api/payments/${paymentId}/timeline`);
      if (!res.ok) throw new Error('Failed to fetch payment timeline');
      return await res.json();
    } catch (e) {
      console.warn("Backend getPaymentTimeline failed:", e.message);
      const p = mockPayments.find(item => item.id === paymentId);
      return p ? (p.timeline || []) : [];
    }
  },

  // Payments: Initiate transfer
  initiatePayment: async (arg1, arg2) => {
    const { idempotencyKey, senderAccountId, receiverAccountId, amount, currency = 'INR' } = (arg2 !== undefined ? arg2 : arg1) || {};
    if (getMockMode()) {
      if (currentAuthenticatedUser && currentAuthenticatedUser.role === 'ADMIN') {
        throw new Error("Administrators cannot initiate payments on behalf of users (Role-Based Separation of Duties).");
      }

      await new Promise(r => setTimeout(r, 400));
      const parsedAmount = parseFloat(amount);
      const sender = mockAccounts.find(a => a.id === senderAccountId);
      const receiver = mockAccounts.find(a => a.id === receiverAccountId);

      const payment = {
        id: crypto.randomUUID(),
        userId: currentAuthenticatedUser ? currentAuthenticatedUser.id : RAHUL_ID,
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

      // Async lifecycle simulation (Kafka event choreography)
      setTimeout(() => {
        if (parsedAmount > 50000) {
          payment.status = 'FAILED';
          payment.failureReason = 'EXCEEDS_MAX_AMOUNT_LIMIT';
          payment.updatedAt = new Date().toISOString();
          payment.timeline.push({ id: crypto.randomUUID(), status: 'FAILED', timestamp: new Date().toISOString(), failureReason: payment.failureReason });
        } else {
          payment.status = 'VALIDATED';
          payment.updatedAt = new Date().toISOString();
          payment.timeline.push({ id: crypto.randomUUID(), status: 'VALIDATED', timestamp: new Date().toISOString(), failureReason: null });

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
          }, 1200);
        }
      }, 800);

      return payment;
    }

    const res = await authFetch('/api/payments', {
      method: 'POST',
      headers: {
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

  // Audit Logs
  getAuditLogs: async (arg1, arg2) => {
    const params = (typeof arg1 === 'object' && arg2 === undefined) ? arg1 : (arg2 || {});
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
      const res = await authFetch(`/api/audit-logs${qs}`);
      if (!res.ok) throw new Error('Failed to fetch audit logs');
      return await res.json();
    } catch (e) {
      console.warn("Backend getAuditLogs failed, fallback to mock:", e.message);
      return { content: mockAuditLogs, totalElements: mockAuditLogs.length, totalPages: 1 };
    }
  }
};
