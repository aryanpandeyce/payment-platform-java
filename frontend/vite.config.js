import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  plugins: [react()],
  // GitHub Pages runs under repository subpath: /payment-platform-java/
  base: process.env.VITE_BASE_PATH || (mode === 'production' ? '/payment-platform-java/' : '/'),
  server: {
    port: 5173,
    proxy: {
      '/auth': {
        target: 'http://localhost:8090',
        changeOrigin: true
      },
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true
      }
    }
  }
}));
