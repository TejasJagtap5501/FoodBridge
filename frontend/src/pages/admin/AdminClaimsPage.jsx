import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Handshake, Search, ShieldAlert } from 'lucide-react';
import { useState } from 'react';
import { adminApi } from '@/api/endpoints';
import { toApiError } from '@/api/client';
import { PageHeader } from '@/components/Stats';
import { TableSkeleton } from '@/components/Loading';
import { Badge, EmptyState, ErrorState, Modal, Pagination } from '@/components/Ui';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
import { claimStatusStyles } from '@/lib/constants';
import { formatDateTime, formatQuantity } from '@/lib/format';
/** Platform-wide claim oversight, including administrator force-cancel. */
export default function AdminClaimsPage() {
    const toast = useToast();
    const [search, setSearch] = useState('');
    const [status, setStatus] = useState('');
    const [page, setPage] = useState(0);
    const [target, setTarget] = useState(null);
    const { data, loading, error, reload } = useApi(() => adminApi.claims({ search, status: status || undefined, page, size: 20, sortBy: 'claimedAt', direction: 'desc' }), [search, status, page]);
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: "Claims", description: "Every claim across the platform. Force-cancelling releases the donation back to AVAILABLE and is audited." }), _jsxs("div", { className: "card card-body mb-4 grid gap-3 sm:grid-cols-3", children: [_jsxs("div", { className: "relative sm:col-span-2", children: [_jsx(Search, { className: "pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" }), _jsx("label", { className: "sr-only", htmlFor: "claim-search", children: "Search claims" }), _jsx("input", { id: "claim-search", className: "input pl-9", placeholder: "Food, donor or NGO", value: search, onChange: (event) => { setSearch(event.target.value); setPage(0); } })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "claim-status", children: "Status" }), _jsxs("select", { id: "claim-status", className: "input", value: status, onChange: (event) => { setStatus(event.target.value); setPage(0); }, children: [_jsx("option", { value: "", children: "All statuses" }), _jsx("option", { value: "ACTIVE", children: "Active" }), _jsx("option", { value: "COMPLETED", children: "Completed" }), _jsx("option", { value: "CANCELLED", children: "Cancelled" })] })] })] }), loading ? (_jsx(TableSkeleton, { rows: 8, columns: 5 })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : !data || data.content.length === 0 ? (_jsx(EmptyState, { icon: _jsx(Handshake, { className: "h-5 w-5" }), title: "No claims match these filters" })) : (_jsxs("div", { className: "card overflow-hidden", children: [_jsx("div", { className: "overflow-x-auto", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { scope: "col", children: "Donation" }), _jsx("th", { scope: "col", children: "Donor \u2192 NGO" }), _jsx("th", { scope: "col", children: "Quantity" }), _jsx("th", { scope: "col", children: "Claimed" }), _jsx("th", { scope: "col", children: "Pickup" }), _jsx("th", { scope: "col", children: "Status" }), _jsx("th", { scope: "col", className: "text-right", children: "Action" })] }) }), _jsx("tbody", { children: data.content.map((claim) => (_jsxs("tr", { children: [_jsxs("td", { children: [_jsx("p", { className: "font-medium text-ink-900", children: claim.foodName }), _jsx("p", { className: "font-mono text-[11px] text-ink-500", children: claim.donationReference })] }), _jsxs("td", { className: "text-xs text-ink-700", children: [claim.donorName, _jsx("span", { className: "mx-1 text-ink-400", children: "\u2192" }), claim.ngoName] }), _jsx("td", { className: "whitespace-nowrap text-ink-700", children: formatQuantity(claim.quantity, claim.quantityUnit) }), _jsx("td", { className: "whitespace-nowrap text-xs text-ink-600", children: formatDateTime(claim.claimedAt) }), _jsx("td", { className: "text-xs text-ink-600", children: claim.pickup ? (_jsxs(_Fragment, { children: [formatDateTime(claim.pickup.scheduledAt), _jsx("span", { className: "mt-0.5 block text-ink-400", children: claim.pickup.status })] })) : '—' }), _jsx("td", { children: _jsx(Badge, { className: claimStatusStyles[claim.status], children: claim.status }) }), _jsx("td", { children: _jsx("div", { className: "flex justify-end", children: claim.status === 'ACTIVE' ? (_jsxs("button", { type: "button", className: "btn-secondary btn-sm", onClick: () => setTarget(claim), children: [_jsx(ShieldAlert, { className: "h-3.5 w-3.5" }), " Force cancel"] })) : (_jsx("span", { className: "text-xs text-ink-400", children: "Closed" })) }) })] }, claim.id))) })] }) }), _jsx(Pagination, { page: data.page, totalPages: data.totalPages, totalElements: data.totalElements, onChange: setPage })] })), _jsx(ForceCancelModal, { claim: target, onClose: () => setTarget(null), onDone: () => { setTarget(null); reload(); }, toast: toast })] }));
}
function ForceCancelModal({ claim, onClose, onDone, toast, }) {
    const [reason, setReason] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);
    const submit = async () => {
        if (!claim)
            return;
        if (reason.trim().length < 5) {
            setError('A reason of at least 5 characters is required and will be audited.');
            return;
        }
        setBusy(true);
        setError(null);
        try {
            await adminApi.cancelClaim(claim.id, reason.trim());
            toast.success('Claim force-cancelled. The donation is AVAILABLE again.');
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
    return (_jsxs(Modal, { open: Boolean(claim), title: "Force-cancel this claim?", description: "Both parties are notified, the pickup is cancelled and the donation becomes AVAILABLE in the same transaction.", onClose: onClose, footer: _jsxs(_Fragment, { children: [_jsx("button", { type: "button", className: "btn-secondary", onClick: onClose, children: "Keep claim" }), _jsx("button", { type: "button", className: "btn-danger", onClick: submit, disabled: busy, children: busy ? 'Cancelling…' : 'Force cancel' })] }), children: [claim && (_jsxs("p", { className: "mb-3 text-sm text-ink-600", children: [claim.foodName, " \u00B7 ", claim.donorName, " \u2192 ", claim.ngoName] })), _jsx("label", { className: "label", htmlFor: "force-reason", children: "Reason (required, audited)" }), _jsx("textarea", { id: "force-reason", rows: 3, className: "input", value: reason, onChange: (event) => setReason(event.target.value) }), error && _jsx("p", { className: "field-error", children: error })] }));
}
