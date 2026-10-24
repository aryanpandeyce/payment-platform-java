import { createSlice } from '@reduxjs/toolkit';
import { getMockMode } from '../services/api';

const initialToken = localStorage.getItem('apexpay_token') || (getMockMode() ? 'mock-demo-token' : null);
const initialUser = localStorage.getItem('apexpay_user') || 'admin';

const authSlice = createSlice({
  name: 'auth',
  initialState: {
    token: initialToken,
    username: initialUser,
    isAuthenticated: !!initialToken,
    isMockMode: getMockMode(),
    loading: false,
    error: null
  },
  reducers: {
    loginSuccess: (state, action) => {
      state.token = action.payload.token;
      state.username = action.payload.username;
      state.isAuthenticated = true;
      state.loading = false;
      state.error = null;
      localStorage.setItem('apexpay_token', action.payload.token);
      localStorage.setItem('apexpay_user', action.payload.username);
    },
    logout: (state) => {
      state.token = null;
      state.username = null;
      state.isAuthenticated = false;
      localStorage.removeItem('apexpay_token');
      localStorage.removeItem('apexpay_user');
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

export const { loginSuccess, logout, setMockModeState, setLoading, setError } = authSlice.actions;
export default authSlice.reducer;
