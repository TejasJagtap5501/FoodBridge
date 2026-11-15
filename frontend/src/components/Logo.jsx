import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Link } from 'react-router-dom';
/**
 * FoodBridge wordmark. Inline SVG so it renders without an external asset and
 * inherits the current text colour.
 */
export function Logo({ to = '/', compact = false }) {
    return (_jsxs(Link, { to: to, className: "inline-flex items-center gap-2", "aria-label": "FoodBridge home", children: [_jsx("span", { className: "flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white shadow-sm", children: _jsxs("svg", { viewBox: "0 0 32 32", className: "h-5 w-5", "aria-hidden": "true", fill: "none", children: [_jsx("path", { d: "M9 21c0-6 5-11 14-12 1 9-4 14-10 14H9v-2z", fill: "currentColor" }), _jsx("path", { d: "M8 24c3-6 7-9 12-11", stroke: "#bbf7d0", strokeWidth: "1.8", strokeLinecap: "round" })] }) }), !compact && (_jsxs("span", { className: "text-lg font-bold tracking-tight text-ink-900", children: ["Food", _jsx("span", { className: "text-brand-700", children: "Bridge" })] }))] }));
}
