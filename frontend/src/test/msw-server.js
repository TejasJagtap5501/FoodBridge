import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
/**
 * Mock backend for component tests. Handlers return shapes that match the real
 * API response fixtures mirror the backend DTOs so the request/response contract is exercised in tests.
 */
export const donorUser = {
    id: 1,
    email: 'donor@foodbridge.dev',
    fullName: 'Aarti Sharma',
    phone: '+91 98765 43210',
    role: 'DONOR',
    status: 'ACTIVE',
    emailVerified: true,
    lastLoginAt: '2026-11-01T09:15:00Z',
    createdAt: '2026-09-01T08:00:00Z',
};
export const ngoUser = {
    id: 2,
    email: 'ngo@foodbridge.dev',
    fullName: 'Ravi Menon',
    role: 'NGO',
    status: 'ACTIVE',
    emailVerified: true,
    createdAt: '2026-09-02T08:00:00Z',
};
export const adminUser = {
    id: 3,
    email: 'admin@foodbridge.dev',
    fullName: 'Platform Admin',
    role: 'ADMIN',
    status: 'ACTIVE',
    emailVerified: true,
    createdAt: '2026-08-01T08:00:00Z',
};
export function authResponse(user) {
    return { accessToken: `test-token-${user.id}`, tokenType: 'Bearer', expiresIn: 900, user };
}
export const donationSummary = {
    id: 10,
    referenceCode: 'FB-2026-0010',
    foodName: 'Vegetable Biryani',
    categoryName: 'Cooked Meals',
    quantity: 12,
    quantityUnit: 'KG',
    foodType: 'VEGETARIAN',
    city: 'Pune',
    expiresAt: new Date(Date.now() + 4 * 3_600_000).toISOString(),
    status: 'AVAILABLE',
    estimatedMeals: 30,
    donorId: 1,
};
export const claim = {
    id: 5,
    status: 'ACTIVE',
    message: 'We can collect at 7pm',
    suitabilityCheck: true,
    claimedAt: '2026-11-05T10:00:00Z',
    donationId: 10,
    donationReference: 'FB-2026-0010',
    foodName: 'Vegetable Biryani',
    categoryName: 'Cooked Meals',
    quantity: 12,
    quantityUnit: 'KG',
    city: 'Pune',
    expiresAt: new Date(Date.now() + 4 * 3_600_000).toISOString(),
    pickupAddress: '12 FC Road',
    donorId: 1,
    donorName: 'Aarti Sharma',
    donorEmail: 'donor@foodbridge.dev',
    ngoId: 2,
    ngoName: 'Ravi Menon',
    ngoEmail: 'ngo@foodbridge.dev',
    pickup: null,
};
export const categories = [
    { id: 1, name: 'Cooked Meals', slug: 'cooked-meals', description: 'Ready to eat', active: true, displayOrder: 1 },
    { id: 2, name: 'Bakery', slug: 'bakery', description: 'Bread and pastries', active: true, displayOrder: 2 },
];
export const donationStats = {
    totalDonations: 12,
    available: 4,
    claimed: 2,
    completed: 5,
    expired: 1,
    cancelled: 0,
    estimatedMealsContributed: 210,
    mealsRescued: 88,
    pendingClaims: 2,
};
export const publicStats = {
    mealsServed: 1200,
    donationsCompleted: 40,
    donors: 6,
    ngos: 3,
    activeDonations: 5,
};
export const platformStats = {
    totalUsers: 9,
    totalDonors: 3,
    totalNgos: 5,
    pendingNgoVerifications: 1,
    approvedNgos: 4,
    rejectedNgos: 0,
    inactiveUsers: 0,
    totalDonations: 41,
    availableDonations: 6,
    claimedDonations: 3,
    completedDonations: 18,
    expiredDonations: 12,
    cancelledDonations: 2,
    activeClaims: 3,
    totalClaims: 21,
    completedPickups: 18,
    estimatedMealsServed: 267,
    donationsCompletedLast7Days: 4,
    newUsersLast7Days: 1,
};
export const donorProfile = {
    user: donorUser,
    donorProfile: {
        id: 1,
        businessName: 'Spice Route Kitchen',
        businessType: 'RESTAURANT',
        contactPerson: 'Aarti Sharma',
        addressLine1: '12 FC Road',
        addressLine2: null,
        city: 'Pune',
        state: 'Maharashtra',
        postalCode: '411005',
        fssaiLicense: '12345678901234',
    },
    ngoProfile: null,
};
function page(content) {
    return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1, first: true, last: true, sort: null };
}
export const handlers = [
    http.post('*/api/auth/login', async ({ request }) => {
        const body = (await request.json());
        if (body.password === 'wrong') {
            return HttpResponse.json({ timestamp: new Date().toISOString(), status: 401, error: 'Unauthorized', code: 'BAD_CREDENTIALS', message: 'Invalid email or password', path: '/api/auth/login' }, { status: 401 });
        }
        const user = body.email.includes('ngo') ? ngoUser : body.email.includes('admin') ? adminUser : donorUser;
        return HttpResponse.json(authResponse(user));
    }),
    http.post('*/api/auth/refresh', () => new HttpResponse(null, { status: 401 })),
    http.post('*/api/auth/register/donor', () => HttpResponse.json(authResponse(donorUser), { status: 201 })),
    http.post('*/api/auth/register/ngo', () => HttpResponse.json(authResponse(ngoUser), { status: 201 })),
    http.post('*/api/auth/logout', () => HttpResponse.json({ message: 'Signed out' })),
    http.get('*/api/donations/stats', () => HttpResponse.json(donationStats)),
    http.get('*/api/donations/mine', () => HttpResponse.json(page([donationSummary]))),
    http.get('*/api/donations/available', () => HttpResponse.json(page([donationSummary]))),
    http.get('*/api/claims', () => HttpResponse.json(page([claim]))),
    http.get('*/api/claims/stats', () => HttpResponse.json({ totalClaims: 3, activeClaims: 1, completedClaims: 2, cancelledClaims: 0 })),
    http.get('*/api/pickups', () => HttpResponse.json([])),
    http.get('*/api/categories', () => HttpResponse.json(categories)),
    http.get('*/api/public/stats', () => HttpResponse.json(publicStats)),
    http.get('*/api/me', () => HttpResponse.json(donorUser)),
    http.get('*/api/me/profile', () => HttpResponse.json(donorProfile)),
    http.get('*/api/notifications', () => HttpResponse.json(page([]))),
    http.get('*/api/notifications/unread-count', () => HttpResponse.json({ count: 0 })),
    http.get('*/api/admin/stats', () => HttpResponse.json(platformStats)),
    http.get('*/api/admin/activity', () => HttpResponse.json([])),
    http.get('*/api/admin/reports', () => HttpResponse.json({
        donationsPerDay: [{ date: '2026-11-01', count: 2 }],
        mealsPerDay: [{ date: '2026-11-01', count: 10 }],
        categoryBreakdown: [],
        topCities: [],
        topDonors: [],
        totalMealsServed: 10,
        totalQuantityRescued: 4,
    })),
];
export const server = setupServer(...handlers);
