import { jsx as _jsx } from "react/jsx-runtime";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { authApi } from '@/api/endpoints';
import { tokenStore, toApiError } from '@/api/client';
const AuthContext = createContext(undefined);
/** Where each role lands after a successful sign-in. */
export function homeForRole(role) {
    switch (role) {
        case 'DONOR':
            return '/donor';
        case 'NGO':
            return '/ngo';
        case 'ADMIN':
            return '/admin';
        default:
            return '/';
    }
}
export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const bootstrapped = useRef(false);
    // On first load, try to restore the session from the HttpOnly refresh cookie.
    useEffect(() => {
        if (bootstrapped.current)
            return;
        bootstrapped.current = true;
        (async () => {
            try {
                const session = await authApi.refresh();
                tokenStore.set(session.accessToken);
                setUser(session.user);
            }
            catch {
                tokenStore.set(null);
            }
            finally {
                setLoading(false);
            }
        })();
    }, []);
    useEffect(() => {
        tokenStore.onExpired(() => setUser(null));
        return () => tokenStore.onExpired(null);
    }, []);
    const applySession = useCallback((accessToken, nextUser) => {
        tokenStore.set(accessToken);
        setUser(nextUser);
        return nextUser;
    }, []);
    const login = useCallback(async (email, password) => {
        try {
            const session = await authApi.login(email, password);
            return applySession(session.accessToken, session.user);
        }
        catch (error) {
            throw new Error(toApiError(error).message);
        }
    }, [applySession]);
    const registerDonor = useCallback(async (payload) => {
        try {
            const session = await authApi.registerDonor(payload);
            return applySession(session.accessToken, session.user);
        }
        catch (error) {
            const apiError = toApiError(error);
            const firstField = apiError.fieldErrors ? Object.values(apiError.fieldErrors)[0] : undefined;
            throw new Error(firstField ?? apiError.message);
        }
    }, [applySession]);
    const registerNgo = useCallback(async (payload) => {
        try {
            const session = await authApi.registerNgo(payload);
            return applySession(session.accessToken, session.user);
        }
        catch (error) {
            const apiError = toApiError(error);
            const firstField = apiError.fieldErrors ? Object.values(apiError.fieldErrors)[0] : undefined;
            throw new Error(firstField ?? apiError.message);
        }
    }, [applySession]);
    const logout = useCallback(async () => {
        try {
            await authApi.logout();
        }
        finally {
            tokenStore.set(null);
            setUser(null);
        }
    }, []);
    const refreshUser = useCallback(async () => {
        try {
            const me = await authApi.me();
            setUser(me);
        }
        catch {
            /* silently ignored - the interceptor handles expired sessions */
        }
    }, []);
    const value = useMemo(() => ({ user, loading, login, registerDonor, registerNgo, logout, refreshUser, setUser }), [user, loading, login, registerDonor, registerNgo, logout, refreshUser]);
    return _jsx(AuthContext.Provider, { value: value, children: children });
}
export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used inside <AuthProvider>');
    }
    return context;
}
