import axios from 'axios';
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';
/**
 * Access token is kept in memory only (never localStorage) so an XSS payload
 * cannot read it from persistent storage. The refresh token lives in an
 * HttpOnly cookie that JavaScript cannot access at all.
 */
let accessToken = null;
let onSessionExpired = null;
let refreshing = null;
export const tokenStore = {
    get: () => accessToken,
    set: (token) => {
        accessToken = token;
    },
    onExpired: (handler) => {
        onSessionExpired = handler;
    },
};
export const http = axios.create({
    baseURL: BASE_URL,
    withCredentials: true,
    timeout: 20000,
    headers: { 'Content-Type': 'application/json' },
});
http.interceptors.request.use((config) => {
    if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
});
/** Single-flight refresh: concurrent 401s share one refresh request. */
async function refreshAccessToken() {
    if (!refreshing) {
        refreshing = axios
            .post(`${BASE_URL}/api/auth/refresh`, {}, { withCredentials: true })
            .then((response) => {
            accessToken = response.data.accessToken;
            return accessToken;
        })
            .catch(() => {
            accessToken = null;
            return null;
        })
            .finally(() => {
            refreshing = null;
        });
    }
    return refreshing;
}
http.interceptors.response.use((response) => response, async (error) => {
    const original = error.config;
    const url = original?.url ?? '';
    const isAuthEndpoint = url.includes('/api/auth/login') ||
        url.includes('/api/auth/register') ||
        url.includes('/api/auth/refresh');
    if (error.response?.status === 401 && original && !original._retried && !isAuthEndpoint) {
        original._retried = true;
        const token = await refreshAccessToken();
        if (token) {
            original.headers = { ...(original.headers ?? {}), Authorization: `Bearer ${token}` };
            return http.request(original);
        }
        onSessionExpired?.();
    }
    return Promise.reject(error);
});
/** Converts any thrown value into a displayable message + machine code. */
export function toApiError(error) {
    if (axios.isAxiosError(error)) {
        const body = error.response?.data;
        if (body?.message) {
            return { message: body.message, code: body.code ?? 'ERROR', fieldErrors: body.fieldErrors };
        }
        if (error.code === 'ECONNABORTED') {
            return { message: 'The request timed out. Please try again.', code: 'TIMEOUT' };
        }
        if (!error.response) {
            return { message: 'Cannot reach the FoodBridge server. Check your connection.', code: 'NETWORK' };
        }
    }
    if (error instanceof Error) {
        return { message: error.message, code: 'ERROR' };
    }
    return { message: 'Something went wrong.', code: 'ERROR' };
}
export const api = {
    get: (url, config) => http.get(url, config).then((r) => r.data),
    post: (url, body, config) => http.post(url, body, config).then((r) => r.data),
    put: (url, body) => http.put(url, body).then((r) => r.data),
    patch: (url, body) => http.patch(url, body).then((r) => r.data),
    delete: (url) => http.delete(url).then((r) => r.data),
};
