import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ListChecks, Plus } from 'lucide-react';
import { useEffect, useState } from 'react';
import { catalogApi, requestApi } from '@/api/endpoints';
import { toApiError } from '@/api/client';
import { PageHeader } from '@/components/Stats';
import { TableSkeleton } from '@/components/Loading';
import { Badge, EmptyState, ErrorState, Modal, Pagination } from '@/components/Ui';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
import { quantityUnits } from '@/lib/constants';
import { fromLocalInputValue, formatDateTime, formatQuantity } from '@/lib/format';
const requestStatusStyles = {
    OPEN: 'bg-sky-100 text-sky-800',
    FULFILLED: 'bg-brand-100 text-brand-800',
    CANCELLED: 'bg-ink-100 text-ink-600',
    EXPIRED: 'bg-amber-100 text-amber-800',
};
const emptyForm = {
    title: '', description: '', categoryId: '', quantityNeeded: '', quantityUnit: 'KG',
    city: '', neededBy: '', beneficiaries: '0',
};
/**
 * NGOs publish what they need so donors can respond with a targeted donation.
 * Requests are informational only — they never auto-create or reserve a donation.
 */
export default function FoodRequestsPage() {
    const toast = useToast();
    const [status, setStatus] = useState('ALL');
    const [page, setPage] = useState(0);
    const [open, setOpen] = useState(false);
    const [form, setForm] = useState(emptyForm);
    const [saving, setSaving] = useState(false);
    const [formError, setFormError] = useState(null);
    const [busyId, setBusyId] = useState(null);
    const categories = useApi(() => catalogApi.categories(), []);
    const { data, loading, error, reload } = useApi(() => requestApi.mine({ status: status === 'ALL' ? undefined : status, page, size: 10 }), [status, page]);
    useEffect(() => {
        if (!open) {
            setForm(emptyForm);
            setFormError(null);
        }
    }, [open]);
    const create = async (event) => {
        event.preventDefault();
        setSaving(true);
        setFormError(null);
        try {
            await requestApi.create({
                title: form.title.trim(),
                description: form.description.trim() || undefined,
                categoryId: form.categoryId ? Number(form.categoryId) : undefined,
                quantityNeeded: Number(form.quantityNeeded),
                quantityUnit: form.quantityUnit,
                city: form.city.trim(),
                neededBy: fromLocalInputValue(form.neededBy),
                beneficiaries: Number(form.beneficiaries || 0),
            });
            toast.success('Food request published.');
            setOpen(false);
            reload();
        }
        catch (caught) {
            const apiError = toApiError(caught);
            setFormError(apiError.fieldErrors ? Object.values(apiError.fieldErrors)[0] : apiError.message);
        }
        finally {
            setSaving(false);
        }
    };
    const setStatusOf = async (request, next) => {
        setBusyId(request.id);
        try {
            await requestApi.updateStatus(request.id, next);
            toast.success(next === 'FULFILLED' ? 'Request marked as fulfilled.' : 'Request cancelled.');
            reload();
        }
        catch (caught) {
            toast.error(toApiError(caught).message);
        }
        finally {
            setBusyId(null);
        }
    };
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: "Food requests", description: "Publish the food your organisation needs so donors can target their surplus.", actions: _jsxs("button", { type: "button", className: "btn-primary", onClick: () => setOpen(true), children: [_jsx(Plus, { className: "h-4 w-4" }), " New request"] }) }), _jsx("div", { className: "mb-4 flex flex-wrap gap-2", children: ['ALL', 'OPEN', 'FULFILLED', 'CANCELLED', 'EXPIRED'].map((option) => (_jsx("button", { type: "button", onClick: () => { setStatus(option); setPage(0); }, className: `rounded-full px-3 py-1.5 text-xs font-semibold transition ${status === option ? 'bg-brand-600 text-white' : 'bg-white text-ink-600 ring-1 ring-ink-200 hover:bg-ink-50'}`, children: option === 'ALL' ? 'All' : option.charAt(0) + option.slice(1).toLowerCase() }, option))) }), loading ? (_jsx(TableSkeleton, { rows: 4, columns: 4 })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : !data || data.content.length === 0 ? (_jsx(EmptyState, { icon: _jsx(ListChecks, { className: "h-5 w-5" }), title: "No food requests yet", description: "Create a request describing the quantity, category and deadline you need \u2014 donors see it on their dashboard.", action: _jsx("button", { type: "button", className: "btn-primary btn-sm", onClick: () => setOpen(true), children: "Create your first request" }) })) : (_jsxs(_Fragment, { children: [_jsx("ul", { className: "space-y-3", children: data.content.map((request) => (_jsx("li", { className: "card card-body", children: _jsxs("div", { className: "flex flex-wrap items-start justify-between gap-3", children: [_jsxs("div", { className: "min-w-0", children: [_jsxs("div", { className: "flex flex-wrap items-center gap-2", children: [_jsx("p", { className: "font-semibold text-ink-900", children: request.title }), _jsx(Badge, { className: requestStatusStyles[request.status], children: request.status })] }), request.description && _jsx("p", { className: "mt-1 line-clamp-2 text-sm text-ink-600", children: request.description }), _jsxs("p", { className: "mt-2 text-xs text-ink-500", children: [formatQuantity(request.quantityNeeded, request.quantityUnit), request.categoryName ? ` · ${request.categoryName}` : '', " \u00B7 ", request.city, " \u00B7 needed by", ' ', formatDateTime(request.neededBy), " \u00B7 ", request.beneficiaries, " beneficiaries"] })] }), request.status === 'OPEN' && (_jsxs("div", { className: "flex gap-2", children: [_jsx("button", { type: "button", className: "btn-secondary btn-sm", disabled: busyId === request.id, onClick: () => setStatusOf(request, 'FULFILLED'), children: "Mark fulfilled" }), _jsx("button", { type: "button", className: "btn-ghost btn-sm text-red-600 hover:bg-red-50", disabled: busyId === request.id, onClick: () => setStatusOf(request, 'CANCELLED'), children: "Cancel" })] }))] }) }, request.id))) }), _jsx(Pagination, { page: data.page, totalPages: data.totalPages, totalElements: data.totalElements, onChange: setPage })] })), _jsx(Modal, { open: open, title: "New food request", description: "Donors see open requests from verified NGOs. This does not reserve food.", onClose: () => setOpen(false), footer: _jsxs(_Fragment, { children: [_jsx("button", { type: "button", className: "btn-secondary", onClick: () => setOpen(false), children: "Cancel" }), _jsx("button", { type: "submit", form: "request-form", className: "btn-primary", disabled: saving, children: saving ? 'Publishing…' : 'Publish request' })] }), children: _jsxs("form", { id: "request-form", onSubmit: create, className: "space-y-3", children: [formError && _jsx("p", { role: "alert", className: "rounded-lg bg-red-50 p-2 text-sm text-red-800", children: formError }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "title", children: ["Title ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "title", className: "input", required: true, value: form.title, onChange: (event) => setForm({ ...form, title: event.target.value }), placeholder: "e.g. Cooked meals for a night shelter" })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "request-description", children: "Description" }), _jsx("textarea", { id: "request-description", rows: 2, className: "input", value: form.description, onChange: (event) => setForm({ ...form, description: event.target.value }) })] }), _jsxs("div", { className: "grid gap-3 sm:grid-cols-2", children: [_jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "categoryId", children: "Category" }), _jsxs("select", { id: "categoryId", className: "input", value: form.categoryId, onChange: (event) => setForm({ ...form, categoryId: event.target.value }), children: [_jsx("option", { value: "", children: "Any category" }), categories.data?.map((category) => (_jsx("option", { value: category.id, children: category.name }, category.id)))] })] }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "request-city", children: ["City ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "request-city", className: "input", required: true, value: form.city, onChange: (event) => setForm({ ...form, city: event.target.value }) })] }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "quantityNeeded", children: ["Quantity needed ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "quantityNeeded", type: "number", min: "0.01", step: "0.01", className: "input", required: true, value: form.quantityNeeded, onChange: (event) => setForm({ ...form, quantityNeeded: event.target.value }) })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "request-unit", children: "Unit" }), _jsx("select", { id: "request-unit", className: "input", value: form.quantityUnit, onChange: (event) => setForm({ ...form, quantityUnit: event.target.value }), children: quantityUnits.map((unit) => _jsx("option", { value: unit, children: unit }, unit)) })] }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "neededBy", children: ["Needed by ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "neededBy", type: "datetime-local", className: "input", required: true, value: form.neededBy, min: new Date(Date.now() + 60_000).toISOString().slice(0, 16), onChange: (event) => setForm({ ...form, neededBy: event.target.value }) })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "beneficiaries", children: "Beneficiaries" }), _jsx("input", { id: "beneficiaries", type: "number", min: "0", step: "1", className: "input", value: form.beneficiaries, onChange: (event) => setForm({ ...form, beneficiaries: event.target.value }) })] })] })] }) })] }));
}
