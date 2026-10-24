import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';
import accountsReducer from './accountsSlice';
import paymentsReducer from './paymentsSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    accounts: accountsReducer,
    payments: paymentsReducer
  }
});
