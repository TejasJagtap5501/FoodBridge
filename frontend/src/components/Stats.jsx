import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { formatNumber } from '@/lib/format';
/** Compact KPI tile used across all three dashboards. */
export function StatCard({ label, value, hint, icon: Icon, tone = 'default', }) {
    const tones = {
        default: 'bg-ink-100 text-ink-600',
        success: 'bg-brand-100 text-brand-700',
        warning: 'bg-amber-100 text-amber-700',
        danger: 'bg-red-100 text-red-700',
        info: 'bg-sky-100 text-sky-700',
    };
    return (_jsx("div", { className: "card card-body", children: _jsxs("div", { className: "flex items-start justify-between gap-3", children: [_jsxs("div", { className: "min-w-0", children: [_jsx("p", { className: "truncate text-xs font-medium uppercase tracking-wide text-ink-500", children: label }), _jsx("p", { className: "mt-1.5 text-2xl font-bold text-ink-900", children: typeof value === 'number' ? formatNumber(value) : value }), hint && _jsx("p", { className: "mt-1 truncate text-xs text-ink-400", children: hint })] }), _jsx("span", { className: `flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${tones[tone]}`, children: _jsx(Icon, { className: "h-4 w-4" }) })] }) }));
}
export function PageHeader({ title, description, actions, }) {
    return (_jsxs("div", { className: "mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between", children: [_jsxs("div", { children: [_jsx("h1", { className: "text-2xl font-bold tracking-tight text-ink-900", children: title }), description && _jsx("p", { className: "mt-1 text-sm text-ink-500", children: description })] }), actions && _jsx("div", { className: "flex flex-wrap gap-2", children: actions })] }));
}
