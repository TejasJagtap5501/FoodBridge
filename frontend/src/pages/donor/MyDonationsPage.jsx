import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Filter, Plus, Search } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { catalogApi, donationApi } from '@/api/endpoints';
import { Badge, EmptyState, ErrorState, Pagination } from '@/components/Ui';
import { PageHeader } from '@/components/Stats';
import { TableSkeleton } from '@/components/Loading';
import { useApi } from '@/hooks/useApi';
import { donationStatusStyles, donationStatuses } from '@/lib/constants';
import { formatDateTime, formatQuantity, timeUntil } from '@/lib/format';
export default function MyDonationsPage() {
    const [search, setSearch] = useState('');
    const [status, setStatus] = useState('');
    const [categoryId, setCategoryId] = useState('');
    const [page, setPage] = useState(0);
    const [sortBy, setSortBy] = useState('createdAt');
    const categories = useApi(() => catalogApi.categories(), []);
    const { data, loading, error, reload } = useApi(() => donationApi.mine({ search, status, categoryId, page, size: 10, sortBy }), [search, status, categoryId, page, sortBy]);
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: "My donations", description: "Search, filter and manage everything you have published.", actions: _jsxs(Link, { to: "/donor/donations/new", className: "btn-primary", children: [_jsx(Plus, { className: "h-4 w-4" }), " New donation"] }) }), _jsxs("div", { className: "card card-body mb-4", children: [_jsxs("div", { className: "grid gap-3 sm:grid-cols-2 lg:grid-cols-4", children: [_jsxs("div", { className: "relative", children: [_jsx(Search, { className: "pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" }), _jsx("label", { className: "sr-only", htmlFor: "search", children: "Search donations" }), _jsx("input", { id: "search", className: "input pl-9", placeholder: "Search by food name or reference", value: search, onChange: (event) => {
                                            setSearch(event.target.value);
                                            setPage(0);
                                        } })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "status", children: "Status" }), _jsxs("select", { id: "status", className: "input", value: status, onChange: (event) => {
                                            setStatus(event.target.value);
                                            setPage(0);
                                        }, children: [_jsx("option", { value: "", children: "All statuses" }), donationStatuses.map((value) => (_jsx("option", { value: value, children: value }, value)))] })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "category", children: "Category" }), _jsxs("select", { id: "category", className: "input", value: categoryId, onChange: (event) => {
                                            setCategoryId(event.target.value);
                                            setPage(0);
                                        }, children: [_jsx("option", { value: "", children: "All categories" }), categories.data?.map((category) => (_jsx("option", { value: category.id, children: category.name }, category.id)))] })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "sort", children: "Sort by" }), _jsxs("select", { id: "sort", className: "input", value: sortBy, onChange: (event) => setSortBy(event.target.value), children: [_jsx("option", { value: "createdAt", children: "Newest first" }), _jsx("option", { value: "expiresAt", children: "Expiry date" }), _jsx("option", { value: "quantity", children: "Quantity" }), _jsx("option", { value: "foodName", children: "Food name" })] })] })] }), (search || status || categoryId) && (_jsxs("button", { type: "button", className: "btn-ghost btn-sm mt-3", onClick: () => {
                            setSearch('');
                            setStatus('');
                            setCategoryId('');
                            setPage(0);
                        }, children: [_jsx(Filter, { className: "h-3.5 w-3.5" }), " Clear filters"] }))] }), loading ? (_jsx(TableSkeleton, { rows: 6, columns: 6 })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : data && data.content.length === 0 ? (_jsx(EmptyState, { title: search || status || categoryId ? 'No donations match these filters' : 'No donations yet', description: search || status || categoryId
                    ? 'Try clearing the filters or searching for a different food name.'
                    : 'Publish your first surplus food donation to get started.', action: _jsx(Link, { to: "/donor/donations/new", className: "btn-primary btn-sm", children: "Create donation" }) })) : (data && (_jsxs(_Fragment, { children: [_jsx("div", { className: "table-wrap", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Food" }), _jsx("th", { children: "Reference" }), _jsx("th", { children: "Quantity" }), _jsx("th", { children: "Category" }), _jsx("th", { children: "Expires" }), _jsx("th", { children: "Status" }), _jsx("th", { children: _jsx("span", { className: "sr-only", children: "Actions" }) })] }) }), _jsx("tbody", { children: data.content.map((donation) => (_jsxs("tr", { children: [_jsx("td", { className: "font-medium text-ink-900", children: donation.foodName }), _jsx("td", { className: "text-xs text-ink-500", children: donation.referenceCode }), _jsx("td", { children: formatQuantity(donation.quantity, donation.quantityUnit) }), _jsx("td", { children: donation.categoryName }), _jsxs("td", { children: [_jsx("span", { className: "block text-xs text-ink-600", children: formatDateTime(donation.expiresAt) }), _jsx("span", { className: "text-[11px] text-ink-400", children: timeUntil(donation.expiresAt) })] }), _jsx("td", { children: _jsx(Badge, { className: donationStatusStyles[donation.status], children: donation.status }) }), _jsx("td", { className: "text-right", children: _jsx(Link, { to: `/donor/donations/${donation.id}`, className: "text-sm font-semibold text-brand-700 hover:underline", children: "View" }) })] }, donation.id))) })] }) }), _jsx(Pagination, { page: data.page, totalPages: data.totalPages, totalElements: data.totalElements, onChange: setPage })] })))] }));
}
