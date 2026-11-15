import { jsx as _jsx } from "react/jsx-runtime";
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '@/components/Toast';
import DonationFormPage from '@/pages/donor/DonationFormPage';
import { donationApi } from '@/api/endpoints';
// The form posts through the real axios client; the endpoint module is mocked so
// the assertions stay about validation behaviour, not HTTP plumbing.
vi.mock('@/api/endpoints', async () => {
    const actual = await vi.importActual('@/api/endpoints');
    return {
        ...actual,
        donationApi: {
            ...actual.donationApi,
            create: vi.fn(),
            get: vi.fn(),
        },
        catalogApi: {
            categories: vi.fn(async () => [
                { id: 1, name: 'Cooked Meals', slug: 'cooked-meals', description: null, active: true, displayOrder: 1 },
            ]),
        },
    };
});
function renderForm() {
    return render(_jsx(ToastProvider, { children: _jsx(MemoryRouter, { initialEntries: ['/donor/donations/new'], children: _jsx(DonationFormPage, {}) }) }));
}
describe('DonationFormPage validation', () => {
    beforeEach(() => {
        vi.mocked(donationApi.create).mockReset();
        vi.mocked(donationApi.create).mockResolvedValue({
            id: 99, referenceCode: 'FB-2026-0099',
        });
    });
    it('blocks submission and shows field errors when required data is missing', async () => {
        const user = userEvent.setup();
        renderForm();
        await user.click(screen.getByRole('button', { name: /publish donation/i }));
        expect(await screen.findByText('Food name is required')).toBeInTheDocument();
        expect(screen.getByText('Select a food category')).toBeInTheDocument();
        expect(screen.getByText('Pickup address is required')).toBeInTheDocument();
        expect(screen.getByText('You must confirm the food safety declaration')).toBeInTheDocument();
        expect(donationApi.create).not.toHaveBeenCalled();
    });
    it('rejects a non-positive quantity', async () => {
        const user = userEvent.setup();
        renderForm();
        const quantity = screen.getByLabelText(/quantity/i);
        await user.clear(quantity);
        await user.type(quantity, '0');
        await user.click(screen.getByRole('button', { name: /publish donation/i }));
        expect(await screen.findByText('Quantity must be greater than zero')).toBeInTheDocument();
        expect(donationApi.create).not.toHaveBeenCalled();
    });
    it('submits a complete, valid donation', async () => {
        const user = userEvent.setup();
        renderForm();
        await user.type(screen.getByLabelText(/food name/i), 'Vegetable Biryani');
        const quantity = screen.getByLabelText(/quantity/i);
        await user.clear(quantity);
        await user.type(quantity, '12');
        await user.selectOptions(screen.getByLabelText(/food category/i), '1');
        await user.type(screen.getByLabelText(/pickup address/i), '12 FC Road, Shivajinagar');
        await user.type(screen.getByLabelText(/^city/i), 'Pune');
        await user.click(screen.getByLabelText(/I confirm that the information provided/i));
        await user.click(screen.getByRole('button', { name: /publish donation/i }));
        await waitFor(() => expect(donationApi.create).toHaveBeenCalledTimes(1));
        const payload = vi.mocked(donationApi.create).mock.calls[0][0];
        expect(payload).toMatchObject({
            foodName: 'Vegetable Biryani',
            quantity: 12,
            categoryId: 1,
            city: 'Pune',
            safetyDeclaration: true,
        });
        // Instants must be serialised as ISO-8601, never as local input strings.
        expect(String(payload.expiresAt)).toMatch(/Z$|([+-]\d{2}:\d{2})$/);
    });
});
