import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '@/auth/AuthContext';
import { ToastProvider } from '@/components/Toast';
import DonationDetailsPage from '@/pages/shared/DonationDetailsPage';
import { claimApi, donationApi, profileApi } from '@/api/endpoints';
const donation = {
    id: 10,
    referenceCode: 'FB-2026-0010',
    foodName: 'Vegetable Biryani',
    description: 'Freshly cooked at 6pm',
    quantity: 12,
    quantityUnit: 'KG',
    foodType: 'VEGETARIAN',
    category: { id: 1, name: 'Cooked Meals', slug: 'cooked-meals' },
    preparedAt: new Date(Date.now() - 3_600_000).toISOString(),
    expiresAt: new Date(Date.now() + 5 * 3_600_000).toISOString(),
    pickupAddress: '12 FC Road',
    city: 'Pune',
    pickupInstructions: 'Ring the bell at the service door',
    storageConditions: 'Keep refrigerated',
    allergenInfo: 'Contains dairy',
    handlingInstructions: 'Use clean containers',
    safetyDeclaration: true,
    estimatedMeals: 30,
    status: 'AVAILABLE',
    expired: false,
    minutesUntilExpiry: 300,
    donorId: 1,
    donorName: 'Aarti Sharma',
    donorEmail: 'donor@foodbridge.dev',
    donorPhone: '+91 98765 43210',
    imageUrls: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
};
vi.mock('@/api/endpoints', async () => {
    const actual = await vi.importActual('@/api/endpoints');
    return {
        ...actual,
        donationApi: { ...actual.donationApi, get: vi.fn() },
        profileApi: { ...actual.profileApi, get: vi.fn() },
        claimApi: { ...actual.claimApi, claim: vi.fn(), mine: vi.fn(async () => ({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true })) },
    };
});
function renderDetails() {
    return render(_jsx(ToastProvider, { children: _jsx(AuthProvider, { children: _jsx(MemoryRouter, { initialEntries: ['/ngo/donations/10'], children: _jsxs(Routes, { children: [_jsx(Route, { path: "/ngo/donations/:id", element: _jsx(DonationDetailsPage, { role: "NGO" }) }), _jsx(Route, { path: "/ngo/claims", element: _jsx("p", { children: "Claims list" }) })] }) }) }) }));
}
describe('DonationDetailsPage — NGO claiming', () => {
    it('shows full food-safety information to the NGO', async () => {
        vi.mocked(donationApi.get).mockResolvedValue(donation);
        vi.mocked(profileApi.get).mockResolvedValue({
            user: { id: 2, email: 'ngo@foodbridge.dev', fullName: 'Ravi Menon', role: 'NGO', status: 'ACTIVE', emailVerified: true, createdAt: '2026-09-01T00:00:00Z' },
            donorProfile: null,
            ngoProfile: { id: 1, organizationName: 'Annapurna Trust', registrationNumber: 'MH/2019/001', contactPerson: 'Ravi Menon', city: 'Pune', state: 'Maharashtra', postalCode: '411001', addressLine1: 'MG Road', verificationStatus: 'APPROVED', verifiedAt: '2026-09-05T00:00:00Z' },
        });
        renderDetails();
        expect(await screen.findByRole('heading', { name: 'Vegetable Biryani' })).toBeInTheDocument();
        expect(screen.getByText('Contains dairy')).toBeInTheDocument();
        expect(screen.getByText('Keep refrigerated')).toBeInTheDocument();
        expect(screen.getByText('Ring the bell at the service door')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /claim donation/i })).toBeInTheDocument();
    });
    it('requires the suitability confirmation before the claim is sent', async () => {
        vi.mocked(donationApi.get).mockResolvedValue(donation);
        vi.mocked(profileApi.get).mockResolvedValue({
            user: { id: 2, email: 'ngo@foodbridge.dev', fullName: 'Ravi Menon', role: 'NGO', status: 'ACTIVE', emailVerified: true, createdAt: '2026-09-01T00:00:00Z' },
            donorProfile: null,
            ngoProfile: { id: 1, organizationName: 'Annapurna Trust', registrationNumber: 'MH/2019/001', contactPerson: 'Ravi Menon', city: 'Pune', state: 'Maharashtra', postalCode: '411001', addressLine1: 'MG Road', verificationStatus: 'APPROVED', verifiedAt: '2026-09-05T00:00:00Z' },
        });
        vi.mocked(claimApi.claim).mockReset();
        const user = userEvent.setup();
        renderDetails();
        await user.click(await screen.findByRole('button', { name: /claim donation/i }));
        // The confirm button stays disabled until the suitability box is ticked.
        const confirm = screen.getByRole('button', { name: /confirm claim/i });
        expect(confirm).toBeDisabled();
        await user.click(screen.getByRole('checkbox'));
        expect(confirm).toBeEnabled();
        vi.mocked(claimApi.claim).mockResolvedValue({ id: 5 });
        await user.click(confirm);
        await waitFor(() => expect(claimApi.claim).toHaveBeenCalledWith(10, expect.objectContaining({ suitabilityCheck: true })));
    });
});
