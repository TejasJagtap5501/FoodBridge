import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '@/api/endpoints';
import { toApiError } from '@/api/client';
export default function ForgotPasswordPage() {
    const [email, setEmail] = useState('');
    const [message, setMessage] = useState(null);
    const [resetToken, setResetToken] = useState(null);
    const [newPassword, setNewPassword] = useState('');
    const [completed, setCompleted] = useState(false);
    const [error, setError] = useState(null);
    const [busy, setBusy] = useState(false);
    const request = async (event) => {
        event.preventDefault();
        setError(null);
        setBusy(true);
        try {
            const response = await authApi.forgotPassword(email);
            setMessage(response.message);
            setResetToken(response.resetToken ?? null);
        }
        catch (err) {
            setError(toApiError(err).message);
        }
        finally {
            setBusy(false);
        }
    };
    const reset = async (event) => {
        event.preventDefault();
        if (!resetToken)
            return;
        setError(null);
        setBusy(true);
        try {
            await authApi.resetPassword(resetToken, newPassword);
            setCompleted(true);
        }
        catch (err) {
            setError(toApiError(err).message);
        }
        finally {
            setBusy(false);
        }
    };
    return (_jsxs("div", { className: "mx-auto max-w-md px-4 py-14 sm:px-6", children: [_jsx("h1", { className: "text-2xl font-bold tracking-tight text-ink-900", children: "Reset your password" }), _jsx("p", { className: "mt-1 text-sm text-ink-500", children: "Enter your account email and we will generate a single-use reset token." }), error && _jsx("p", { role: "alert", className: "mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800", children: error }), _jsxs("form", { onSubmit: request, className: "mt-6 space-y-4", children: [_jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "email", children: "Email address" }), _jsx("input", { id: "email", type: "email", required: true, className: "input", value: email, onChange: (event) => setEmail(event.target.value), placeholder: "you@example.com" })] }), _jsx("button", { type: "submit", className: "btn-primary w-full", disabled: busy, children: busy ? 'Working…' : 'Generate reset token' })] }), message && (_jsxs("div", { className: "mt-6 rounded-lg border border-sky-200 bg-sky-50 p-4 text-sm text-sky-900", children: [_jsx("p", { children: message }), resetToken && (_jsxs(_Fragment, { children: [_jsx("p", { className: "mt-2 text-xs", children: "No mail service is configured in this build, so the token is shown here for local testing:" }), _jsx("code", { className: "mt-1 block break-all rounded bg-white p-2 text-xs", children: resetToken }), _jsxs("form", { onSubmit: reset, className: "mt-3 space-y-3", children: [_jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "newPassword", children: "New password" }), _jsx("input", { id: "newPassword", type: "password", required: true, minLength: 10, className: "input", value: newPassword, onChange: (event) => setNewPassword(event.target.value), placeholder: "At least 10 characters" })] }), _jsx("button", { type: "submit", className: "btn-primary btn-sm", disabled: busy, children: "Set new password" })] })] })), !resetToken && !completed && (_jsx("p", { className: "mt-2 text-xs", children: "Check your inbox for the reset instructions." }))] })), completed && (_jsxs("div", { className: "mt-4 rounded-lg border border-brand-200 bg-brand-50 p-4 text-sm text-brand-900", children: ["Password updated. All previous sessions have been revoked.", ' ', _jsx(Link, { to: "/login", className: "font-semibold underline", children: "Sign in" })] })), _jsx("p", { className: "mt-6 text-center text-sm text-ink-500", children: _jsx(Link, { to: "/login", className: "link", children: "Back to sign in" }) })] }));
}
