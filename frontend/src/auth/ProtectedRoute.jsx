import { jsx as _jsx, Fragment as _Fragment } from "react/jsx-runtime";
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth, homeForRole } from './AuthContext';
import { PageLoader } from '@/components/Loading';
/**
 * Route guard.
 *
 * <p>This is a UX convenience only: every protected API call is authorised again
 * by Spring Security. Hiding a route never substitutes for backend checks.</p>
 */
export function ProtectedRoute({ roles, children }) {
    const { user, loading } = useAuth();
    const location = useLocation();
    if (loading) {
        return _jsx(PageLoader, { label: "Checking your session\u2026" });
    }
    if (!user) {
        return _jsx(Navigate, { to: "/login", replace: true, state: { from: location.pathname } });
    }
    if (!roles.includes(user.role)) {
        return _jsx(Navigate, { to: homeForRole(user.role), replace: true });
    }
    return _jsx(_Fragment, { children: children });
}
