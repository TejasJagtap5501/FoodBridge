import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { CalendarClock, Handshake, MapPin, X } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { claimApi, pickupApi } from '@/api/endpoints';
import { toApiError } from '@/api/client';
import { PageHeader } from '@/components/Stats';
import { TableSkeleton } from '@/components/Loading';
import { Badge, EmptyState, ErrorState, Modal, Pagination } from '@/components/Ui';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
import { claimStatusStyles } from '@/lib/constants';
import { formatDateTime, formatQuantity, timeUntil } from '@/lib/format';
/**
 * Claims list shared by donors (claims received on their donations) and NGOs
 * (claims they placed). Both cancel and pickup scheduling post to the same
 * endpoints; the backend re-checks ownership on every call.
 */
export default function ClaimsPage({ role }) {
    const [status, setStatus] = useState('ALL');
    const [page, setPage] = useState(0);
    const [cancelTarget, setCancelTarget] = useState(null);
    const [scheduleTarget, setScheduleTarget] = useState(null);
    const { data, loading, error, reload } = useApi(() => claimApi.mine({ status: status === 'ALL' ? undefined : status, page, size: 10 }), [status, page]);
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: role === 'DONOR' ? 'Claims on my donations' : 'My claims', description: role === 'DONOR'
                    ? 'NGOs that have claimed your surplus food, with pickup coordination.'
                    : 'Every donation you have claimed and its fulfilment status.', actions: role === 'NGO' ? _jsxs(Link, { to: "/ngo/browse", className: "btn-primary", children: [_jsx(Handshake, { className: "h-4 w-4" }), " Claim more food"] }) : undefined }), _jsx("div", { className: "mb-4 flex flex-wrap gap-2", children: ['ALL', 'ACTIVE', 'COMPLETED', 'CANCELLED'].map((option) => (_jsx("button", { type: "button", onClick: () => { setStatus(option); setPage(0); }, className: `rounded-full px-3 py-1.5 text-xs font-semibold transition ${status === option ? 'bg-brand-600 text-white' : 'bg-white text-ink-600 ring-1 ring-ink-200 hover:bg-ink-50'}`, children: option === 'ALL' ? 'All' : option.charAt(0) + option.slice(1).toLowerCase() }, option))) }), loading ? (_jsx(TableSkeleton, { rows: 5, columns: 5 })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : !data || data.content.length === 0 ? (_jsx(EmptyState, { icon: _jsx(Handshake, { className: "h-5 w-5" }), title: status === 'ALL' ? 'No claims yet' : `No ${status.toLowerCase()} claims`, description: role === 'NGO'
                    ? 'Browse available donations and claim the food your organisation can redistribute safely.'
                    : 'When an NGO claims one of your donations it will appear here, along with the generated pickup.', action: role === 'NGO' ? _jsx(Link, { to: "/ngo/browse", className: "btn-primary btn-sm", children: "Browse donations" }) : undefined })) : (_jsxs(_Fragment, { children: [_jsx("div", { className: "space-y-3 md:hidden", children: data.content.map((claim) => (_jsx(ClaimCard, { claim: claim, role: role, onCancel: setCancelTarget, onSchedule: setScheduleTarget }, claim.id))) }), _jsxs("div", { className: "card hidden overflow-hidden md:block", children: [_jsx("div", { className: "overflow-x-auto", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { scope: "col", children: "Donation" }), _jsx("th", { scope: "col", children: role === 'DONOR' ? 'NGO' : 'Donor' }), _jsx("th", { scope: "col", children: "Quantity" }), _jsx("th", { scope: "col", children: "Claimed" }), _jsx("th", { scope: "col", children: "Status" }), _jsx("th", { scope: "col", children: "Pickup" }), _jsx("th", { scope: "col", className: "text-right", children: "Actions" })] }) }), _jsx("tbody", { children: data.content.map((claim) => (_jsxs("tr", { children: [_jsxs("td", { children: [_jsx("p", { className: "font-medium text-ink-900", children: claim.foodName }), _jsxs("p", { className: "text-xs text-ink-500", children: [claim.donationReference, " \u00B7 ", claim.city] })] }), _jsx("td", { className: "text-ink-700", children: role === 'DONOR' ? claim.ngoName : claim.donorName }), _jsx("td", { className: "whitespace-nowrap text-ink-700", children: formatQuantity(claim.quantity, claim.quantityUnit) }), _jsx("td", { className: "whitespace-nowrap text-xs text-ink-600", children: formatDateTime(claim.claimedAt) }), _jsx("td", { children: _jsx(Badge, { className: claimStatusStyles[claim.status], children: claim.status }) }), _jsx("td", { className: "text-xs", children: claim.pickup ? (_jsxs("span", { className: "text-ink-700", children: [formatDateTime(claim.pickup.scheduledAt), _jsx("span", { className: "mt-0.5 block text-ink-400", children: claim.pickup.status })] })) : claim.status === 'ACTIVE' ? (_jsx("span", { className: "text-amber-700", children: "Not scheduled" })) : (_jsx("span", { className: "text-ink-400", children: "\u2014" })) }), _jsx("td", { children: _jsxs("div", { className: "flex justify-end gap-1.5", children: [_jsx(Link, { to: role === 'DONOR' ? `/donor/donations/${claim.donationId}` : `/ngo/donations/${claim.donationId}`, className: "btn-ghost btn-sm", children: "View" }), claim.status === 'ACTIVE' && !claim.pickup?.ngoConfirmedAt && !claim.pickup?.donorConfirmedAt && (_jsx("button", { type: "button", className: "btn-secondary btn-sm", onClick: () => setScheduleTarget(claim), children: claim.pickup ? 'Reschedule' : 'Schedule' })), claim.status === 'ACTIVE' && _jsx(CancelButton, { claim: claim, role: role, onCancel: setCancelTarget })] }) })] }, claim.id))) })] }) }), _jsx(Pagination, { page: data.page, totalPages: data.totalPages, totalElements: data.totalElements, onChange: setPage })] }), _jsx("div", { className: "md:hidden", children: _jsx(Pagination, { page: data.page, totalPages: data.totalPages, totalElements: data.totalElements, onChange: setPage }) })] })), _jsx(CancelClaimModal, { claim: cancelTarget, role: role, onClose: () => setCancelTarget(null), onDone: () => { setCancelTarget(null); reload(); } }), _jsx(SchedulePickupModal, { claim: scheduleTarget, onClose: () => setScheduleTarget(null), onDone: () => { setScheduleTarget(null); reload(); } })] }));
}
// ---------------------------------------------------------------------------
// Mobile card
// ---------------------------------------------------------------------------
function ClaimCard({ claim, role, onCancel, onSchedule, }) {
    return (_jsxs("div", { className: "card card-body", children: [_jsxs("div", { className: "flex items-start justify-between gap-2", children: [_jsxs("div", { className: "min-w-0", children: [_jsx("p", { className: "truncate font-semibold text-ink-900", children: claim.foodName }), _jsx("p", { className: "text-xs text-ink-500", children: role === 'DONOR' ? claim.ngoName : claim.donorName })] }), _jsx(Badge, { className: claimStatusStyles[claim.status], children: claim.status })] }), _jsxs("p", { className: "mt-2 flex items-center gap-1 text-xs text-ink-500", children: [_jsx(MapPin, { className: "h-3.5 w-3.5" }), " ", claim.city, " \u00B7 ", formatQuantity(claim.quantity, claim.quantityUnit)] }), claim.pickup && (_jsxs("p", { className: "mt-1 flex items-center gap-1 text-xs text-ink-500", children: [_jsx(CalendarClock, { className: "h-3.5 w-3.5" }), " ", formatDateTime(claim.pickup.scheduledAt), " (", claim.pickup.status, ")"] })), claim.status === 'ACTIVE' && (_jsxs("div", { className: "mt-3 flex gap-2", children: [_jsx("button", { type: "button", className: "btn-secondary btn-sm flex-1", onClick: () => onSchedule(claim), children: claim.pickup ? 'Reschedule' : 'Schedule pickup' }), _jsx(CancelButton, { claim: claim, role: role, onCancel: onCancel })] })), _jsx("p", { className: "mt-2 text-xs text-ink-400", children: timeUntil(claim.expiresAt) })] }));
}
function CancelButton({ claim, role, onCancel }) {
    // Both parties may cancel while the claim is active and before handover; the
    // backend rejects anything else with a 409/403, so no client-side state machine.
    if (claim.status !== 'ACTIVE')
        return null;
    return (_jsxs("button", { type: "button", className: "btn-ghost btn-sm text-red-600 hover:bg-red-50", onClick: () => onCancel(claim), children: [_jsx(X, { className: "h-3.5 w-3.5" }), " ", role === 'DONOR' ? 'Cancel' : 'Cancel claim'] }));
}
// ---------------------------------------------------------------------------
// Cancel claim dialog
// ---------------------------------------------------------------------------
function CancelClaimModal({ claim, role, onClose, onDone, }) {
    const toast = useToast();
    const [reason, setReason] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);
    const submit = async () => {
        if (!claim)
            return;
        if (reason.trim().length < 5) {
            setError('Please give a short reason (at least 5 characters).');
            return;
        }
        setBusy(true);
        setError(null);
        try {
            await claimApi.cancel(claim.id, reason.trim());
            toast.success('Claim cancelled. The donation is available again.');
            setReason('');
            onDone();
        }
        catch (caught) {
            setError(toApiError(caught).message);
        }
        finally {
            setBusy(false);
        }
    };
    return (_jsxs(Modal, { open: Boolean(claim), title: "Cancel this claim?", description: role === 'DONOR'
            ? 'The donation returns to the AVAILABLE pool so another verified NGO can claim it.'
            : 'The donation returns to the AVAILABLE pool for other NGOs.', onClose: () => { setReason(''); setError(null); onClose(); }, footer: _jsxs(_Fragment, { children: [_jsx("button", { type: "button", className: "btn-secondary", onClick: () => { setReason(''); setError(null); onClose(); }, children: "Keep claim" }), _jsx("button", { type: "button", className: "btn-danger", onClick: submit, disabled: busy, children: busy ? 'Cancelling…' : 'Cancel claim' })] }), children: [claim && _jsxs("p", { className: "mb-3 text-sm text-ink-600", children: [claim.foodName, " \u00B7 ", claim.donationReference] }), _jsx("label", { className: "label", htmlFor: "cancel-reason", children: "Reason" }), _jsx("textarea", { id: "cancel-reason", rows: 3, className: "input", value: reason, onChange: (event) => setReason(event.target.value), placeholder: "e.g. food spoiled before pickup could be arranged" }), error && _jsx("p", { className: "field-error", children: error })] }));
}
// ---------------------------------------------------------------------------
// Schedule pickup dialog
// ---------------------------------------------------------------------------
function SchedulePickupModal({ claim, onClose, onDone, }) {
    const toast = useToast();
    const [scheduledAt, setScheduledAt] = useState('');
    const [instructions, setInstructions] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);
    const latest = claim ? new Date(claim.expiresAt).getTime() : Date.now();
    const maxValue = new Date(latest - 5 * 60_000).toISOString().slice(0, 16);
    const submit = async () => {
        if (!claim)
            return;
        if (!scheduledAt) {
            setError('Choose a pickup date and time.');
            return;
        }
        setBusy(true);
        setError(null);
        try {
            await pickupApi.schedule(claim.id, new Date(scheduledAt).toISOString(), instructions.trim() || undefined);
            toast.success(claim.pickup ? 'Pickup rescheduled.' : 'Pickup scheduled. The other party has been notified.');
            setScheduledAt('');
            setInstructions('');
            onDone();
        }
        catch (caught) {
            setError(toApiError(caught).message);
        }
        finally {
            setBusy(false);
        }
    };
    return (_jsxs(Modal, { open: Boolean(claim), title: claim?.pickup ? 'Reschedule pickup' : 'Schedule pickup', description: "A pickup must happen before the food expires. Both parties receive a notification.", onClose: () => { setError(null); onClose(); }, footer: _jsxs(_Fragment, { children: [_jsx("button", { type: "button", className: "btn-secondary", onClick: () => { setError(null); onClose(); }, children: "Cancel" }), _jsx("button", { type: "button", className: "btn-primary", onClick: submit, disabled: busy, children: busy ? 'Saving…' : 'Confirm pickup' })] }), children: [claim && (_jsxs("p", { className: "mb-3 text-sm text-ink-600", children: [claim.foodName, " \u00B7 expires ", formatDateTime(claim.expiresAt)] })), _jsx("label", { className: "label", htmlFor: "scheduledAt", children: "Pickup date & time" }), _jsx("input", { id: "scheduledAt", type: "datetime-local", className: "input", min: new Date().toISOString().slice(0, 16), max: maxValue, value: scheduledAt, onChange: (event) => setScheduledAt(event.target.value) }), _jsx("label", { className: "label mt-3", htmlFor: "instructions", children: "Instructions for the other party" }), _jsx("textarea", { id: "instructions", rows: 2, className: "input", value: instructions, onChange: (event) => setInstructions(event.target.value), placeholder: "e.g. call on arrival, loading bay at the rear" }), error && _jsx("p", { className: "field-error", children: error })] }));
}
