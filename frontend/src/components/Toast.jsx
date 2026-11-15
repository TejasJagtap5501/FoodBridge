import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';
import { createContext, useCallback, useContext, useMemo, useState } from 'react';
const ToastContext = createContext(undefined);
let nextId = 1;
export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);
    const dismiss = useCallback((id) => {
        setToasts((current) => current.filter((toast) => toast.id !== id));
    }, []);
    const push = useCallback((tone, message) => {
        const id = nextId++;
        setToasts((current) => [...current, { id, tone, message }]);
        window.setTimeout(() => dismiss(id), 5000);
    }, [dismiss]);
    const api = useMemo(() => ({
        success: (message) => push('success', message),
        error: (message) => push('error', message),
        info: (message) => push('info', message),
    }), [push]);
    return (_jsxs(ToastContext.Provider, { value: api, children: [children, _jsx("div", { className: "pointer-events-none fixed inset-x-0 bottom-4 z-[60] flex flex-col items-center gap-2 px-4 sm:bottom-6 sm:items-end sm:pr-6", role: "status", "aria-live": "polite", children: toasts.map((toast) => {
                    const styles = toast.tone === 'success'
                        ? 'border-brand-200 bg-white text-ink-800'
                        : toast.tone === 'error'
                            ? 'border-red-200 bg-white text-ink-800'
                            : 'border-sky-200 bg-white text-ink-800';
                    const Icon = toast.tone === 'success' ? CheckCircle2 : toast.tone === 'error' ? AlertCircle : Info;
                    const iconColor = toast.tone === 'success' ? 'text-brand-600' : toast.tone === 'error' ? 'text-red-600' : 'text-sky-600';
                    return (_jsxs("div", { className: `pointer-events-auto flex w-full max-w-sm animate-fade-in items-start gap-3 rounded-lg border p-3 shadow-lift ${styles}`, children: [_jsx(Icon, { className: `mt-0.5 h-5 w-5 shrink-0 ${iconColor}` }), _jsx("p", { className: "flex-1 text-sm", children: toast.message }), _jsx("button", { type: "button", onClick: () => dismiss(toast.id), "aria-label": "Dismiss notification", className: "text-ink-400 hover:text-ink-700", children: _jsx(X, { className: "h-4 w-4" }) })] }, toast.id));
                }) })] }));
}
export function useToast() {
    const context = useContext(ToastContext);
    if (!context) {
        throw new Error('useToast must be used inside <ToastProvider>');
    }
    return context;
}
