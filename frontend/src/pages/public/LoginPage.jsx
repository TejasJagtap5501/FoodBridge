import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertCircle, LogIn } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { useAuth, homeForRole } from '@/auth/AuthContext';
import { useToast } from '@/components/Toast';
const schema = z.object({
    email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
    password: z.string().min(1, 'Password is required'),
});
const demoAccounts = [
    { label: 'Donor', email: 'donor@foodbridge.dev', password: 'Donor@12345' },
    { label: 'NGO', email: 'ngo@foodbridge.dev', password: 'Ngo@12345' },
    { label: 'Admin', email: 'admin@foodbridge.dev', password: 'Admin@12345' },
];
export default function LoginPage() {
    const { login, user } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const toast = useToast();
    const [serverError, setServerError] = useState(null);
    const { register, handleSubmit, setValue, formState: { errors, isSubmitting }, } = useForm({ resolver: zodResolver(schema), defaultValues: { email: '', password: '' } });
    if (user) {
        return _jsx(Navigate, { to: homeForRole(user.role), replace: true });
    }
    const onSubmit = async (values) => {
        setServerError(null);
        try {
            const signedIn = await login(values.email, values.password);
            toast.success(`Welcome back, ${signedIn.fullName.split(' ')[0]}!`);
            const from = location.state?.from;
            navigate(from ?? homeForRole(signedIn.role), { replace: true });
        }
        catch (error) {
            setServerError(error instanceof Error ? error.message : 'Sign in failed');
        }
    };
    const fillDemo = (account) => {
        setValue('email', account.email, { shouldValidate: true });
        setValue('password', account.password, { shouldValidate: true });
    };
    return (_jsxs("div", { className: "mx-auto flex max-w-md flex-col px-4 py-14 sm:px-6", children: [_jsx("h1", { className: "text-2xl font-bold tracking-tight text-ink-900", children: "Sign in to FoodBridge" }), _jsx("p", { className: "mt-1 text-sm text-ink-500", children: "Use your registered email address and password." }), serverError && (_jsxs("div", { role: "alert", className: "mt-5 flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800", children: [_jsx(AlertCircle, { className: "mt-0.5 h-4 w-4 shrink-0" }), _jsx("span", { children: serverError })] })), _jsxs("form", { onSubmit: handleSubmit(onSubmit), className: "mt-6 space-y-4", noValidate: true, children: [_jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "email", children: "Email address" }), _jsx("input", { id: "email", type: "email", autoComplete: "email", className: `input ${errors.email ? 'input-error' : ''}`, placeholder: "you@example.com", "aria-invalid": Boolean(errors.email), ...register('email') }), errors.email && _jsx("p", { className: "field-error", children: errors.email.message })] }), _jsxs("div", { children: [_jsxs("div", { className: "flex items-center justify-between", children: [_jsx("label", { className: "label", htmlFor: "password", children: "Password" }), _jsx(Link, { to: "/forgot-password", className: "mb-1.5 text-xs font-semibold text-brand-700 hover:underline", children: "Forgot password?" })] }), _jsx("input", { id: "password", type: "password", autoComplete: "current-password", className: `input ${errors.password ? 'input-error' : ''}`, placeholder: "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022", "aria-invalid": Boolean(errors.password), ...register('password') }), errors.password && _jsx("p", { className: "field-error", children: errors.password.message })] }), _jsx("button", { type: "submit", className: "btn-primary w-full", disabled: isSubmitting, children: isSubmitting ? 'Signing in…' : (_jsxs(_Fragment, { children: [_jsx(LogIn, { className: "h-4 w-4" }), " Sign in"] })) })] }), _jsxs("p", { className: "mt-5 text-center text-sm text-ink-500", children: ["New to FoodBridge?", ' ', _jsx(Link, { to: "/register", className: "link", children: "Create an account" })] }), _jsxs("div", { className: "mt-8 rounded-xl border border-dashed border-ink-300 bg-ink-50/60 p-4", children: [_jsx("p", { className: "text-xs font-semibold uppercase tracking-wide text-ink-500", children: "Demo accounts" }), _jsx("p", { className: "mt-1 text-[11px] text-ink-400", children: "Seeded for local development only. Click to fill the form." }), _jsx("div", { className: "mt-3 flex flex-wrap gap-2", children: demoAccounts.map((account) => (_jsx("button", { type: "button", onClick: () => fillDemo(account), className: "btn-secondary btn-sm", children: account.label }, account.email))) })] })] }));
}
