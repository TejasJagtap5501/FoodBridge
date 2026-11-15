import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { MapPin, Search, SlidersHorizontal, Utensils } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { catalogApi, donationApi } from '@/api/endpoints';
import { Badge, EmptyState, ErrorState, Pagination } from '@/components/Ui';
import { PageHeader } from '@/components/Stats';
import { CardGridSkeleton } from '@/components/Loading';
import { useApi } from '@/hooks/useApi';
import { foodTypeLabel, foodTypeStyles } from '@/lib/constants';
import { formatQuantity, timeUntil } from '@/lib/format';
/** NGO browse view: server-side search, filtering, sorting and pagination. */
export default function BrowseDonationsPage() {
    const [search, setSearch] = useState('');
    const [categoryId, setCategoryId] = useState('');
    const [city, setCity] = useState('');
    const [minQuantity, setMinQuantity] = useState('');
    const [expiringWithin, setExpiringWithin] = useState('');
    const [sortBy, setSortBy] = useState('expiresAt');
    const [page, setPage] = useState(0);
    const categories = useApi(() => catalogApi.categories(), []);
    const expiringBefore = expiringWithin
        ? new Date(Date.now() + Number(expiringWithin) * 3_600_000).toISOString()
        : undefined;
    const { data, loading, error, reload } = useApi(() => donationApi.available({
        search, categoryId, city, minQuantity, expiringBefore, page, size: 9,
        sortBy, direction: sortBy === 'foodName' ? 'asc' : 'asc',
    }), [search, categoryId, city, minQuantity, expiringWithin, page, sortBy]);
    const reset = () => {
        setSearch('');
        setCategoryId('');
        setCity('');
        setMinQuantity('');
        setExpiringWithin('');
        setPage(0);
    };
    const hasFilters = Boolean(search || categoryId || city || minQuantity || expiringWithin);
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: "Available donations", description: "Only AVAILABLE, unexpired donations are listed. Sorted by nearest expiry by default." }), _jsx("div", { className: "card card-body mb-5", children: _jsxs("div", { className: "grid gap-3 md:grid-cols-2 lg:grid-cols-4", children: [_jsxs("div", { className: "relative lg:col-span-2", children: [_jsx(Search, { className: "pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" }), _jsx("label", { className: "sr-only", htmlFor: "search", children: "Search food" }), _jsx("input", { id: "search", className: "input pl-9", placeholder: "Search by food name", value: search, onChange: (event) => { setSearch(event.target.value); setPage(0); } })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "category", children: "Category" }), _jsxs("select", { id: "category", className: "input", value: categoryId, onChange: (event) => { setCategoryId(event.target.value); setPage(0); }, children: [_jsx("option", { value: "", children: "All categories" }), categories.data?.map((category) => (_jsx("option", { value: category.id, children: category.name }, category.id)))] })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "city", children: "City" }), _jsx("input", { id: "city", className: "input", placeholder: "City", value: city, onChange: (event) => { setCity(event.target.value); setPage(0); } })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "minQuantity", children: "Minimum quantity" }), _jsx("input", { id: "minQuantity", type: "number", min: "0", step: "0.5", className: "input", placeholder: "Min quantity", value: minQuantity, onChange: (event) => { setMinQuantity(event.target.value); setPage(0); } })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "expiry", children: "Expiring within" }), _jsxs("select", { id: "expiry", className: "input", value: expiringWithin, onChange: (event) => { setExpiringWithin(event.target.value); setPage(0); }, children: [_jsx("option", { value: "", children: "Any expiry" }), _jsx("option", { value: "2", children: "Within 2 hours" }), _jsx("option", { value: "6", children: "Within 6 hours" }), _jsx("option", { value: "12", children: "Within 12 hours" }), _jsx("option", { value: "24", children: "Within 24 hours" })] })] }), _jsxs("div", { children: [_jsx("label", { className: "sr-only", htmlFor: "sort", children: "Sort" }), _jsxs("select", { id: "sort", className: "input", value: sortBy, onChange: (event) => { setSortBy(event.target.value); setPage(0); }, children: [_jsx("option", { value: "expiresAt", children: "Nearest expiry" }), _jsx("option", { value: "quantity", children: "Largest quantity" }), _jsx("option", { value: "foodName", children: "Food name" }), _jsx("option", { value: "createdAt", children: "Newest" })] })] }), _jsx("div", { className: "flex items-end", children: _jsxs("button", { type: "button", className: "btn-secondary w-full", onClick: reset, disabled: !hasFilters, children: [_jsx(SlidersHorizontal, { className: "h-4 w-4" }), " Clear filters"] }) })] }) }), loading ? (_jsx(CardGridSkeleton, { cards: 6 })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : data && data.content.length === 0 ? (_jsx(EmptyState, { icon: _jsx(Utensils, { className: "h-5 w-5" }), title: hasFilters ? 'No donations match these filters' : 'No donations available right now', description: hasFilters
                    ? 'Try widening the expiry window, removing the city filter or lowering the minimum quantity.'
                    : 'New surplus food is published throughout the day — check back shortly.', action: hasFilters ? _jsx("button", { type: "button", className: "btn-secondary btn-sm", onClick: reset, children: "Clear filters" }) : undefined })) : (data && (_jsxs(_Fragment, { children: [_jsx("div", { className: "grid gap-4 sm:grid-cols-2 xl:grid-cols-3", children: data.content.map((donation) => (_jsxs("article", { className: "card card-body flex flex-col", children: [_jsxs("div", { className: "flex items-start justify-between gap-2", children: [_jsx("h3", { className: "font-semibold leading-snug text-ink-900", children: donation.foodName }), _jsx(Badge, { className: foodTypeStyles[donation.foodType], children: foodTypeLabel[donation.foodType] })] }), _jsx("p", { className: "mt-1 text-xs text-ink-500", children: donation.categoryName }), _jsxs("dl", { className: "mt-3 space-y-1.5 text-sm", children: [_jsxs("div", { className: "flex justify-between", children: [_jsx("dt", { className: "text-ink-500", children: "Quantity" }), _jsx("dd", { className: "font-medium text-ink-800", children: formatQuantity(donation.quantity, donation.quantityUnit) })] }), _jsxs("div", { className: "flex justify-between", children: [_jsx("dt", { className: "text-ink-500", children: "Approx. meals" }), _jsx("dd", { className: "font-medium text-ink-800", children: donation.estimatedMeals })] }), _jsxs("div", { className: "flex justify-between", children: [_jsxs("dt", { className: "flex items-center gap-1 text-ink-500", children: [_jsx(MapPin, { className: "h-3.5 w-3.5" }), " City"] }), _jsx("dd", { className: "font-medium text-ink-800", children: donation.city })] })] }), _jsx("p", { className: "mt-3 rounded-lg bg-brand-50 px-2.5 py-1.5 text-xs font-semibold text-brand-800", children: timeUntil(donation.expiresAt) }), _jsx(Link, { to: `/ngo/donations/${donation.id}`, className: "btn-primary btn-sm mt-4", children: "View details" })] }, donation.id))) }), _jsx(Pagination, { page: data.page, totalPages: data.totalPages, totalElements: data.totalElements, onChange: setPage })] })))] }));
}
