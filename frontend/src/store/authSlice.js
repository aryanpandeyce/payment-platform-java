import { createSlice } from '@reduxjs/toolkit';
import { getMockMode } from '../services/api';

const authSlice = createSlice({
  name: 'auth',
  initialState: {
    token: null,           // In-memory access token (JWT), NEVER stored in localStorage
    refreshToken: null,    // In-memory refresh token, NEVER stored in localStorage
    user: null,            // { id, username, email, fullName, role, status }
    isAuthenticated: false,
    isMockMode: getMockMode(),
    loading: false,
    error: null
  },
  reducers: {
    loginSuccess: (state, action) => {
      state.token = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      state.user = {
        id: action.payload.userId,
        username: action.payload.username,
        email: action.payload.email,
        fullName: action.payload.fullName,
        role: action.payload.role,
        status: action.payload.status || 'ACTIVE'
      };
      state.isAuthenticated = true;
      state.loading = false;
      state.error = null;
    },
    tokenRefreshed: (state, action) => {
      state.token = action.payload.accessToken;
      if (action.payload.refreshToken) {
        state.refreshToken = action.payload.refreshToken;
      }
    },
    updateUserProfile: (state, action) => {
      if (state.user) {
        state.user = { ...state.user, ...action.payload };
      }
    },
    logout: (state) => {
      state.token = null;
      state.refreshToken = null;
      state.user = null;
      state.isAuthenticated = false;
      state.loading = false;
      state.error = null;
    },
    setMockModeState: (state, action) => {
      state.isMockMode = action.payload;
    },
    setLoading: (state, action) => {
      state.loading = action.payload;
    },
    setError: (state, action) => {
      state.error = action.payload;
      state.loading = false;
    }
  }
});

export const {
  loginSuccess,
  tokenRefreshed,
  updateUserProfile,
  logout,
  setMockModeState,
  setLoading,
  setError
} = authSlice.actions;

export default authSlice.reducer;
