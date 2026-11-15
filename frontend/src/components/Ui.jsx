import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { AlertTriangle, Info, Inbox, X } from 'lucide-react';
import { useEffect } from 'react';
// ---------------------------------------------------------------------------
// Status pill
// ---------------------------------------------------------------------------
export function Badge({ children, className = '' }) {
    return _jsx("span", { className: `badge ${className}`, children: children });
}
// ---------------------------------------------------------------------------
// Empty / error states
// ---------------------------------------------------------------------------
export function EmptyState({ title, description, action, icon, }) {
    return (_jsxs("div", { className: "flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-ink-300 bg-white/60 px-6 py-14 text-center", children: [_jsx("div", { className: "rounded-full bg-ink-100 p-3 text-ink-500", children: icon ?? _jsx(Inbox, { className: "h-5 w-5" }) }), _jsxs("div", { children: [_jsx("p", { className: "font-semibold text-ink-800", children: title }), description && _jsx("p", { className: "mx-auto mt-1 max-w-md text-sm text-ink-500", children: description })] }), action] }));
}
export function ErrorState({ message, onRetry }) {
    return (_jsxs("div", { className: "flex flex-col items-start gap-3 rounded-xl border border-red-200 bg-red-50 p-5", children: [_jsxs("div", { className: "flex items-start gap-2", children: [_jsx(AlertTriangle, { className: "mt-0.5 h-5 w-5 shrink-0 text-red-600" }), _jsx("p", { className: "text-sm font-medium text-red-800", children: message })] }), onRetry && (_jsx("button", { type: "button", className: "btn-secondary btn-sm", onClick: onRetry, children: "Try again" }))] }));
}
/** Persistent informational banner used for legal/safety copy. */
export function Callout({ tone = 'info', title, children, }) {
    const styles = tone === 'warning'
        ? 'border-amber-200 bg-amber-50 text-amber-900'
        : 'border-sky-200 bg-sky-50 text-sky-900';
    return (_jsxs("div", { className: `flex gap-3 rounded-xl border p-4 text-sm ${styles}`, children: [tone === 'warning' ? (_jsx(AlertTriangle, { className: "mt-0.5 h-4 w-4 shrink-0" })) : (_jsx(Info, { className: "mt-0.5 h-4 w-4 shrink-0" })), _jsxs("div", { children: [title && _jsx("p", { className: "font-semibold", children: title }), _jsx("div", { className: title ? 'mt-1' : '', children: children })] })] }));
}
// ---------------------------------------------------------------------------
// Modal
// ---------------------------------------------------------------------------
export function Modal({ open, title, description, onClose, children, footer, }) {
    useEffect(() => {
        if (!open)
            return;
        const onKeyDown = (event) => {
            if (event.key === 'Escape')
                onClose();
        };
        document.addEventListener('keydown', onKeyDown);
        const previous = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        return () => {
            document.removeEventListener('keydown', onKeyDown);
            document.body.style.overflow = previous;
        };
    }, [open, onClose]);
    if (!open)
        return null;
    return (_jsx("div", { className: "fixed inset-0 z-50 flex items-end justify-center bg-ink-900/40 p-0 sm:items-center sm:p-4", children: _jsxs("div", { role: "dialog", "aria-modal": "true", "aria-label": title, className: "w-full max-w-lg animate-fade-in rounded-t-2xl bg-white shadow-lift sm:rounded-2xl", children: [_jsxs("div", { className: "flex items-start justify-between gap-4 border-b border-ink-100 p-5", children: [_jsxs("div", { children: [_jsx("h2", { className: "text-lg font-semibold text-ink-900", children: title }), description && _jsx("p", { className: "mt-1 text-sm text-ink-500", children: description })] }), _jsx("button", { type: "button", onClick: onClose, className: "btn-ghost btn-sm", "aria-label": "Close dialog", children: _jsx(X, { className: "h-4 w-4" }) })] }), _jsx("div", { className: "max-h-[70vh] overflow-y-auto p-5", children: children }), footer && _jsx("div", { className: "flex justify-end gap-2 border-t border-ink-100 p-4", children: footer })] }) }));
}
// ---------------------------------------------------------------------------
// Pagination
// ---------------------------------------------------------------------------
export function Pagination({ page, totalPages, totalElements, onChange, }) {
    if (totalPages <= 1) {
        return _jsxs("p", { className: "px-1 py-2 text-xs text-ink-500", children: [totalElements, " result", totalElements === 1 ? '' : 's'] });
    }
    return (_jsxs("div", { className: "flex flex-wrap items-center justify-between gap-3 px-1 py-2", children: [_jsxs("p", { className: "text-xs text-ink-500", children: ["Page ", page + 1, " of ", totalPages, " \u00B7 ", totalElements, " result", totalElements === 1 ? '' : 's'] }), _jsxs("div", { className: "flex items-center gap-2", children: [_jsx("button", { type: "button", className: "btn-secondary btn-sm", disabled: page === 0, onClick: () => onChange(page - 1), children: "Previous" }), _jsx("button", { type: "button", className: "btn-secondary btn-sm", disabled: page >= totalPages - 1, onClick: () => onChange(page + 1), children: "Next" })] })] }));
}
// ---------------------------------------------------------------------------
// Simple SVG bar chart (no chart dependency needed)
// ---------------------------------------------------------------------------
export function BarChart({ data, height = 180, label, valueLabel = 'value', }) {
    const max = Math.max(1, ...data.map((d) => d.value));
    const barWidth = 100 / Math.max(data.length, 1);
    return (_jsxs("figure", { children: [_jsxs("svg", { viewBox: `0 0 100 ${height}`, preserveAspectRatio: "none", role: "img", "aria-label": label, className: "w-full", style: { height }, children: [data.map((point, index) => {
                        const barHeight = (point.value / max) * (height - 24);
                        return (_jsx("rect", { x: index * barWidth + barWidth * 0.18, y: height - 18 - barHeight, width: barWidth * 0.64, height: Math.max(barHeight, point.value > 0 ? 2 : 0), rx: 0.6, className: "fill-brand-500", children: _jsx("title", { children: `${point.label}: ${point.value} ${valueLabel}` }) }, `${point.label}-${index}`));
                    }), _jsx("line", { x1: "0", y1: height - 18, x2: "100", y2: height - 18, className: "stroke-ink-200", strokeWidth: "0.4" })] }), _jsxs("figcaption", { className: "mt-1 flex justify-between text-[10px] text-ink-400", children: [_jsx("span", { children: data[0]?.label }), _jsx("span", { children: data[data.length - 1]?.label })] })] }));
}
