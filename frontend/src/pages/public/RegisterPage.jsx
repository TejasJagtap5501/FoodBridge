import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertCircle, Building2, HeartHandshake } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { z } from 'zod';
import { useAuth, homeForRole } from '@/auth/AuthContext';
import { useToast } from '@/components/Toast';
import { businessTypes } from '@/lib/constants';
const passwordRule = z
    .string()
    .min(10, 'Password must be at least 10 characters')
    .max(72, 'Password must be at most 72 characters')
    .regex(/[A-Z]/, 'Include at least one uppercase letter')
    .regex(/[a-z]/, 'Include at least one lowercase letter')
    .regex(/[0-9]/, 'Include at least one digit');
const baseSchema = {
    email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
    password: z.string().min(1, 'Password is required'),
    fullName: z.string().min(2, 'Enter your full name').max(120),
    phone: z
        .string()
        .regex(/^$|^[0-9+\-() ]{7,24}$/, 'Enter a valid phone number')
        .optional()
        .or(z.literal('')),
    addressLine1: z.string().min(3, 'Street address is required').max(200),
    addressLine2: z.string().max(200).optional().or(z.literal('')),
    city: z.string().min(2, 'City is required').max(80),
    state: z.string().min(2, 'State is required').max(80),
    postalCode: z.string().min(3, 'Postal code is required').max(16),
};
const donorSchema = z.object({
    ...baseSchema,
    password: passwordRule,
    businessName: z.string().min(2, 'Business name is required').max(160),
    businessType: z.string().min(1, 'Select a business type'),
    fssaiLicense: z.string().max(32).optional().or(z.literal('')),
});
const ngoSchema = z.object({
    ...baseSchema,
    password: passwordRule,
    organizationName: z.string().min(2, 'Organisation name is required').max(160),
    registrationNumber: z.string().min(3, 'Registration number is required').max(64),
    website: z.string().max(200).optional().or(z.literal('')),
});
export default function RegisterPage() {
    const [params, setParams] = useSearchParams();
    const role = params.get('role') === 'ngo' ? 'NGO' : 'DONOR';
    const { user, registerDonor, registerNgo } = useAuth();
    const navigate = useNavigate();
    const toast = useToast();
    const [serverError, setServerError] = useState(null);
    const donorForm = useForm({
        resolver: zodResolver(donorSchema),
        defaultValues: { businessType: 'RESTAURANT' },
    });
    const ngoForm = useForm({ resolver: zodResolver(ngoSchema) });
    if (user) {
        return _jsx(Navigate, { to: homeForRole(user.role), replace: true });
    }
    const onDonorSubmit = async (values) => {
        setServerError(null);
        try {
            const created = await registerDonor({
                ...values,
                contactPerson: values.fullName,
                addressLine2: values.addressLine2 || undefined,
                phone: values.phone || undefined,
                fssaiLicense: values.fssaiLicense || undefined,
            });
            toast.success('Donor account created. Publish your first donation!');
            navigate(homeForRole(created.role), { replace: true });
        }
        catch (error) {
            setServerError(error instanceof Error ? error.message : 'Registration failed');
        }
    };
    const onNgoSubmit = async (values) => {
        setServerError(null);
        try {
            const created = await registerNgo({
                ...values,
                contactPerson: values.fullName,
                addressLine2: values.addressLine2 || undefined,
                phone: values.phone || undefined,
                website: values.website || undefined,
            });
            toast.success('Application submitted. An administrator will review your organisation.');
            navigate(homeForRole(created.role), { replace: true });
        }
        catch (error) {
            setServerError(error instanceof Error ? error.message : 'Registration failed');
        }
    };
    const tabClass = (active) => `flex flex-1 items-center justify-center gap-2 rounded-lg px-3 py-2.5 text-sm font-semibold transition-colors ${active ? 'bg-white text-brand-800 shadow-sm ring-1 ring-ink-200' : 'text-ink-600 hover:text-ink-900'}`;
    const form = role === 'DONOR' ? donorForm : ngoForm;
    const errors = form.formState.errors;
    const field = (name, label, options = {}) => {
        const id = String(name);
        const error = errors[id];
        return (_jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: id, children: [label, " ", options.required && _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: id, type: options.type ?? 'text', className: `input ${error ? 'input-error' : ''}`, placeholder: options.placeholder, autoComplete: options.autoComplete, "aria-invalid": Boolean(error), ...form.register(id) }), error?.message && _jsx("p", { className: "field-error", children: error.message })] }));
    };
    return (_jsxs("div", { className: "mx-auto max-w-2xl px-4 py-12 sm:px-6", children: [_jsx("h1", { className: "text-2xl font-bold tracking-tight text-ink-900", children: "Create your FoodBridge account" }), _jsx("p", { className: "mt-1 text-sm text-ink-500", children: "Choose the account type that matches how you will use the platform." }), _jsxs("div", { className: "mt-6 flex gap-2 rounded-xl bg-ink-100 p-1.5", children: [_jsxs("button", { type: "button", className: tabClass(role === 'DONOR'), onClick: () => setParams({}), children: [_jsx(Building2, { className: "h-4 w-4" }), " Food donor"] }), _jsxs("button", { type: "button", className: tabClass(role === 'NGO'), onClick: () => setParams({ role: 'ngo' }), children: [_jsx(HeartHandshake, { className: "h-4 w-4" }), " NGO"] })] }), role === 'NGO' && (_jsxs("div", { className: "mt-4 rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs text-amber-900", children: ["NGO accounts start as ", _jsx("strong", { children: "pending verification" }), ". You can sign in immediately, but you cannot claim donations until an administrator approves your organisation."] })), serverError && (_jsxs("div", { role: "alert", className: "mt-4 flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800", children: [_jsx(AlertCircle, { className: "mt-0.5 h-4 w-4 shrink-0" }), _jsx("span", { children: serverError })] })), role === 'DONOR' ? (_jsxs("form", { onSubmit: donorForm.handleSubmit(onDonorSubmit), className: "mt-6 space-y-4", noValidate: true, children: [_jsxs("div", { className: "grid gap-4 sm:grid-cols-2", children: [field('fullName', 'Your name', { autoComplete: 'name', required: true }), field('email', 'Email address', { type: 'email', autoComplete: 'email', required: true }), field('password', 'Password', { type: 'password', autoComplete: 'new-password', required: true }), field('phone', 'Phone', { type: 'tel', autoComplete: 'tel', placeholder: '+91 98765 43210' })] }), _jsx("p", { className: "-mt-2 text-xs text-ink-500", children: "At least 10 characters, with an uppercase letter, a lowercase letter and a digit." }), _jsxs("div", { className: "grid gap-4 sm:grid-cols-2", children: [field('businessName', 'Business name', { required: true }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "businessType", children: ["Business type ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("select", { id: "businessType", className: "input", ...donorForm.register('businessType'), children: businessTypes.map((type) => (_jsx("option", { value: type, children: type.charAt(0) + type.slice(1).toLowerCase() }, type))) }), errors.businessType?.message && _jsx("p", { className: "field-error", children: errors.businessType.message })] })] }), field('addressLine1', 'Street address', { autoComplete: 'address-line1', required: true }), field('addressLine2', 'Address line 2', { autoComplete: 'address-line2' }), _jsxs("div", { className: "grid gap-4 sm:grid-cols-3", children: [field('city', 'City', { autoComplete: 'address-level2', required: true }), field('state', 'State', { autoComplete: 'address-level1', required: true }), field('postalCode', 'Postal code', { autoComplete: 'postal-code', required: true })] }), field('fssaiLicense', 'FSSAI licence (optional)'), _jsx("button", { type: "submit", className: "btn-primary w-full", disabled: donorForm.formState.isSubmitting, children: donorForm.formState.isSubmitting ? 'Creating account…' : 'Create donor account' })] })) : (_jsxs("form", { onSubmit: ngoForm.handleSubmit(onNgoSubmit), className: "mt-6 space-y-4", noValidate: true, children: [_jsxs("div", { className: "grid gap-4 sm:grid-cols-2", children: [field('fullName', 'Contact person', { autoComplete: 'name', required: true }), field('email', 'Email address', { type: 'email', autoComplete: 'email', required: true }), field('password', 'Password', { type: 'password', autoComplete: 'new-password', required: true }), field('phone', 'Phone', { type: 'tel', autoComplete: 'tel', placeholder: '+91 98765 43210' })] }), _jsx("p", { className: "-mt-2 text-xs text-ink-500", children: "At least 10 characters, with an uppercase letter, a lowercase letter and a digit." }), _jsxs("div", { className: "grid gap-4 sm:grid-cols-2", children: [field('organizationName', 'Organisation name', { required: true }), field('registrationNumber', 'Registration number', { placeholder: 'MH/2025/0123456', required: true })] }), field('website', 'Website (optional)', { type: 'url', placeholder: 'https://example.org' }), field('addressLine1', 'Street address', { autoComplete: 'address-line1', required: true }), field('addressLine2', 'Address line 2', { autoComplete: 'address-line2' }), _jsxs("div", { className: "grid gap-4 sm:grid-cols-3", children: [field('city', 'City', { autoComplete: 'address-level2', required: true }), field('state', 'State', { autoComplete: 'address-level1', required: true }), field('postalCode', 'Postal code', { autoComplete: 'postal-code', required: true })] }), _jsx("button", { type: "submit", className: "btn-primary w-full", disabled: ngoForm.formState.isSubmitting, children: ngoForm.formState.isSubmitting ? 'Submitting application…' : 'Submit NGO application' })] })), _jsxs("p", { className: "mt-5 text-center text-sm text-ink-500", children: ["Already registered? ", _jsx(Link, { to: "/login", className: "link", children: "Sign in" })] })] }));
}
