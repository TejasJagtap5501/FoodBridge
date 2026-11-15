import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Bell, Check } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { notificationApi } from '@/api/endpoints';
import { tokenStore } from '@/api/client';
import { relativeToNow } from '@/lib/format';
const STREAM_BASE = import.meta.env.VITE_API_BASE_URL ?? '';
/**
 * Notification bell backed by Server-Sent Events.
 *
 * <p>Reconnection strategy: when the stream errors, the browser would retry the
 * original URL - which contains a now-expired access token - so the component
 * closes it and re-opens with the current token using exponential backoff
 * (2s, 4s, 8s, capped at 30s). History is fetched after every (re)connect so
 * notifications produced during the gap are never lost.</p>
 */
export function NotificationBell() {
    const [open, setOpen] = useState(false);
    const [unread, setUnread] = useState(0);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(false);
    const [streamState, setStreamState] = useState('connecting');
    const containerRef = useRef(null);
    const retryRef = useRef(0);
    const sourceRef = useRef(null);
    const timerRef = useRef(null);
    const loadHistory = useCallback(async () => {
        setLoading(true);
        try {
            const [page, counter] = await Promise.all([
                notificationApi.list(0, 15),
                notificationApi.unreadCount(),
            ]);
            setItems(page.content);
            setUnread(counter.count);
        }
        catch {
            /* offline - keep whatever is cached */
        }
        finally {
            setLoading(false);
        }
    }, []);
    const connect = useCallback(() => {
        const token = tokenStore.get();
        if (!token)
            return;
        sourceRef.current?.close();
        const source = new EventSource(`${STREAM_BASE}/api/notifications/stream?access_token=${encodeURIComponent(token)}`);
        sourceRef.current = source;
        source.addEventListener('connected', () => {
            retryRef.current = 0;
            setStreamState('live');
            void loadHistory();
        });
        source.addEventListener('notification', (event) => {
            try {
                const payload = JSON.parse(event.data);
                setItems((current) => [payload, ...current].slice(0, 15));
                setUnread((current) => current + 1);
            }
            catch {
                /* ignore malformed frames */
            }
        });
        source.onerror = () => {
            source.close();
            setStreamState('offline');
            const delay = Math.min(2000 * 2 ** retryRef.current, 30000);
            retryRef.current += 1;
            timerRef.current = window.setTimeout(connect, delay);
        };
    }, [loadHistory]);
    useEffect(() => {
        void loadHistory();
        connect();
        return () => {
            sourceRef.current?.close();
            if (timerRef.current)
                window.clearTimeout(timerRef.current);
        };
    }, [connect, loadHistory]);
    useEffect(() => {
        if (!open)
            return;
        const onClick = (event) => {
            if (containerRef.current && !containerRef.current.contains(event.target)) {
                setOpen(false);
            }
        };
        document.addEventListener('mousedown', onClick);
        return () => document.removeEventListener('mousedown', onClick);
    }, [open]);
    const markAllRead = async () => {
        try {
            await notificationApi.markAllRead();
            setUnread(0);
            setItems((current) => current.map((item) => ({ ...item, read: true })));
        }
        catch {
            /* ignore */
        }
    };
    const markOne = async (item) => {
        if (item.read)
            return;
        try {
            await notificationApi.markRead(item.id);
            setItems((current) => current.map((entry) => (entry.id === item.id ? { ...entry, read: true } : entry)));
            setUnread((current) => Math.max(0, current - 1));
        }
        catch {
            /* ignore */
        }
    };
    return (_jsxs("div", { ref: containerRef, className: "relative", children: [_jsxs("button", { type: "button", onClick: () => setOpen((value) => !value), className: "relative rounded-lg p-2 text-ink-600 transition-colors hover:bg-ink-100", "aria-label": `Notifications${unread > 0 ? `, ${unread} unread` : ''}`, "aria-expanded": open, children: [_jsx(Bell, { className: "h-5 w-5" }), unread > 0 && (_jsx("span", { className: "absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-bold text-white", children: unread > 9 ? '9+' : unread }))] }), open && (_jsxs("div", { className: "absolute right-0 z-40 mt-2 w-80 animate-fade-in overflow-hidden rounded-xl border border-ink-200 bg-white shadow-lift sm:w-96", children: [_jsxs("div", { className: "flex items-center justify-between border-b border-ink-100 px-4 py-3", children: [_jsxs("div", { children: [_jsx("p", { className: "text-sm font-semibold text-ink-900", children: "Notifications" }), _jsx("p", { className: "text-[11px] text-ink-400", children: streamState === 'live' ? 'Live' : streamState === 'connecting' ? 'Connecting…' : 'Reconnecting…' })] }), _jsxs("button", { type: "button", onClick: markAllRead, className: "btn-ghost btn-sm", disabled: unread === 0, children: [_jsx(Check, { className: "h-3.5 w-3.5" }), " Mark all read"] })] }), _jsxs("div", { className: "max-h-80 overflow-y-auto", children: [loading && items.length === 0 && _jsx("p", { className: "px-4 py-6 text-center text-sm text-ink-400", children: "Loading\u2026" }), !loading && items.length === 0 && (_jsx("p", { className: "px-4 py-8 text-center text-sm text-ink-400", children: "You have no notifications yet." })), items.map((item) => (_jsxs("button", { type: "button", onClick: () => void markOne(item), className: `flex w-full flex-col gap-0.5 border-b border-ink-100 px-4 py-3 text-left transition-colors hover:bg-ink-50 ${item.read ? '' : 'bg-brand-50/60'}`, children: [_jsxs("span", { className: "flex items-center gap-2 text-sm font-medium text-ink-800", children: [!item.read && _jsx("span", { className: "h-1.5 w-1.5 rounded-full bg-brand-600", "aria-hidden": "true" }), item.title] }), _jsx("span", { className: "text-xs text-ink-500", children: item.message }), _jsx("span", { className: "text-[11px] text-ink-400", children: relativeToNow(item.createdAt) })] }, item.id)))] }), _jsx(Link, { to: "/notifications", onClick: () => setOpen(false), className: "block border-t border-ink-100 px-4 py-3 text-center text-sm font-semibold text-brand-700 hover:bg-ink-50", children: "View all notifications" })] }))] }));
}
