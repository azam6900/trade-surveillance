/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },   // fail instead of silently moving to 5174
  test: {
    environment: 'jsdom',                      // a fake browser DOM for tests
    setupFiles: './src/setupTests.ts',
  },
});