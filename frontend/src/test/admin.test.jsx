import { jsx as _jsx } from "react/jsx-runtime";
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthProvider } from '@/auth/AuthContext';
import { ToastProvider } from '@/components/Toast';
import AdminDashboard from '@/pages/admin/AdminDashboard';
function renderAdmin() {
    return render(_jsx(ToastProvider, { children: _jsx(AuthProvider, { children: _jsx(MemoryRouter, { initialEntries: ['/admin'], children: _jsx(AdminDashboard, {}) }) }) }));
}
describe('AdminDashboard', () => {
    it('renders platform totals returned by the API, not constants', async () => {
        renderAdmin();
        // Values come from MSW's /api/admin/stats handler.
        expect(await screen.findByText('Total users')).toBeInTheDocument();
        expect(screen.getByText('9')).toBeInTheDocument(); // totalUsers
        expect(screen.getByText('267')).toBeInTheDocument(); // estimatedMealsServed
        expect(screen.getByText('Pending verifications')).toBeInTheDocument();
        expect(screen.getByText('Action needed')).toBeInTheDocument(); // 1 pending NGO
    });
    it('links to the verification queue and reports', async () => {
        renderAdmin();
        expect(await screen.findByRole('link', { name: /review ngos/i })).toHaveAttribute('href', '/admin/ngo-verification');
        expect(screen.getByRole('link', { name: /^reports$/i })).toHaveAttribute('href', '/admin/reports');
    });
});
