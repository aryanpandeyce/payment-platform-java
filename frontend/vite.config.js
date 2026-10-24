import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  base: './', // GitHub Pages safe relative base
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
});
