import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Search, UserCheck, UserX } from 'lucide-react';
import { useState } from 'react';
import { adminApi } from '@/api/endpoints';
import { toApiError } from '@/api/client';
import { PageHeader } from '@/components/Stats';
import { TableSkeleton } from '@/components/Loading';
import { Badge, EmptyState, ErrorState, Modal, Pagination } from '@/components/Ui';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
import { accountStatusStyles, verificationStyles } from '@/lib/constants';
import { formatDate } from '@/lib/format';
/** Paginated, server-filtered user administration. */
export default function AdminUsersPage() {
    const toast = useToast();
    const [search, setSearch] = useState('');
    const [role, setRole] = useState('');
    const [status, setStatus] = useState('');
    const [sortBy, setSortBy] = useState('createdAt');
    const [direction, setDirection] = useState('desc');
    const [page, setPage] = useState(0);
    const [target, setTarget] = useState(null);
    const { data, loading, error, reload } = useApi(() => adminApi.users({ search, role: role || undefined, status: status || undefined, page, size: 20, sortBy, direction }), [search, role, status, page, sortBy, direction]);
    const toggle = (user) => setTarget(user);
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: "Users", description: "Donors, NGOs and administrators. Deactivating an account blocks sign-in without deleting its history." }), _jsxs("div", { className: "card card-body mb-4 grid gap-3 md:grid-cols-2 lg:grid-cols-5", children: [_jsxs("div", { className: "relative lg:col-span-2", children: [_jsx(Search, { className: "pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" }), _jsx("label", { className: "sr-only", htmlFor: "user-search", children: "Search users" }), _jsx("input", { id: "user-search", className: "input pl-9", placeholder: "Search name or email", value: search, onChange: (event) => { setSearch(event.target.value); setPage(0); } })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "role-filter", children: "Role" }), _jsxs("select", { id: "role-filter", className: "input", value: role, onChange: (event) => { setRole(event.target.value); setPage(0); }, children: [_jsx("option", { value: "", children: "All roles" }), _jsx("option", { value: "DONOR", children: "Donor" }), _jsx("option", { value: "NGO", children: "NGO" }), _jsx("option", { value: "ADMIN", children: "Admin" })] })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "status-filter", children: "Status" }), _jsxs("select", { id: "status-filter", className: "input", value: status, onChange: (event) => { setStatus(event.target.value); setPage(0); }, children: [_jsx("option", { value: "", children: "All statuses" }), _jsx("option", { value: "ACTIVE", children: "Active" }), _jsx("option", { value: "PENDING", children: "Pending" }), _jsx("option", { value: "INACTIVE", children: "Inactive" })] })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "user-sort", children: "Sort" }), _jsxs("select", { id: "user-sort", className: "input", value: `${sortBy}:${direction}`, onChange: (event) => {
                                    const [nextSort, nextDirection] = event.target.value.split(':');
                                    setSortBy(nextSort);
                                    setDirection(nextDirection);
                                    setPage(0);
                                }, children: [_jsx("option", { value: "createdAt:desc", children: "Newest first" }), _jsx("option", { value: "createdAt:asc", children: "Oldest first" }), _jsx("option", { value: "fullName:asc", children: "Name A\u2192Z" }), _jsx("option", { value: "email:asc", children: "Email A\u2192Z" }), _jsx("option", { value: "lastLoginAt:desc", children: "Recent login" })] })] })] }), loading ? (_jsx(TableSkeleton, { rows: 8, columns: 6 })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : !data || data.content.length === 0 ? (_jsx(EmptyState, { title: "No users match these filters", description: "Try a different search term or clear the role and status filters." })) : (_jsxs("div", { className: "card overflow-hidden", children: [_jsx("div", { className: "overflow-x-auto", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { scope: "col", children: "User" }), _jsx("th", { scope: "col", children: "Role" }), _jsx("th", { scope: "col", children: "Organisation" }), _jsx("th", { scope: "col", children: "Status" }), _jsx("th", { scope: "col", children: "Last login" }), _jsx("th", { scope: "col", children: "Joined" }), _jsx("th", { scope: "col", className: "text-right", children: "Action" })] }) }), _jsx("tbody", { children: data.content.map((user) => (_jsxs("tr", { children: [_jsxs("td", { children: [_jsx("p", { className: "font-medium text-ink-900", children: user.fullName }), _jsx("p", { className: "text-xs text-ink-500", children: user.email })] }), _jsx("td", { children: _jsx(Badge, { className: "bg-ink-100 text-ink-700", children: user.role }) }), _jsxs("td", { className: "text-xs text-ink-600", children: [user.organizationName ?? '—', user.ngoVerificationStatus && (_jsx("span", { className: "mt-1 block", children: _jsx(Badge, { className: verificationStyles[user.ngoVerificationStatus], children: user.ngoVerificationStatus }) }))] }), _jsx("td", { children: _jsx(Badge, { className: accountStatusStyles[user.status], children: user.status }) }), _jsx("td", { className: "whitespace-nowrap text-xs text-ink-600", children: user.lastLoginAt ? formatDate(user.lastLoginAt) : 'Never' }), _jsx("td", { className: "whitespace-nowrap text-xs text-ink-600", children: formatDate(user.createdAt) }), _jsx("td", { children: _jsx("div", { className: "flex justify-end", children: user.role === 'ADMIN' ? (_jsx("span", { className: "text-xs text-ink-400", children: "Protected" })) : (_jsxs("button", { type: "button", className: "btn-secondary btn-sm", onClick: () => toggle(user), children: [user.status === 'ACTIVE' ? _jsx(UserX, { className: "h-3.5 w-3.5" }) : _jsx(UserCheck, { className: "h-3.5 w-3.5" }), user.status === 'ACTIVE' ? 'Deactivate' : 'Activate'] })) }) })] }, user.id))) })] }) }), _jsx(Pagination, { page: data.page, totalPages: data.totalPages, totalElements: data.totalElements, onChange: setPage })] })), _jsx(StatusModal, { user: target, onClose: () => setTarget(null), onDone: () => { setTarget(null); reload(); }, toast: toast })] }));
}
function StatusModal({ user, onClose, onDone, toast, }) {
    const [reason, setReason] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);
    const deactivating = user?.status === 'ACTIVE';
    const submit = async () => {
        if (!user)
            return;
        setBusy(true);
        setError(null);
        try {
            await adminApi.setUserStatus(user.id, deactivating ? 'INACTIVE' : 'ACTIVE', reason.trim() || undefined);
            toast.success(deactivating ? `${user.fullName} deactivated.` : `${user.fullName} activated.`);
            setReason('');
            onDone();
        }
        catch (caught) {
            setError(toApiError(caught).message);
        }
        finally {
            setBusy(false);
        }
    };
    return (_jsxs(Modal, { open: Boolean(user), title: deactivating ? 'Deactivate this account?' : 'Activate this account?', description: deactivating
            ? 'The user can no longer sign in. Existing donations, claims and audit records are preserved.'
            : 'The user regains access immediately and can sign in again.', onClose: onClose, footer: _jsxs(_Fragment, { children: [_jsx("button", { type: "button", className: "btn-secondary", onClick: onClose, children: "Cancel" }), _jsx("button", { type: "button", className: deactivating ? 'btn-danger' : 'btn-primary', onClick: submit, disabled: busy, children: busy ? 'Saving…' : deactivating ? 'Deactivate' : 'Activate' })] }), children: [user && _jsxs("p", { className: "mb-3 text-sm text-ink-600", children: [user.fullName, " \u00B7 ", user.email] }), _jsx("label", { className: "label", htmlFor: "status-reason", children: "Reason (recorded in the audit log)" }), _jsx("textarea", { id: "status-reason", rows: 2, className: "input", value: reason, onChange: (event) => setReason(event.target.value) }), error && _jsx("p", { className: "field-error", children: error })] }));
}
