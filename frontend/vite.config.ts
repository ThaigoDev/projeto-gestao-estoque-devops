import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Em desenvolvimento, as chamadas para /api sao repassadas para a API (porta 8080).
// Assim o navegador fala so com o frontend e nao ha problema de CORS.
// Para apontar para outra API: API_URL=http://outro-host:8080 npm run dev
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.API_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
