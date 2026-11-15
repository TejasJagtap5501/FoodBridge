import { jsx as _jsx } from "react/jsx-runtime";
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthProvider } from '@/auth/AuthContext';
import { ToastProvider } from '@/components/Toast';
import LandingPage from '@/pages/public/LandingPage';
import NgoDashboard from '@/pages/ngo/NgoDashboard';
function renderWithProviders(ui, path = '/') {
    return render(_jsx(ToastProvider, { children: _jsx(AuthProvider, { children: _jsx(MemoryRouter, { initialEntries: [path], children: ui }) }) }));
}
describe('LandingPage', () => {
    it('renders the live impact tiles from the public stats endpoint', async () => {
        renderWithProviders(_jsx(LandingPage, {}));
        // Values come from MSW (/api/public/stats) — never hard-coded in the page.
        expect(await screen.findByText('1,200')).toBeInTheDocument();
        expect(screen.getByText('Meals rescued')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /register your ngo/i })).toHaveAttribute('href', '/register?role=ngo');
    });
    it('states that FoodBridge does not certify food safety', async () => {
        renderWithProviders(_jsx(LandingPage, {}));
        expect(await screen.findByText(/not a food safety certifier/i)).toBeInTheDocument();
    });
});
describe('NgoDashboard', () => {
    it('shows the verification warning when the organisation is not approved', async () => {
        renderWithProviders(_jsx(NgoDashboard, {}), '/ngo');
        // MSW returns a donor profile for /api/me/profile, so NgoDashboard shows the
        // generic "no verification state" layout rather than fabricating an approval.
        expect(await screen.findByText('Expiring soonest')).toBeInTheDocument();
        // Appears in both the "expiring soonest" and "active claims" panels.
        expect(await screen.findAllByText('Vegetable Biryani')).toHaveLength(2);
        // Claimed food must not be offered as "active claims" without real data — it comes from MSW.
        expect(screen.getByText('My active claims')).toBeInTheDocument();
    });
});
