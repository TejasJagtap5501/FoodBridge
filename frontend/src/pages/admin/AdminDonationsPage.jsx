import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Search } from 'lucide-react';
import { useState } from 'react';
import { adminApi, catalogApi } from '@/api/endpoints';
import { PageHeader } from '@/components/Stats';
import { TableSkeleton } from '@/components/Loading';
import { Badge, EmptyState, ErrorState, Pagination } from '@/components/Ui';
import { useApi } from '@/hooks/useApi';
import { donationStatusStyles, donationStatuses } from '@/lib/constants';
import { formatDateTime, formatQuantity } from '@/lib/format';
/** Read-only moderation view of every donation on the platform. */
export default function AdminDonationsPage() {
    const [search, setSearch] = useState('');
    const [status, setStatus] = useState('');
    const [city, setCity] = useState('');
    const [categoryId, setCategoryId] = useState('');
    const [page, setPage] = useState(0);
    const categories = useApi(() => catalogApi.categories(false), []);
    const { data, loading, error, reload } = useApi(() => adminApi.donations({
        search, city,
        status: status || undefined,
        categoryId: categoryId || undefined,
        page, size: 20, sortBy: 'createdAt', direction: 'desc',
    }), [search, city, status, categoryId, page]);
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: "Donations", description: "Every donation with its current lifecycle state. Expiry is applied by a scheduled job, not by the browser." }), _jsxs("div", { className: "card card-body mb-4 grid gap-3 md:grid-cols-2 lg:grid-cols-4", children: [_jsxs("div", { className: "relative", children: [_jsx(Search, { className: "pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" }), _jsx("label", { className: "sr-only", htmlFor: "donation-search", children: "Search donations" }), _jsx("input", { id: "donation-search", className: "input pl-9", placeholder: "Food name", value: search, onChange: (event) => { setSearch(event.target.value); setPage(0); } })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "donation-status", children: "Status" }), _jsxs("select", { id: "donation-status", className: "input", value: status, onChange: (event) => { setStatus(event.target.value); setPage(0); }, children: [_jsx("option", { value: "", children: "All statuses" }), donationStatuses.map((option) => _jsx("option", { value: option, children: option }, option))] })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "donation-city", children: "City" }), _jsx("input", { id: "donation-city", className: "input", placeholder: "City", value: city, onChange: (event) => { setCity(event.target.value); setPage(0); } })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "donation-category", children: "Category" }), _jsxs("select", { id: "donation-category", className: "input", value: categoryId, onChange: (event) => { setCategoryId(event.target.value); setPage(0); }, children: [_jsx("option", { value: "", children: "All categories" }), categories.data?.map((category) => (_jsx("option", { value: category.id, children: category.name }, category.id)))] })] })] }), loading ? (_jsx(TableSkeleton, { rows: 8, columns: 6 })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : !data || data.content.length === 0 ? (_jsx(EmptyState, { title: "No donations match these filters", description: "Clear the search box or choose a different status." })) : (_jsxs("div", { className: "card overflow-hidden", children: [_jsx("div", { className: "overflow-x-auto", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { scope: "col", children: "Reference" }), _jsx("th", { scope: "col", children: "Food" }), _jsx("th", { scope: "col", children: "Category" }), _jsx("th", { scope: "col", children: "Quantity" }), _jsx("th", { scope: "col", children: "City" }), _jsx("th", { scope: "col", children: "Expires" }), _jsx("th", { scope: "col", children: "Status" })] }) }), _jsx("tbody", { children: data.content.map((donation) => (_jsxs("tr", { children: [_jsx("td", { className: "whitespace-nowrap font-mono text-xs text-ink-600", children: donation.referenceCode }), _jsx("td", { className: "font-medium text-ink-900", children: donation.foodName }), _jsx("td", { className: "text-xs text-ink-600", children: donation.categoryName }), _jsx("td", { className: "whitespace-nowrap text-ink-700", children: formatQuantity(donation.quantity, donation.quantityUnit) }), _jsx("td", { className: "text-ink-700", children: donation.city }), _jsx("td", { className: "whitespace-nowrap text-xs text-ink-600", children: formatDateTime(donation.expiresAt) }), _jsx("td", { children: _jsx(Badge, { className: donationStatusStyles[donation.status], children: donation.status }) })] }, donation.id))) })] }) }), _jsx(Pagination, { page: data.page, totalPages: data.totalPages, totalElements: data.totalElements, onChange: setPage })] }))] }));
}
