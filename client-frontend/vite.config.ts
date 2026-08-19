import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const backendTarget = loadEnv(mode, '.', '').VITE_BACKEND_TARGET || 'http://localhost:8080';
  return {
    plugins: [react()],
    server: {
      host: 'localhost',
      port: 5174,
      strictPort: true,
      proxy: {
        '/api': { target: backendTarget, changeOrigin: true },
        '/oauth2': { target: backendTarget, changeOrigin: true },
        '/logout': { target: backendTarget, changeOrigin: true },
      },
    },
    preview: {
      host: 'localhost',
      port: 5174,
      strictPort: true,
    },
  };
});
