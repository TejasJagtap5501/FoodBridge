import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { AlertTriangle, ArrowLeft, CheckCircle2, Clock, MapPin, Package, Pencil, Trash2, Utensils, X } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { donationApi, claimApi } from '@/api/endpoints';
import { Badge, Callout, ErrorState, Modal } from '@/components/Ui';
import { PageHeader } from '@/components/Stats';
import { PageLoader } from '@/components/Loading';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
import { foodTypeLabel, foodTypeStyles, donationStatusStyles, SAFETY_DISCLAIMER } from '@/lib/constants';
import { formatDateTime, formatQuantity, timeUntil } from '@/lib/format';
/**
 * Shared donation detail view. {@code role} decides which actions are offered —
 * and the backend authorises every one of them independently.
 */
export default function DonationDetailsPage({ role }) {
    const { id } = useParams();
    const donationId = Number(id);
    const navigate = useNavigate();
    const toast = useToast();
    const { data, loading, error, reload } = useApi(() => donationApi.get(donationId), [donationId]);
    const [showCancel, setShowCancel] = useState(false);
    const [cancelReason, setCancelReason] = useState('');
    const [showClaim, setShowClaim] = useState(false);
    const [claimMessage, setClaimMessage] = useState('');
    const [suitability, setSuitability] = useState(false);
    const [busy, setBusy] = useState(false);
    if (loading)
        return _jsx(PageLoader, { label: "Loading donation\u2026" });
    if (error)
        return _jsx(ErrorState, { message: error, onRetry: reload });
    if (!data)
        return null;
    const isOwner = role === 'DONOR';
    const canEdit = isOwner && data.status === 'AVAILABLE';
    const canCancel = isOwner && (data.status === 'AVAILABLE' || data.status === 'CLAIMED');
    const canClaim = role === 'NGO' && data.status === 'AVAILABLE' && !data.expired;
    const cancel = async () => {
        setBusy(true);
        try {
            await donationApi.cancel(donationId, cancelReason || 'Cancelled by donor');
            toast.success('Donation cancelled.');
            setShowCancel(false);
            reload();
        }
        catch (err) {
            toast.error(err instanceof Error ? err.message : 'Could not cancel the donation');
        }
        finally {
            setBusy(false);
        }
    };
    const claim = async () => {
        setBusy(true);
        try {
            await claimApi.claim(donationId, { message: claimMessage || undefined, suitabilityCheck: suitability });
            toast.success('Donation claimed. Schedule a pickup to coordinate collection.');
            setShowClaim(false);
            navigate('/ngo/claims');
        }
        catch (err) {
            toast.error(err instanceof Error ? err.message : 'Could not claim this donation');
        }
        finally {
            setBusy(false);
        }
    };
    const backTo = isOwner ? '/donor/donations' : '/ngo/browse';
    return (_jsxs("div", { children: [_jsxs(Link, { to: backTo, className: "mb-4 inline-flex items-center gap-1 text-sm font-medium text-ink-500 hover:text-ink-800", children: [_jsx(ArrowLeft, { className: "h-4 w-4" }), " Back"] }), _jsx(PageHeader, { title: data.foodName, description: `Reference ${data.referenceCode} · ${data.category.name}`, actions: _jsxs(_Fragment, { children: [canClaim && (_jsxs("button", { type: "button", className: "btn-primary", onClick: () => setShowClaim(true), children: [_jsx(CheckCircle2, { className: "h-4 w-4" }), " Claim donation"] })), canEdit && (_jsxs(Link, { to: `/donor/donations/${data.id}/edit`, className: "btn-secondary", children: [_jsx(Pencil, { className: "h-4 w-4" }), " Edit"] })), canCancel && (_jsxs("button", { type: "button", className: "btn-danger", onClick: () => setShowCancel(true), children: [_jsx(Trash2, { className: "h-4 w-4" }), " Cancel"] }))] }) }), _jsxs("div", { className: "grid gap-6 lg:grid-cols-3", children: [_jsxs("div", { className: "space-y-4 lg:col-span-2", children: [_jsxs("div", { className: "card card-body", children: [_jsxs("div", { className: "flex flex-wrap items-center gap-2", children: [_jsx(Badge, { className: donationStatusStyles[data.status], children: data.status }), _jsx(Badge, { className: foodTypeStyles[data.foodType], children: foodTypeLabel[data.foodType] }), data.expired && _jsx(Badge, { className: "bg-red-100 text-red-700", children: "EXPIRED" })] }), data.description && _jsx("p", { className: "mt-3 text-sm text-ink-600", children: data.description }), _jsxs("dl", { className: "mt-5 grid gap-4 sm:grid-cols-2", children: [_jsxs("div", { children: [_jsx("dt", { className: "text-xs font-medium uppercase tracking-wide text-ink-400", children: "Quantity" }), _jsxs("dd", { className: "mt-0.5 flex items-center gap-1.5 text-sm font-medium text-ink-800", children: [_jsx(Package, { className: "h-4 w-4 text-ink-400" }), formatQuantity(data.quantity, data.quantityUnit)] })] }), _jsxs("div", { children: [_jsx("dt", { className: "text-xs font-medium uppercase tracking-wide text-ink-400", children: "Estimated meals" }), _jsxs("dd", { className: "mt-0.5 flex items-center gap-1.5 text-sm font-medium text-ink-800", children: [_jsx(Utensils, { className: "h-4 w-4 text-ink-400" }), " ", data.estimatedMeals] })] }), _jsxs("div", { children: [_jsx("dt", { className: "text-xs font-medium uppercase tracking-wide text-ink-400", children: "Prepared" }), _jsx("dd", { className: "mt-0.5 text-sm text-ink-800", children: formatDateTime(data.preparedAt) })] }), _jsxs("div", { children: [_jsx("dt", { className: "text-xs font-medium uppercase tracking-wide text-ink-400", children: "Expires" }), _jsxs("dd", { className: "mt-0.5 flex items-center gap-1.5 text-sm font-medium text-ink-800", children: [_jsx(Clock, { className: "h-4 w-4 text-ink-400" }), formatDateTime(data.expiresAt), _jsxs("span", { className: "text-xs font-normal text-brand-700", children: ["(", timeUntil(data.expiresAt), ")"] })] })] })] })] }), _jsxs("div", { className: "card card-body", children: [_jsx("h2", { className: "font-semibold text-ink-900", children: "Pickup & food safety" }), _jsxs("dl", { className: "mt-4 space-y-4 text-sm", children: [_jsxs("div", { children: [_jsx("dt", { className: "text-xs font-medium uppercase tracking-wide text-ink-400", children: "Pickup address" }), _jsxs("dd", { className: "mt-0.5 flex items-start gap-1.5 text-ink-800", children: [_jsx(MapPin, { className: "mt-0.5 h-4 w-4 shrink-0 text-ink-400" }), _jsxs("span", { children: [data.pickupAddress, _jsx("br", {}), _jsx("span", { className: "text-ink-500", children: data.city })] })] })] }), data.pickupInstructions && (_jsxs("div", { children: [_jsx("dt", { className: "text-xs font-medium uppercase tracking-wide text-ink-400", children: "Pickup instructions" }), _jsx("dd", { className: "mt-0.5 text-ink-800", children: data.pickupInstructions })] })), data.storageConditions && (_jsxs("div", { children: [_jsx("dt", { className: "text-xs font-medium uppercase tracking-wide text-ink-400", children: "Storage conditions" }), _jsx("dd", { className: "mt-0.5 text-ink-800", children: data.storageConditions })] })), data.allergenInfo && (_jsxs("div", { children: [_jsx("dt", { className: "text-xs font-medium uppercase tracking-wide text-ink-400", children: "Allergen information" }), _jsxs("dd", { className: "mt-0.5 flex items-start gap-1.5 text-amber-800", children: [_jsx(AlertTriangle, { className: "mt-0.5 h-4 w-4 shrink-0" }), data.allergenInfo] })] })), data.handlingInstructions && (_jsxs("div", { children: [_jsx("dt", { className: "text-xs font-medium uppercase tracking-wide text-ink-400", children: "Handling instructions" }), _jsx("dd", { className: "mt-0.5 text-ink-800", children: data.handlingInstructions })] })), _jsxs("div", { children: [_jsx("dt", { className: "text-xs font-medium uppercase tracking-wide text-ink-400", children: "Food safety declaration" }), _jsx("dd", { className: "mt-0.5 flex items-center gap-1.5 text-ink-800", children: data.safetyDeclaration ? (_jsxs(_Fragment, { children: [_jsx(CheckCircle2, { className: "h-4 w-4 text-brand-600" }), " Donor declared the information is accurate"] })) : (_jsxs(_Fragment, { children: [_jsx(X, { className: "h-4 w-4 text-red-600" }), " Not declared"] })) })] })] })] }), _jsx(Callout, { tone: "warning", title: "Food safety responsibility", children: SAFETY_DISCLAIMER })] }), _jsxs("div", { className: "space-y-4", children: [_jsxs("div", { className: "card card-body", children: [_jsx("h2", { className: "font-semibold text-ink-900", children: "Donor" }), _jsx("p", { className: "mt-2 text-sm text-ink-800", children: data.donorName ?? 'Restaurant partner' }), data.donorEmail ? (_jsxs(_Fragment, { children: [_jsx("p", { className: "mt-1 text-sm text-ink-500", children: data.donorEmail }), data.donorPhone && _jsx("p", { className: "text-sm text-ink-500", children: data.donorPhone })] })) : (_jsx("p", { className: "mt-1 text-xs text-ink-400", children: "Contact details are shared with the claiming NGO once a claim exists." }))] }), _jsxs("div", { className: "card card-body", children: [_jsx("h2", { className: "font-semibold text-ink-900", children: "Timeline" }), _jsxs("ul", { className: "mt-3 space-y-3 text-sm", children: [_jsxs("li", { children: [_jsx("p", { className: "font-medium text-ink-800", children: "Published" }), _jsx("p", { className: "text-xs text-ink-500", children: formatDateTime(data.createdAt) })] }), _jsxs("li", { children: [_jsx("p", { className: "font-medium text-ink-800", children: "Last updated" }), _jsx("p", { className: "text-xs text-ink-500", children: formatDateTime(data.updatedAt) })] })] })] })] })] }), _jsxs(Modal, { open: showCancel, title: "Cancel this donation?", description: "The donation will be marked CANCELLED and will no longer appear in NGO searches.", onClose: () => setShowCancel(false), footer: _jsxs(_Fragment, { children: [_jsx("button", { type: "button", className: "btn-secondary", onClick: () => setShowCancel(false), children: "Keep it" }), _jsx("button", { type: "button", className: "btn-danger", onClick: cancel, disabled: busy || !cancelReason.trim(), children: busy ? 'Cancelling…' : 'Cancel donation' })] }), children: [_jsx("label", { className: "label", htmlFor: "reason", children: "Reason (required)" }), _jsx("textarea", { id: "reason", className: "input", rows: 3, value: cancelReason, onChange: (event) => setCancelReason(event.target.value), placeholder: "e.g. the food was not collected in time" })] }), _jsx(Modal, { open: showClaim, title: "Claim this donation", description: "Only verified NGOs can claim. Your organisation will be recorded as the claimant.", onClose: () => setShowClaim(false), footer: _jsxs(_Fragment, { children: [_jsx("button", { type: "button", className: "btn-secondary", onClick: () => setShowClaim(false), children: "Cancel" }), _jsx("button", { type: "button", className: "btn-primary", onClick: claim, disabled: busy || !suitability, children: busy ? 'Claiming…' : 'Confirm claim' })] }), children: _jsxs("div", { className: "space-y-4", children: [_jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "claimMessage", children: "Message to the donor (optional)" }), _jsx("textarea", { id: "claimMessage", className: "input", rows: 3, value: claimMessage, onChange: (event) => setClaimMessage(event.target.value), placeholder: "e.g. We can collect within the next hour" })] }), _jsxs("label", { className: "flex items-start gap-2 rounded-lg border border-ink-200 bg-ink-50 p-3 text-sm text-ink-700", children: [_jsx("input", { type: "checkbox", className: "mt-0.5 h-4 w-4 rounded border-ink-300 text-brand-600", checked: suitability, onChange: (event) => setSuitability(event.target.checked) }), _jsx("span", { children: "Our organisation has independently assessed this food as suitable for our beneficiaries, and we will follow the stated storage and handling instructions." })] })] }) })] }));
}
