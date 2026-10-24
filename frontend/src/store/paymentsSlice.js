import { createSlice } from '@reduxjs/toolkit';

const paymentsSlice = createSlice({
  name: 'payments',
  initialState: {
    list: [],
    isSubmitting: false,
    lastSubmittedKey: null,
    error: null,
    successMessage: null
  },
  reducers: {
    setPayments: (state, action) => {
      state.list = action.payload;
    },
    paymentInitiated: (state, action) => {
      state.list.unshift(action.payload);
      state.isSubmitting = false;
      state.lastSubmittedKey = action.payload.idempotencyKey;
      state.successMessage = `Payment #${action.payload.id.substring(0, 8)} initiated!`;
      state.error = null;
    },
    setSubmitting: (state, action) => {
      state.isSubmitting = action.payload;
      if (action.payload) {
        state.error = null;
        state.successMessage = null;
      }
    },
    setPaymentError: (state, action) => {
      state.error = action.payload;
      state.isSubmitting = false;
    },
    clearMessages: (state) => {
      state.error = null;
      state.successMessage = null;
    }
  }
});

export const { setPayments, paymentInitiated, setSubmitting, setPaymentError, clearMessages } = paymentsSlice.actions;
export default paymentsSlice.reducer;
