import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { AlertCircle, Save, ShieldCheck } from 'lucide-react';
import { useEffect, useState } from 'react';
import { authApi, profileApi } from '@/api/endpoints';
import { toApiError } from '@/api/client';
import { PageHeader } from '@/components/Stats';
import { PageLoader } from '@/components/Loading';
import { Badge, Callout, ErrorState } from '@/components/Ui';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
import { useAuth } from '@/auth/AuthContext';
import { accountStatusStyles, businessTypes, verificationStyles } from '@/lib/constants';
import { formatDate } from '@/lib/format';
/**
 * Profile management for all three roles. The backend merges the shared user
 * fields with the role-specific profile row, so this page posts one payload.
 */
export default function ProfilePage() {
    const { user, refreshUser } = useAuth();
    const toast = useToast();
    const { data, loading, error, reload } = useApi(() => profileApi.get(), []);
    const [form, setForm] = useState({
        fullName: '', phone: '', businessName: '', businessType: 'RESTAURANT',
        organizationName: '', website: '', addressLine1: '', addressLine2: '',
        city: '', state: '', postalCode: '',
    });
    const [saving, setSaving] = useState(false);
    const [formError, setFormError] = useState(null);
    const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirm: '' });
    const [passwordError, setPasswordError] = useState(null);
    const [changingPassword, setChangingPassword] = useState(false);
    useEffect(() => {
        if (!data)
            return;
        setForm({
            fullName: data.user.fullName ?? '',
            phone: data.user.phone ?? '',
            businessName: data.donorProfile?.businessName ?? '',
            businessType: data.donorProfile?.businessType ?? 'RESTAURANT',
            organizationName: data.ngoProfile?.organizationName ?? '',
            website: data.ngoProfile?.website ?? '',
            addressLine1: data.donorProfile?.addressLine1 ?? data.ngoProfile?.addressLine1 ?? '',
            addressLine2: data.donorProfile?.addressLine2 ?? data.ngoProfile?.addressLine2 ?? '',
            city: data.donorProfile?.city ?? data.ngoProfile?.city ?? '',
            state: data.donorProfile?.state ?? data.ngoProfile?.state ?? '',
            postalCode: data.donorProfile?.postalCode ?? data.ngoProfile?.postalCode ?? '',
        });
    }, [data]);
    if (loading)
        return _jsx(PageLoader, { label: "Loading profile\u2026" });
    if (error)
        return _jsx(ErrorState, { message: error, onRetry: reload });
    const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
    const save = async (event) => {
        event.preventDefault();
        setSaving(true);
        setFormError(null);
        try {
            const updated = await profileApi.update({
                fullName: form.fullName.trim(),
                phone: form.phone.trim() || '',
                businessName: data?.donorProfile ? form.businessName.trim() : undefined,
                businessType: data?.donorProfile ? form.businessType : undefined,
                organizationName: data?.ngoProfile ? form.organizationName.trim() : undefined,
                website: data?.ngoProfile ? form.website.trim() || '' : undefined,
                addressLine1: form.addressLine1.trim(),
                addressLine2: form.addressLine2.trim(),
                city: form.city.trim(),
                state: form.state.trim(),
                postalCode: form.postalCode.trim(),
            });
            await refreshUser();
            toast.success('Profile updated.');
            reload();
            void updated;
        }
        catch (caught) {
            const apiError = toApiError(caught);
            setFormError(apiError.fieldErrors ? Object.values(apiError.fieldErrors)[0] : apiError.message);
        }
        finally {
            setSaving(false);
        }
    };
    const changePassword = async (event) => {
        event.preventDefault();
        setPasswordError(null);
        if (passwords.newPassword !== passwords.confirm) {
            setPasswordError('The new passwords do not match.');
            return;
        }
        setChangingPassword(true);
        try {
            await authApi.changePassword(passwords.currentPassword, passwords.newPassword);
            toast.success('Password changed. Other devices stay signed in until their token expires.');
            setPasswords({ currentPassword: '', newPassword: '', confirm: '' });
        }
        catch (caught) {
            const apiError = toApiError(caught);
            setPasswordError(apiError.fieldErrors ? Object.values(apiError.fieldErrors)[0] : apiError.message);
        }
        finally {
            setChangingPassword(false);
        }
    };
    const isDonor = Boolean(data?.donorProfile);
    const isNgo = Boolean(data?.ngoProfile);
    return (_jsxs("div", { className: "mx-auto max-w-3xl", children: [_jsx(PageHeader, { title: "Profile", description: "Keep your contact and organisation details current \u2014 NGOs and donors rely on them for pickups." }), _jsxs("section", { className: "card card-body mb-6", children: [_jsxs("div", { className: "flex flex-wrap items-start justify-between gap-3", children: [_jsxs("div", { children: [_jsx("p", { className: "font-semibold text-ink-900", children: data?.user.fullName }), _jsx("p", { className: "text-sm text-ink-500", children: data?.user.email }), _jsxs("p", { className: "mt-1 text-xs text-ink-400", children: [data?.user.role, " \u00B7 member since ", formatDate(data?.user.createdAt), data?.user.lastLoginAt ? ` · last login ${formatDate(data.user.lastLoginAt)}` : ''] })] }), _jsxs("div", { className: "flex flex-col items-end gap-2", children: [_jsx(Badge, { className: accountStatusStyles[data?.user.status ?? 'PENDING'], children: data?.user.status }), isNgo && data?.ngoProfile && (_jsxs(Badge, { className: verificationStyles[data.ngoProfile.verificationStatus], children: ["NGO ", data.ngoProfile.verificationStatus] }))] })] }), isNgo && data?.ngoProfile && (_jsx("div", { className: "mt-4", children: _jsxs(Callout, { tone: data.ngoProfile.verificationStatus === 'APPROVED' ? 'info' : 'warning', title: "Verification", children: [data.ngoProfile.verificationStatus === 'APPROVED' && (_jsxs(_Fragment, { children: ["Your organisation is verified", data.ngoProfile.verifiedAt ? ` since ${formatDate(data.ngoProfile.verifiedAt)}` : '', ". You can claim donations."] })), data.ngoProfile.verificationStatus === 'PENDING' && (_jsxs(_Fragment, { children: ["An administrator is reviewing registration number ", data.ngoProfile.registrationNumber, ". Claiming is disabled until then."] })), data.ngoProfile.verificationStatus === 'REJECTED' && (_jsxs(_Fragment, { children: ["Your application was rejected", data.ngoProfile.verificationNote ? `: ${data.ngoProfile.verificationNote}` : '.', " Contact an administrator to reapply."] }))] }) }))] }), _jsxs("form", { onSubmit: save, className: "card card-body space-y-4", children: [_jsx("h2", { className: "font-semibold text-ink-900", children: "Contact details" }), formError && (_jsxs("p", { role: "alert", className: "flex items-center gap-2 rounded-lg bg-red-50 p-2 text-sm text-red-800", children: [_jsx(AlertCircle, { className: "h-4 w-4" }), " ", formError] })), _jsxs("div", { className: "grid gap-4 sm:grid-cols-2", children: [_jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "fullName", children: ["Full name ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "fullName", className: "input", value: form.fullName, onChange: update('fullName'), required: true })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "phone", children: "Phone" }), _jsx("input", { id: "phone", className: "input", value: form.phone, onChange: update('phone'), placeholder: "+91 98765 43210" })] })] }), isDonor && (_jsxs("div", { className: "grid gap-4 sm:grid-cols-2", children: [_jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "businessName", children: ["Business name ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "businessName", className: "input", value: form.businessName, onChange: update('businessName'), required: true })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "businessType", children: "Business type" }), _jsx("select", { id: "businessType", className: "input", value: form.businessType, onChange: update('businessType'), children: businessTypes.map((type) => _jsx("option", { value: type, children: type }, type)) })] })] })), isNgo && (_jsxs("div", { className: "grid gap-4 sm:grid-cols-2", children: [_jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "organizationName", children: ["Organisation name ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "organizationName", className: "input", value: form.organizationName, onChange: update('organizationName'), required: true })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "website", children: "Website" }), _jsx("input", { id: "website", className: "input", value: form.website, onChange: update('website'), placeholder: "https://" })] }), _jsxs("div", { className: "sm:col-span-2", children: [_jsx("p", { className: "label", children: "Registration number" }), _jsxs("p", { className: "flex items-center gap-2 text-sm text-ink-600", children: [_jsx(ShieldCheck, { className: "h-4 w-4 text-ink-400" }), data?.ngoProfile?.registrationNumber, " \u2014 this cannot be changed here; contact an administrator."] })] })] })), _jsx("h2", { className: "pt-2 font-semibold text-ink-900", children: "Address" }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "addressLine1", children: ["Address line 1 ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "addressLine1", className: "input", value: form.addressLine1, onChange: update('addressLine1'), required: true })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "addressLine2", children: "Address line 2" }), _jsx("input", { id: "addressLine2", className: "input", value: form.addressLine2, onChange: update('addressLine2') })] }), _jsxs("div", { className: "grid gap-4 sm:grid-cols-3", children: [_jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "city", children: ["City ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "city", className: "input", value: form.city, onChange: update('city'), required: true })] }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "state", children: ["State ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "state", className: "input", value: form.state, onChange: update('state'), required: true })] }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "postalCode", children: ["Postal code ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "postalCode", className: "input", value: form.postalCode, onChange: update('postalCode'), required: true })] })] }), _jsx("div", { className: "flex justify-end", children: _jsxs("button", { type: "submit", className: "btn-primary", disabled: saving, children: [_jsx(Save, { className: "h-4 w-4" }), " ", saving ? 'Saving…' : 'Save changes'] }) })] }), _jsxs("form", { onSubmit: changePassword, className: "card card-body mt-6 space-y-4", children: [_jsxs("div", { children: [_jsx("h2", { className: "font-semibold text-ink-900", children: "Change password" }), _jsx("p", { className: "text-xs text-ink-500", children: "Minimum 10 characters with an uppercase letter, a lowercase letter and a digit." })] }), passwordError && (_jsxs("p", { role: "alert", className: "flex items-center gap-2 rounded-lg bg-red-50 p-2 text-sm text-red-800", children: [_jsx(AlertCircle, { className: "h-4 w-4" }), " ", passwordError] })), _jsxs("div", { className: "grid gap-4 sm:grid-cols-3", children: [_jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "currentPassword", children: "Current password" }), _jsx("input", { id: "currentPassword", type: "password", className: "input", autoComplete: "current-password", required: true, value: passwords.currentPassword, onChange: (event) => setPasswords({ ...passwords, currentPassword: event.target.value }) })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "newPassword", children: "New password" }), _jsx("input", { id: "newPassword", type: "password", className: "input", autoComplete: "new-password", required: true, minLength: 10, value: passwords.newPassword, onChange: (event) => setPasswords({ ...passwords, newPassword: event.target.value }) })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "confirmPassword", children: "Confirm new password" }), _jsx("input", { id: "confirmPassword", type: "password", className: "input", autoComplete: "new-password", required: true, minLength: 10, value: passwords.confirm, onChange: (event) => setPasswords({ ...passwords, confirm: event.target.value }) })] })] }), _jsx("div", { className: "flex justify-end", children: _jsx("button", { type: "submit", className: "btn-secondary", disabled: changingPassword, children: changingPassword ? 'Updating…' : 'Update password' }) })] }), _jsxs("p", { className: "mt-4 text-center text-xs text-ink-400", children: ["Signed in as user #", user?.id, " \u00B7 accounts are deactivated by administrators, never deleted."] })] }));
}
