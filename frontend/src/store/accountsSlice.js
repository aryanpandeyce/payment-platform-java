import { createSlice } from '@reduxjs/toolkit';

const accountsSlice = createSlice({
  name: 'accounts',
  initialState: {
    list: [],
    loading: false,
    error: null
  },
  reducers: {
    setAccounts: (state, action) => {
      state.list = action.payload;
      state.loading = false;
      state.error = null;
    },
    addAccount: (state, action) => {
      state.list.push(action.payload);
    },
    setAccountsLoading: (state, action) => {
      state.loading = action.payload;
    },
    setAccountsError: (state, action) => {
      state.error = action.payload;
      state.loading = false;
    }
  }
});

export const { setAccounts, addAccount, setAccountsLoading, setAccountsError } = accountsSlice.actions;
export default accountsSlice.reducer;
