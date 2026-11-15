import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Link } from 'react-router-dom';
export default function NotFoundPage() {
    return (_jsxs("div", { className: "mx-auto flex max-w-md flex-col items-center px-4 py-24 text-center sm:px-6", children: [_jsx("p", { className: "text-5xl font-black text-brand-600", children: "404" }), _jsx("h1", { className: "mt-3 text-xl font-bold text-ink-900", children: "Page not found" }), _jsx("p", { className: "mt-2 text-sm text-ink-500", children: "The page you were looking for does not exist or may have been moved." }), _jsx(Link, { to: "/", className: "btn-primary mt-6", children: "Back to home" })] }));
}
