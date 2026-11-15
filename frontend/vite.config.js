import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
// The dev server proxies /api and /uploads to the Spring Boot backend so the
// browser only ever talks to one origin (no CORS surprises in development).
export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: { '@': resolve(__dirname, './src') },
    },
    server: {
        host: '0.0.0.0',
        port: 5173,
        strictPort: false,
        proxy: {
            '/api': { target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true },
            '/uploads': { target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true },
        },
    },
    preview: { host: '0.0.0.0', port: 4173 },
    test: {
        environment: 'jsdom',
        globals: true,
        setupFiles: ['./src/test/setup.js'],
        css: false,
    },
});
