import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthProvider, useAuth, homeForRole } from '@/auth/AuthContext';
import { ProtectedRoute } from '@/auth/ProtectedRoute';
import { ToastProvider } from '@/components/Toast';
import LoginPage from '@/pages/public/LoginPage';
function renderLogin() {
    return render(_jsx(ToastProvider, { children: _jsx(AuthProvider, { children: _jsx(MemoryRouter, { initialEntries: ['/login'], children: _jsxs(Routes, { children: [_jsx(Route, { path: "/login", element: _jsx(LoginPage, {}) }), _jsx(Route, { path: "/donor", element: _jsx("p", { children: "Donor dashboard" }) }), _jsx(Route, { path: "/ngo", element: _jsx("p", { children: "NGO dashboard" }) })] }) }) }) }));
}
describe('AuthContext', () => {
    it('maps each role to its own landing route', () => {
        expect(homeForRole('DONOR')).toBe('/donor');
        expect(homeForRole('NGO')).toBe('/ngo');
        expect(homeForRole('ADMIN')).toBe('/admin');
        expect(homeForRole(undefined)).toBe('/');
    });
    it('starts signed out when the refresh cookie is absent', async () => {
        function Probe() {
            const { user, loading } = useAuth();
            if (loading)
                return _jsx("p", { children: "loading" });
            return _jsx("p", { children: user ? `signed in as ${user.email}` : 'signed out' });
        }
        render(_jsx(AuthProvider, { children: _jsx(Probe, {}) }));
        expect(await screen.findByText('signed out')).toBeInTheDocument();
    });
    it('signs a donor in and redirects to the donor dashboard', async () => {
        const user = userEvent.setup();
        renderLogin();
        await user.type(screen.getByLabelText(/email/i), 'donor@foodbridge.dev');
        await user.type(screen.getByLabelText(/password/i), 'Donor@12345');
        await user.click(screen.getByRole('button', { name: /sign in/i }));
        expect(await screen.findByText('Donor dashboard')).toBeInTheDocument();
    });
    it('surfaces the backend error message for bad credentials', async () => {
        const user = userEvent.setup();
        renderLogin();
        await user.type(screen.getByLabelText(/email/i), 'donor@foodbridge.dev');
        await user.type(screen.getByLabelText(/password/i), 'wrong');
        await user.click(screen.getByRole('button', { name: /sign in/i }));
        expect(await screen.findByRole('alert')).toHaveTextContent(/invalid email or password/i);
        expect(screen.queryByText('Donor dashboard')).not.toBeInTheDocument();
    });
});
describe('ProtectedRoute', () => {
    it('redirects anonymous visitors to the login page', async () => {
        render(_jsx(AuthProvider, { children: _jsx(MemoryRouter, { initialEntries: ['/donor'], children: _jsxs(Routes, { children: [_jsx(Route, { path: "/login", element: _jsx("p", { children: "Login screen" }) }), _jsx(Route, { path: "/donor", element: _jsx(ProtectedRoute, { roles: ['DONOR'], children: _jsx("p", { children: "Secret donor area" }) }) })] }) }) }));
        await waitFor(() => expect(screen.getByText('Login screen')).toBeInTheDocument());
        expect(screen.queryByText('Secret donor area')).not.toBeInTheDocument();
    });
});
