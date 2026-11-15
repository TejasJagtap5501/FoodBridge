import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Bell, CheckCheck, RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { notificationApi } from '@/api/endpoints';
import { toApiError } from '@/api/client';
import { PageHeader } from '@/components/Stats';
import { TableSkeleton } from '@/components/Loading';
import { EmptyState, ErrorState, Pagination } from '@/components/Ui';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
import { relativeToNow } from '@/lib/format';
/**
 * Persistent notification history. Live events also arrive over SSE (see
 * NotificationBell); this page is the durable record and lets the user mark
 * items as read.
 */
export default function NotificationsPage() {
    const [page, setPage] = useState(0);
    const toast = useToast();
    const { data, loading, error, reload } = useApi(() => notificationApi.list(page, 15), [page]);
    const markRead = async (id) => {
        try {
            await notificationApi.markRead(id);
            reload();
        }
        catch (caught) {
            toast.error(toApiError(caught).message);
        }
    };
    const markAll = async () => {
        try {
            const result = await notificationApi.markAllRead();
            toast.success(result.updated > 0 ? `${result.updated} notification(s) marked as read.` : 'Everything was already read.');
            reload();
        }
        catch (caught) {
            toast.error(toApiError(caught).message);
        }
    };
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: "Notifications", description: "Claims, pickups, verification decisions and expiry reminders.", actions: _jsxs(_Fragment, { children: [_jsxs("button", { type: "button", className: "btn-secondary", onClick: reload, children: [_jsx(RefreshCw, { className: "h-4 w-4" }), " Refresh"] }), _jsxs("button", { type: "button", className: "btn-primary", onClick: markAll, children: [_jsx(CheckCheck, { className: "h-4 w-4" }), " Mark all read"] })] }) }), loading ? (_jsx(TableSkeleton, { rows: 6, columns: 3 })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : !data || data.content.length === 0 ? (_jsx(EmptyState, { icon: _jsx(Bell, { className: "h-5 w-5" }), title: "No notifications yet", description: "You will be notified here when donations are claimed, pickups are scheduled, or your organisation is verified." })) : (_jsxs("div", { className: "card overflow-hidden", children: [_jsx("ul", { className: "divide-y divide-ink-100", children: data.content.map((item) => (_jsxs("li", { className: `flex flex-wrap items-start justify-between gap-3 p-4 ${item.read ? '' : 'bg-brand-50/40'}`, children: [_jsxs("div", { className: "min-w-0", children: [_jsxs("div", { className: "flex items-center gap-2", children: [!item.read && _jsx("span", { className: "h-2 w-2 shrink-0 rounded-full bg-brand-500", "aria-label": "Unread" }), _jsx("p", { className: "font-medium text-ink-900", children: item.title })] }), _jsx("p", { className: "mt-0.5 text-sm text-ink-600", children: item.message }), _jsxs("p", { className: "mt-1 text-xs text-ink-400", children: [relativeToNow(item.createdAt), " \u00B7 ", item.type.replaceAll('_', ' ').toLowerCase()] })] }), _jsxs("div", { className: "flex items-center gap-2", children: [item.link && (_jsx(Link, { to: item.link, className: "btn-ghost btn-sm", children: "Open" })), !item.read && (_jsx("button", { type: "button", className: "btn-secondary btn-sm", onClick: () => markRead(item.id), children: "Mark read" }))] })] }, item.id))) }), _jsx(Pagination, { page: data.page, totalPages: data.totalPages, totalElements: data.totalElements, onChange: setPage })] }))] }));
}
