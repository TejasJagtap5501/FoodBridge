import { api, http } from './client';
/** Query-string helper that drops empty values. */
function qs(params) {
    const search = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
            search.set(key, String(value));
        }
    });
    const query = search.toString();
    return query ? `?${query}` : '';
}
export const authApi = {
    loginDonor: (email, password) => api.post('/api/auth/login', { email, password }),
    login: (email, password) => api.post('/api/auth/login', { email, password }),
    registerDonor: (payload) => api.post('/api/auth/register/donor', payload),
    registerNgo: (payload) => api.post('/api/auth/register/ngo', payload),
    refresh: () => api.post('/api/auth/refresh'),
    logout: () => api.post('/api/auth/logout'),
    me: () => api.get('/api/me'),
    forgotPassword: (email) => api.post('/api/auth/forgot-password', { email }),
    resetPassword: (token, newPassword) => api.post('/api/auth/reset-password', { token, newPassword }),
    changePassword: (currentPassword, newPassword) => api.post('/api/me/change-password', { currentPassword, newPassword }),
};
export const profileApi = {
    get: () => api.get('/api/me/profile'),
    update: (payload) => api.put('/api/me/profile', payload),
};
export const catalogApi = {
    categories: (onlyActive = true) => api.get(`/api/categories${qs({ onlyActive })}`),
};
export const donationApi = {
    create: (payload) => api.post('/api/donations', payload),
    get: (id) => api.get(`/api/donations/${id}`),
    update: (id, payload) => api.put(`/api/donations/${id}`, payload),
    cancel: (id, reason) => api.post(`/api/donations/${id}/cancel`, { reason }),
    mine: (params) => api.get(`/api/donations/mine${qs(params)}`),
    available: (params) => api.get(`/api/donations/available${qs(params)}`),
    stats: () => api.get('/api/donations/stats'),
    uploadImage: (id, file) => {
        const form = new FormData();
        form.append('file', file);
        return api.post(`/api/donations/${id}/images`, form, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },
};
export const claimApi = {
    claim: (donationId, payload) => api.post(`/api/claims/donations/${donationId}`, payload),
    mine: (params) => api.get(`/api/claims${qs(params)}`),
    get: (id) => api.get(`/api/claims/${id}`),
    cancel: (id, reason) => api.post(`/api/claims/${id}/cancel`, { reason }),
    stats: () => api.get('/api/claims/stats'),
};
export const pickupApi = {
    schedule: (claimId, scheduledAt, instructions) => api.post(`/api/pickups/claims/${claimId}`, { scheduledAt, instructions }),
    mine: () => api.get('/api/pickups'),
    confirmHandover: (id) => api.post(`/api/pickups/${id}/confirm-handover`),
    confirmCollection: (id) => api.post(`/api/pickups/${id}/confirm-collection`),
    cancel: (id, reason) => api.post(`/api/pickups/${id}/cancel`, { reason }),
};
export const requestApi = {
    create: (payload) => api.post('/api/requests', payload),
    mine: (params) => api.get(`/api/requests/mine${qs(params)}`),
    open: (params) => api.get(`/api/requests/open${qs(params)}`),
    updateStatus: (id, status) => api.patch(`/api/requests/${id}/status?status=${status}`),
};
export const notificationApi = {
    list: (page = 0, size = 20) => api.get(`/api/notifications${qs({ page, size })}`),
    unreadCount: () => api.get('/api/notifications/unread-count'),
    markRead: (id) => http.patch(`/api/notifications/${id}/read`),
    markAllRead: () => api.post('/api/notifications/read-all'),
};
export const adminApi = {
    stats: () => api.get('/api/admin/stats'),
    activity: (limit = 10) => api.get(`/api/admin/activity${qs({ limit })}`),
    users: (params) => api.get(`/api/admin/users${qs(params)}`),
    setUserStatus: (id, status, reason) => api.patch(`/api/admin/users/${id}/status`, { status, reason }),
    ngos: (params) => api.get(`/api/admin/ngos${qs(params)}`),
    decideNgo: (profileId, approved, note) => api.post(`/api/admin/ngos/${profileId}/verification`, { approved, note }),
    donations: (params) => api.get(`/api/admin/donations${qs(params)}`),
    claims: (params) => api.get(`/api/admin/claims${qs(params)}`),
    cancelClaim: (id, reason) => api.post(`/api/admin/claims/${id}/cancel`, { reason }),
    createCategory: (payload) => api.post('/api/admin/categories', payload),
    updateCategory: (id, payload) => api.put(`/api/admin/categories/${id}`, payload),
    deactivateCategory: (id) => api.delete(`/api/admin/categories/${id}`),
    reports: (days = 30) => api.get(`/api/admin/reports${qs({ days })}`),
    auditLogs: (params) => api.get(`/api/admin/audit-logs${qs(params)}`),
};
export const publicApi = {
    stats: () => api.get('/api/public/stats'),
};
