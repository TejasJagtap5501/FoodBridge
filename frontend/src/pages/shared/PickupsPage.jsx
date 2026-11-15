import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { CheckCircle2, CalendarClock, PackageCheck, PackageX, Truck } from 'lucide-react';
import { useState } from 'react';
import { pickupApi } from '@/api/endpoints';
import { toApiError } from '@/api/client';
import { PageHeader } from '@/components/Stats';
import { TableSkeleton } from '@/components/Loading';
import { Badge, EmptyState, ErrorState, Modal } from '@/components/Ui';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
import { pickupStatusStyles } from '@/lib/constants';
import { formatDateTime } from '@/lib/format';
/**
 * Two-party pickup workflow.
 *
 * The donor confirms the handover, the NGO confirms the collection; the backend
 * completes the donation only once both confirmations exist. This page simply
 * offers whichever action belongs to the signed-in role.
 */
export default function PickupsPage({ role }) {
    const [filter, setFilter] = useState('UPCOMING');
    const toast = useToast();
    const [busyId, setBusyId] = useState(null);
    const [cancelTarget, setCancelTarget] = useState(null);
    const { data, loading, error, reload } = useApi(() => pickupApi.mine(), []);
    const act = async (pickup, action) => {
        setBusyId(pickup.id);
        try {
            const updated = action === 'handover'
                ? await pickupApi.confirmHandover(pickup.id)
                : await pickupApi.confirmCollection(pickup.id);
            if (updated.status === 'COMPLETED') {
                toast.success('Pickup complete — the donation is now marked as completed.');
            }
            else {
                toast.success(action === 'handover' ? 'Handover confirmed. Waiting for the NGO to confirm collection.' : 'Collection confirmed. Waiting for the donor to confirm the handover.');
            }
            reload();
        }
        catch (caught) {
            toast.error(toApiError(caught).message);
        }
        finally {
            setBusyId(null);
        }
    };
    const visible = (data ?? []).filter((pickup) => {
        if (filter === 'ALL')
            return true;
        if (filter === 'UPCOMING')
            return pickup.status === 'SCHEDULED' || pickup.status === 'HANDED_OVER';
        return pickup.status === filter;
    });
    const counters = {
        upcoming: (data ?? []).filter((p) => p.status === 'SCHEDULED' || p.status === 'HANDED_OVER').length,
        completed: (data ?? []).filter((p) => p.status === 'COMPLETED').length,
        cancelled: (data ?? []).filter((p) => p.status === 'CANCELLED').length,
    };
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: "Pickups", description: "Both the donor and the NGO confirm the handover. The donation completes only when both have confirmed." }), _jsxs("div", { className: "mb-4 grid gap-3 sm:grid-cols-3", children: [_jsxs("button", { type: "button", onClick: () => setFilter('UPCOMING'), className: `card card-body text-left transition ${filter === 'UPCOMING' ? 'ring-2 ring-brand-500' : 'hover:bg-ink-50'}`, children: [_jsx("p", { className: "text-xs font-medium uppercase tracking-wide text-ink-500", children: "Upcoming" }), _jsx("p", { className: "mt-1 text-xl font-bold text-ink-900", children: counters.upcoming })] }), _jsxs("button", { type: "button", onClick: () => setFilter('COMPLETED'), className: `card card-body text-left transition ${filter === 'COMPLETED' ? 'ring-2 ring-brand-500' : 'hover:bg-ink-50'}`, children: [_jsx("p", { className: "text-xs font-medium uppercase tracking-wide text-ink-500", children: "Completed" }), _jsx("p", { className: "mt-1 text-xl font-bold text-ink-900", children: counters.completed })] }), _jsxs("button", { type: "button", onClick: () => setFilter('CANCELLED'), className: `card card-body text-left transition ${filter === 'CANCELLED' ? 'ring-2 ring-brand-500' : 'hover:bg-ink-50'}`, children: [_jsx("p", { className: "text-xs font-medium uppercase tracking-wide text-ink-500", children: "Cancelled" }), _jsx("p", { className: "mt-1 text-xl font-bold text-ink-900", children: counters.cancelled })] })] }), filter !== 'ALL' && (_jsx("button", { type: "button", className: "mb-4 text-xs font-semibold text-brand-700 hover:underline", onClick: () => setFilter('ALL'), children: "Show all pickups" })), loading ? (_jsx(TableSkeleton, { rows: 4, columns: 4 })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : visible.length === 0 ? (_jsx(EmptyState, { icon: _jsx(Truck, { className: "h-5 w-5" }), title: filter === 'UPCOMING' ? 'No upcoming pickups' : 'Nothing here yet', description: role === 'NGO'
                    ? 'Schedule a pickup from an active claim and it will appear here.'
                    : 'When an NGO schedules a pickup for one of your claimed donations you will see it here.' })) : (_jsx("ul", { className: "space-y-3", children: visible.map((pickup) => {
                    const myConfirmation = role === 'DONOR' ? pickup.donorConfirmedAt : pickup.ngoConfirmedAt;
                    const otherConfirmation = role === 'DONOR' ? pickup.ngoConfirmedAt : pickup.donorConfirmedAt;
                    const canConfirm = pickup.status !== 'COMPLETED' && pickup.status !== 'CANCELLED' && !myConfirmation;
                    return (_jsxs("li", { className: "card card-body", children: [_jsxs("div", { className: "flex flex-wrap items-start justify-between gap-3", children: [_jsxs("div", { className: "min-w-0", children: [_jsxs("div", { className: "flex flex-wrap items-center gap-2", children: [_jsx("p", { className: "font-semibold text-ink-900", children: pickup.foodName }), _jsx(Badge, { className: pickupStatusStyles[pickup.status], children: pickup.status })] }), _jsxs("p", { className: "mt-0.5 text-xs text-ink-500", children: [pickup.donationReference, " \u00B7 ", role === 'DONOR' ? `NGO: ${pickup.ngoName}` : `Donor: ${pickup.donorName}`] }), _jsxs("p", { className: "mt-2 flex items-center gap-1.5 text-sm text-ink-700", children: [_jsx(CalendarClock, { className: "h-4 w-4 text-ink-400" }), " ", formatDateTime(pickup.scheduledAt)] }), pickup.instructions && _jsxs("p", { className: "mt-1 text-xs text-ink-500", children: ["Instructions: ", pickup.instructions] }), pickup.cancelledReason && _jsxs("p", { className: "mt-1 text-xs text-red-600", children: ["Cancelled: ", pickup.cancelledReason] })] }), _jsxs("div", { className: "flex flex-col items-stretch gap-2 sm:items-end", children: [canConfirm && (_jsxs("button", { type: "button", className: "btn-primary btn-sm", disabled: busyId === pickup.id, onClick: () => act(pickup, role === 'DONOR' ? 'handover' : 'collection'), children: [role === 'DONOR' ? _jsx(PackageCheck, { className: "h-4 w-4" }) : _jsx(Truck, { className: "h-4 w-4" }), busyId === pickup.id
                                                        ? 'Confirming…'
                                                        : role === 'DONOR'
                                                            ? 'Confirm handover'
                                                            : 'Confirm collection'] })), pickup.status !== 'COMPLETED' && pickup.status !== 'CANCELLED' && (_jsxs("button", { type: "button", className: "btn-ghost btn-sm text-red-600 hover:bg-red-50", onClick: () => setCancelTarget(pickup), children: [_jsx(PackageX, { className: "h-3.5 w-3.5" }), " Cancel pickup"] }))] })] }), _jsxs("div", { className: "mt-3 flex flex-wrap gap-3 border-t border-ink-100 pt-3 text-xs", children: [_jsxs("span", { className: pickup.donorConfirmedAt ? 'text-brand-700' : 'text-ink-400', children: [_jsx(CheckCircle2, { className: "mr-1 inline h-3.5 w-3.5" }), "Donor handover ", pickup.donorConfirmedAt ? `confirmed ${formatDateTime(pickup.donorConfirmedAt)}` : 'pending'] }), _jsxs("span", { className: pickup.ngoConfirmedAt ? 'text-brand-700' : 'text-ink-400', children: [_jsx(CheckCircle2, { className: "mr-1 inline h-3.5 w-3.5" }), "NGO collection ", pickup.ngoConfirmedAt ? `confirmed ${formatDateTime(pickup.ngoConfirmedAt)}` : 'pending'] }), otherConfirmation && !myConfirmation && (_jsx("span", { className: "font-medium text-amber-700", children: "The other party has already confirmed \u2014 your confirmation completes it." }))] })] }, pickup.id));
                }) })), _jsx(CancelPickupModal, { pickup: cancelTarget, onClose: () => setCancelTarget(null), onDone: () => { setCancelTarget(null); reload(); } })] }));
}
function CancelPickupModal({ pickup, onClose, onDone }) {
    const toast = useToast();
    const [reason, setReason] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);
    const submit = async () => {
        if (!pickup)
            return;
        if (reason.trim().length < 5) {
            setError('Please give a short reason (at least 5 characters).');
            return;
        }
        setBusy(true);
        setError(null);
        try {
            await pickupApi.cancel(pickup.id, reason.trim());
            toast.success('Pickup cancelled and the claim released.');
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
    return (_jsxs(Modal, { open: Boolean(pickup), title: "Cancel this pickup?", description: "Cancelling also cancels the claim, which returns the donation to the AVAILABLE pool.", onClose: () => { setReason(''); setError(null); onClose(); }, footer: _jsxs(_Fragment, { children: [_jsx("button", { type: "button", className: "btn-secondary", onClick: () => { setReason(''); setError(null); onClose(); }, children: "Keep pickup" }), _jsx("button", { type: "button", className: "btn-danger", onClick: submit, disabled: busy, children: busy ? 'Cancelling…' : 'Cancel pickup' })] }), children: [pickup && _jsxs("p", { className: "mb-3 text-sm text-ink-600", children: [pickup.foodName, " \u00B7 ", formatDateTime(pickup.scheduledAt)] }), _jsx("label", { className: "label", htmlFor: "pickup-cancel-reason", children: "Reason" }), _jsx("textarea", { id: "pickup-cancel-reason", rows: 3, className: "input", value: reason, onChange: (event) => setReason(event.target.value), placeholder: "e.g. vehicle breakdown, food no longer suitable" }), error && _jsx("p", { className: "field-error", children: error })] }));
}
