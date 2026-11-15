import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Pencil, Plus, Tag, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { adminApi, catalogApi } from '@/api/endpoints';
import { toApiError } from '@/api/client';
import { PageHeader } from '@/components/Stats';
import { TableSkeleton } from '@/components/Loading';
import { Badge, EmptyState, ErrorState, Modal } from '@/components/Ui';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
/**
 * Category management. Categories are soft-deactivated rather than deleted so
 * historical donations keep their category label.
 */
export default function AdminCategoriesPage() {
    const toast = useToast();
    const { data, loading, error, reload } = useApi(() => catalogApi.categories(false), []);
    const [editor, setEditor] = useState(null);
    const [deactivateTarget, setDeactivateTarget] = useState(null);
    const activate = async (category) => {
        try {
            await adminApi.updateCategory(category.id, {
                name: category.name,
                description: category.description ?? '',
                active: true,
                displayOrder: category.displayOrder,
            });
            toast.success(`${category.name} reactivated.`);
            reload();
        }
        catch (caught) {
            toast.error(toApiError(caught).message);
        }
    };
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: "Food categories", description: "Categories power donation filters and reporting. Deactivating hides a category from donors without altering history.", actions: _jsxs("button", { type: "button", className: "btn-primary", onClick: () => setEditor({ category: null }), children: [_jsx(Plus, { className: "h-4 w-4" }), " New category"] }) }), loading ? (_jsx(TableSkeleton, { rows: 6, columns: 4 })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : !data || data.length === 0 ? (_jsx(EmptyState, { icon: _jsx(Tag, { className: "h-5 w-5" }), title: "No categories yet", description: "Create the first category so donors can classify their food." })) : (_jsx("div", { className: "card overflow-hidden", children: _jsx("div", { className: "overflow-x-auto", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { scope: "col", children: "Order" }), _jsx("th", { scope: "col", children: "Name" }), _jsx("th", { scope: "col", children: "Slug" }), _jsx("th", { scope: "col", children: "Description" }), _jsx("th", { scope: "col", children: "State" }), _jsx("th", { scope: "col", className: "text-right", children: "Actions" })] }) }), _jsx("tbody", { children: [...data].sort((a, b) => a.displayOrder - b.displayOrder).map((category) => (_jsxs("tr", { children: [_jsx("td", { className: "text-xs text-ink-500", children: category.displayOrder }), _jsx("td", { className: "font-medium text-ink-900", children: category.name }), _jsx("td", { className: "font-mono text-xs text-ink-500", children: category.slug }), _jsx("td", { className: "max-w-xs truncate text-sm text-ink-600", children: category.description ?? '—' }), _jsx("td", { children: _jsx(Badge, { className: category.active ? 'bg-brand-100 text-brand-800' : 'bg-ink-100 text-ink-600', children: category.active ? 'Active' : 'Inactive' }) }), _jsx("td", { children: _jsxs("div", { className: "flex justify-end gap-1.5", children: [_jsxs("button", { type: "button", className: "btn-ghost btn-sm", onClick: () => setEditor({ category }), children: [_jsx(Pencil, { className: "h-3.5 w-3.5" }), " Edit"] }), category.active ? (_jsxs("button", { type: "button", className: "btn-ghost btn-sm text-red-600 hover:bg-red-50", onClick: () => setDeactivateTarget(category), children: [_jsx(Trash2, { className: "h-3.5 w-3.5" }), " Deactivate"] })) : (_jsx("button", { type: "button", className: "btn-secondary btn-sm", onClick: () => activate(category), children: "Activate" }))] }) })] }, category.id))) })] }) }) })), _jsx(CategoryEditor, { editor: editor, onClose: () => setEditor(null), onDone: () => { setEditor(null); reload(); }, toast: toast }), _jsx(DeactivateModal, { category: deactivateTarget, onClose: () => setDeactivateTarget(null), onDone: () => { setDeactivateTarget(null); reload(); }, toast: toast })] }));
}
function CategoryEditor({ editor, onClose, onDone, toast, }) {
    const editing = Boolean(editor?.category);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [displayOrder, setDisplayOrder] = useState('0');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);
    // Seed the form when the modal opens for a specific row.
    const categoryId = editor?.category?.id ?? null;
    useEffect(() => {
        if (!editor)
            return;
        setName(editor.category?.name ?? '');
        setDescription(editor.category?.description ?? '');
        setDisplayOrder(String(editor.category?.displayOrder ?? 0));
        setError(null);
    }, [editor, categoryId]);
    const submit = async (event) => {
        event.preventDefault();
        setBusy(true);
        setError(null);
        const payload = {
            name: name.trim(),
            description: description.trim() || undefined,
            active: editor?.category ? editor.category.active : true,
            displayOrder: Number(displayOrder || 0),
        };
        try {
            if (editor?.category) {
                await adminApi.updateCategory(editor.category.id, payload);
                toast.success('Category updated.');
            }
            else {
                await adminApi.createCategory(payload);
                toast.success('Category created.');
            }
            onDone();
        }
        catch (caught) {
            const apiError = toApiError(caught);
            setError(apiError.fieldErrors ? Object.values(apiError.fieldErrors)[0] : apiError.message);
        }
        finally {
            setBusy(false);
        }
    };
    const close = () => {
        onClose();
    };
    return (_jsx(Modal, { open: Boolean(editor), title: editing ? 'Edit category' : 'New category', onClose: close, footer: _jsxs(_Fragment, { children: [_jsx("button", { type: "button", className: "btn-secondary", onClick: close, children: "Cancel" }), _jsx("button", { type: "submit", form: "category-form", className: "btn-primary", disabled: busy, children: busy ? 'Saving…' : editing ? 'Save changes' : 'Create category' })] }), children: _jsxs("form", { id: "category-form", onSubmit: submit, className: "space-y-3", children: [error && _jsx("p", { role: "alert", className: "rounded-lg bg-red-50 p-2 text-sm text-red-800", children: error }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "category-name", children: ["Name ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "category-name", className: "input", required: true, value: name, onChange: (event) => setName(event.target.value), placeholder: "e.g. Bakery & Confectionery" })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "category-description", children: "Description" }), _jsx("textarea", { id: "category-description", rows: 2, className: "input", value: description, onChange: (event) => setDescription(event.target.value) })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "category-order", children: "Display order" }), _jsx("input", { id: "category-order", type: "number", min: "0", className: "input", value: displayOrder, onChange: (event) => setDisplayOrder(event.target.value) }), _jsx("p", { className: "mt-1 text-xs text-ink-400", children: "Lower numbers appear first in donation filters." })] })] }) }));
}
function DeactivateModal({ category, onClose, onDone, toast, }) {
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);
    const submit = async () => {
        if (!category)
            return;
        setBusy(true);
        setError(null);
        try {
            await adminApi.deactivateCategory(category.id);
            toast.success(`${category.name} deactivated.`);
            onDone();
        }
        catch (caught) {
            setError(toApiError(caught).message);
        }
        finally {
            setBusy(false);
        }
    };
    return (_jsxs(Modal, { open: Boolean(category), title: "Deactivate this category?", description: "Donors can no longer select it for new donations. Existing donations keep their label.", onClose: onClose, footer: _jsxs(_Fragment, { children: [_jsx("button", { type: "button", className: "btn-secondary", onClick: onClose, children: "Cancel" }), _jsx("button", { type: "button", className: "btn-danger", onClick: submit, disabled: busy, children: busy ? 'Deactivating…' : 'Deactivate' })] }), children: [category && _jsx("p", { className: "text-sm text-ink-600", children: category.name }), error && _jsx("p", { className: "field-error", children: error })] }));
}
