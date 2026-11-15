import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Check, Mail, MapPin, Search, ShieldCheck, X } from 'lucide-react';
import { useState } from 'react';
import { adminApi } from '@/api/endpoints';
import { toApiError } from '@/api/client';
import { PageHeader } from '@/components/Stats';
import { TableSkeleton } from '@/components/Loading';
import { Badge, EmptyState, ErrorState, Modal, Pagination } from '@/components/Ui';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
import { verificationStyles } from '@/lib/constants';
import { formatDate } from '@/lib/format';
/**
 * Approval gate for NGOs. Only APPROVED organisations can claim donations —
 * enforced in ClaimService, not just here.
 */
export default function AdminNgoVerificationPage() {
    const toast = useToast();
    const [status, setStatus] = useState('PENDING');
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
    const [decision, setDecision] = useState(null);
    const { data, loading, error, reload } = useApi(() => adminApi.ngos({ status: status === 'ALL' ? undefined : status, search, page, size: 12, sortBy: 'createdAt', direction: 'asc' }), [status, search, page]);
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: "NGO verification", description: "Verify registration details before an organisation can claim food. Every decision is written to the audit log." }), _jsxs("div", { className: "mb-4 flex flex-wrap items-center gap-2", children: [['PENDING', 'APPROVED', 'REJECTED', 'ALL'].map((option) => (_jsx("button", { type: "button", onClick: () => { setStatus(option); setPage(0); }, className: `rounded-full px-3 py-1.5 text-xs font-semibold transition ${status === option ? 'bg-brand-600 text-white' : 'bg-white text-ink-600 ring-1 ring-ink-200 hover:bg-ink-50'}`, children: option === 'ALL' ? 'All' : option.charAt(0) + option.slice(1).toLowerCase() }, option))), _jsxs("div", { className: "relative ml-auto w-full sm:w-64", children: [_jsx(Search, { className: "pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" }), _jsx("label", { className: "sr-only", htmlFor: "ngo-search", children: "Search NGOs" }), _jsx("input", { id: "ngo-search", className: "input pl-9", placeholder: "Organisation or contact", value: search, onChange: (event) => { setSearch(event.target.value); setPage(0); } })] })] }), loading ? (_jsx(TableSkeleton, { rows: 5, columns: 4 })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : !data || data.content.length === 0 ? (_jsx(EmptyState, { icon: _jsx(ShieldCheck, { className: "h-5 w-5" }), title: status === 'PENDING' ? 'No applications waiting' : 'No organisations match these filters', description: status === 'PENDING'
                    ? 'Every NGO application has been reviewed. New applications appear here automatically.'
                    : 'Try another verification status or clear the search box.' })) : (_jsxs(_Fragment, { children: [_jsx("ul", { className: "grid gap-4 lg:grid-cols-2", children: data.content.map((application) => (_jsxs("li", { className: "card card-body", children: [_jsxs("div", { className: "flex items-start justify-between gap-3", children: [_jsxs("div", { className: "min-w-0", children: [_jsx("p", { className: "font-semibold text-ink-900", children: application.organizationName }), _jsxs("p", { className: "text-xs text-ink-500", children: ["Reg. ", application.registrationNumber] })] }), _jsx(Badge, { className: verificationStyles[application.verificationStatus], children: application.verificationStatus })] }), _jsxs("dl", { className: "mt-3 space-y-1.5 text-sm", children: [_jsxs("div", { className: "flex items-start gap-2 text-ink-600", children: [_jsx(ShieldCheck, { className: "mt-0.5 h-3.5 w-3.5 shrink-0 text-ink-400" }), _jsx("span", { children: application.contactPerson })] }), _jsxs("div", { className: "flex items-start gap-2 text-ink-600", children: [_jsx(Mail, { className: "mt-0.5 h-3.5 w-3.5 shrink-0 text-ink-400" }), _jsxs("span", { children: [application.email, application.phone ? ` · ${application.phone}` : ''] })] }), _jsxs("div", { className: "flex items-start gap-2 text-ink-600", children: [_jsx(MapPin, { className: "mt-0.5 h-3.5 w-3.5 shrink-0 text-ink-400" }), _jsxs("span", { children: [application.addressLine1, application.addressLine2 ? `, ${application.addressLine2}` : '', ", ", application.city, ", ", application.state, ' ', application.postalCode] })] })] }), _jsxs("p", { className: "mt-2 text-xs text-ink-400", children: ["Applied ", formatDate(application.appliedAt), application.verifiedAt ? ` · decided ${formatDate(application.verifiedAt)}` : '', application.website ? ` · ${application.website}` : ''] }), application.verificationNote && (_jsxs("p", { className: "mt-2 rounded-lg bg-ink-50 p-2 text-xs text-ink-600", children: ["Note: ", application.verificationNote] })), application.verificationStatus === 'PENDING' ? (_jsxs("div", { className: "mt-4 flex gap-2", children: [_jsxs("button", { type: "button", className: "btn-primary btn-sm flex-1", onClick: () => setDecision({ application, approved: true }), children: [_jsx(Check, { className: "h-4 w-4" }), " Approve"] }), _jsxs("button", { type: "button", className: "btn-secondary btn-sm flex-1", onClick: () => setDecision({ application, approved: false }), children: [_jsx(X, { className: "h-4 w-4" }), " Reject"] })] })) : (_jsx("div", { className: "mt-4 flex gap-2", children: _jsx("button", { type: "button", className: "btn-secondary btn-sm flex-1", onClick: () => setDecision({ application, approved: application.verificationStatus !== 'APPROVED' }), children: application.verificationStatus === 'APPROVED' ? 'Revoke approval' : 'Re-approve' }) }))] }, application.profileId))) }), _jsx(Pagination, { page: data.page, totalPages: data.totalPages, totalElements: data.totalElements, onChange: setPage })] })), _jsx(DecisionModal, { target: decision, onClose: () => setDecision(null), onDone: () => { setDecision(null); reload(); }, toast: toast })] }));
}
function DecisionModal({ target, onClose, onDone, toast, }) {
    const [note, setNote] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);
    const submit = async () => {
        if (!target)
            return;
        if (!target.approved && note.trim().length < 5) {
            setError('Add a short reason so the organisation understands the decision.');
            return;
        }
        setBusy(true);
        setError(null);
        try {
            await adminApi.decideNgo(target.application.profileId, target.approved, note.trim() || undefined);
            toast.success(target.approved ? `${target.application.organizationName} approved.` : `${target.application.organizationName} rejected.`);
            setNote('');
            onDone();
        }
        catch (caught) {
            setError(toApiError(caught).message);
        }
        finally {
            setBusy(false);
        }
    };
    return (_jsxs(Modal, { open: Boolean(target), title: target?.approved ? 'Approve this organisation?' : 'Reject this organisation?', description: target?.approved
            ? 'The NGO will be able to claim donations immediately and will receive a notification.'
            : 'The NGO cannot claim donations. Your note is included in the notification and the audit log.', onClose: onClose, footer: _jsxs(_Fragment, { children: [_jsx("button", { type: "button", className: "btn-secondary", onClick: onClose, children: "Cancel" }), _jsx("button", { type: "button", className: target?.approved ? 'btn-primary' : 'btn-danger', onClick: submit, disabled: busy, children: busy ? 'Saving…' : target?.approved ? 'Approve' : 'Reject' })] }), children: [target && (_jsxs("p", { className: "mb-3 text-sm text-ink-600", children: [target.application.organizationName, " \u00B7 reg. ", target.application.registrationNumber] })), _jsx("label", { className: "label", htmlFor: "decision-note", children: target?.approved ? 'Note (optional)' : 'Reason for rejection' }), _jsx("textarea", { id: "decision-note", rows: 3, className: "input", value: note, onChange: (event) => setNote(event.target.value), placeholder: target?.approved ? 'e.g. registration verified against public records' : 'e.g. registration number could not be verified' }), error && _jsx("p", { className: "field-error", children: error })] }));
}
