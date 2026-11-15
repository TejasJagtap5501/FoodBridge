import { jsx as _jsx } from "react/jsx-runtime";
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { AuthProvider } from './auth/AuthContext';
import { ToastProvider } from './components/Toast';
import './index.css';
const container = document.getElementById('root');
if (!container) {
    throw new Error('Root element #root not found');
}
createRoot(container).render(_jsx(StrictMode, { children: _jsx(BrowserRouter, { children: _jsx(ToastProvider, { children: _jsx(AuthProvider, { children: _jsx(App, {}) }) }) }) }));
