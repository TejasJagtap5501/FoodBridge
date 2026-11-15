import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/** Loading placeholders. Skeletons keep layout stable while data loads. */
export function PageLoader({ label = 'Loading…' }) {
    return (_jsxs("div", { className: "flex min-h-[40vh] flex-col items-center justify-center gap-3", role: "status", "aria-live": "polite", children: [_jsx("div", { className: "h-8 w-8 animate-spin rounded-full border-2 border-ink-200 border-t-brand-600" }), _jsx("p", { className: "text-sm text-ink-500", children: label })] }));
}
export function Skeleton({ className = '' }) {
    return _jsx("div", { className: `skeleton ${className}`, "aria-hidden": "true" });
}
export function StatCardSkeleton() {
    return (_jsxs("div", { className: "card card-body", children: [_jsx(Skeleton, { className: "h-3 w-24" }), _jsx(Skeleton, { className: "mt-3 h-7 w-16" }), _jsx(Skeleton, { className: "mt-3 h-3 w-32" })] }));
}
export function TableSkeleton({ rows = 5, columns = 5 }) {
    return (_jsx("div", { className: "table-wrap p-4", role: "status", "aria-label": "Loading table", children: Array.from({ length: rows }).map((_, rowIndex) => (_jsx("div", { className: "flex gap-4 border-b border-ink-100 py-3 last:border-0", children: Array.from({ length: columns }).map((__, columnIndex) => (_jsx(Skeleton, { className: "h-4 flex-1" }, columnIndex))) }, rowIndex))) }));
}
export function CardGridSkeleton({ cards = 6 }) {
    return (_jsx("div", { className: "grid gap-4 sm:grid-cols-2 xl:grid-cols-3", role: "status", "aria-label": "Loading cards", children: Array.from({ length: cards }).map((_, index) => (_jsxs("div", { className: "card card-body", children: [_jsx(Skeleton, { className: "h-4 w-2/3" }), _jsx(Skeleton, { className: "mt-3 h-3 w-1/3" }), _jsx(Skeleton, { className: "mt-5 h-3 w-full" }), _jsx(Skeleton, { className: "mt-2 h-3 w-4/5" }), _jsx(Skeleton, { className: "mt-5 h-9 w-28" })] }, index))) }));
}
