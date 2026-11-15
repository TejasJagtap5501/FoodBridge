import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Download, Utensils } from 'lucide-react';
import { useState } from 'react';
import { adminApi } from '@/api/endpoints';
import { PageHeader, StatCard } from '@/components/Stats';
import { PageLoader } from '@/components/Loading';
import { BarChart, EmptyState, ErrorState } from '@/components/Ui';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
import { formatNumber, formatQuantity } from '@/lib/format';
/**
 * Aggregated reporting. The charts and both CSV exports are generated from the
 * same live API payload, so nothing on this page is hard-coded.
 */
export default function AdminReportsPage() {
    const [days, setDays] = useState(30);
    const toast = useToast();
    const { data, loading, error, reload } = useApi(() => adminApi.reports(days), [days]);
    const downloadCsv = (filename, rows) => {
        const escape = (value) => {
            const text = String(value);
            return /[",\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
        };
        const csv = rows.map((row) => row.map(escape).join(',')).join('\n');
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(url);
        toast.success(`${filename} downloaded.`);
    };
    const exportDaily = () => {
        if (!data)
            return;
        downloadCsv(`foodbridge-daily-${days}d.csv`, [
            ['date', 'donations', 'meals_estimate'],
            ...data.donationsPerDay.map((point) => [
                point.date,
                point.count,
                data.mealsPerDay.find((meal) => meal.date === point.date)?.count ?? 0,
            ]),
        ]);
    };
    const exportCategories = () => {
        if (!data)
            return;
        downloadCsv('foodbridge-categories.csv', [
            ['category', 'donations', 'meals', 'share_of_donations'],
            ...data.categoryBreakdown.map((row) => [
                row.category,
                row.donations,
                row.meals,
                data.categoryBreakdown.length
                    ? `${((row.donations / Math.max(1, data.categoryBreakdown.reduce((sum, item) => sum + item.donations, 0))) * 100).toFixed(1)}%`
                    : '0%',
            ]),
        ]);
    };
    return (_jsxs("div", { children: [_jsx(PageHeader, { title: "Reports", description: "Impact reporting derived from completed donations. Meal counts are estimates, not verified nutritional data.", actions: _jsxs(_Fragment, { children: [_jsx("label", { className: "sr-only", htmlFor: "days", children: "Reporting window" }), _jsxs("select", { id: "days", className: "input w-auto", value: days, onChange: (event) => setDays(Number(event.target.value)), children: [_jsx("option", { value: 7, children: "Last 7 days" }), _jsx("option", { value: 14, children: "Last 14 days" }), _jsx("option", { value: 30, children: "Last 30 days" }), _jsx("option", { value: 90, children: "Last 90 days" })] }), _jsxs("button", { type: "button", className: "btn-secondary", onClick: exportDaily, disabled: !data, children: [_jsx(Download, { className: "h-4 w-4" }), " Daily CSV"] }), _jsxs("button", { type: "button", className: "btn-secondary", onClick: exportCategories, disabled: !data, children: [_jsx(Download, { className: "h-4 w-4" }), " Category CSV"] })] }) }), loading ? (_jsx(PageLoader, { label: "Crunching the numbers\u2026" })) : error ? (_jsx(ErrorState, { message: error, onRetry: reload })) : !data ? null : (_jsxs(_Fragment, { children: [_jsxs("div", { className: "grid gap-4 sm:grid-cols-3", children: [_jsx(StatCard, { label: "Estimated meals served", value: data.totalMealsServed, icon: Utensils, tone: "success" }), _jsx(StatCard, { label: "Quantity rescued", value: formatNumber(Math.round(data.totalQuantityRescued)), icon: Utensils, hint: "Sum of donated quantities (mixed units)" }), _jsx(StatCard, { label: "Leading category", value: data.categoryBreakdown[0]?.category ?? '—', icon: Utensils, hint: data.categoryBreakdown[0] ? `${data.categoryBreakdown[0].donations} donations` : undefined })] }), _jsxs("section", { className: "card card-body mt-6", children: [_jsx("h2", { className: "mb-3 font-semibold text-ink-900", children: "Donations published per day" }), data.donationsPerDay.length === 0 ? (_jsx(EmptyState, { title: "No donations in this window", description: "Choose a longer reporting period." })) : (_jsxs(_Fragment, { children: [_jsx(BarChart, { data: data.donationsPerDay.map((point) => ({ label: point.date.slice(5), value: point.count })), label: "Donations published per day", valueLabel: "donations", height: 200 }), _jsxs("div", { className: "mt-1 flex justify-between text-[10px] text-ink-400", children: [_jsx("span", { children: data.donationsPerDay[0]?.date }), _jsx("span", { children: data.donationsPerDay[data.donationsPerDay.length - 1]?.date })] })] }))] }), _jsxs("div", { className: "mt-6 grid gap-6 lg:grid-cols-2", children: [_jsxs("section", { className: "card card-body", children: [_jsx("h2", { className: "mb-3 font-semibold text-ink-900", children: "Meals by category" }), data.categoryBreakdown.length === 0 ? (_jsx("p", { className: "text-sm text-ink-500", children: "No completed donations in this window." })) : (_jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { scope: "col", children: "Category" }), _jsx("th", { scope: "col", className: "text-right", children: "Donations" }), _jsx("th", { scope: "col", className: "text-right", children: "Est. meals" })] }) }), _jsx("tbody", { children: data.categoryBreakdown.map((row) => (_jsxs("tr", { children: [_jsx("td", { className: "text-ink-800", children: row.category }), _jsx("td", { className: "text-right text-ink-700", children: formatNumber(row.donations) }), _jsx("td", { className: "text-right font-medium text-ink-900", children: formatNumber(row.meals) })] }, row.category))) })] }))] }), _jsxs("section", { className: "card card-body", children: [_jsx("h2", { className: "mb-3 font-semibold text-ink-900", children: "Top cities" }), data.topCities.length === 0 ? (_jsx("p", { className: "text-sm text-ink-500", children: "No completed donations in this window." })) : (_jsx("ul", { className: "space-y-2", children: data.topCities.map((city) => {
                                            const max = Math.max(...data.topCities.map((item) => item.donations), 1);
                                            return (_jsxs("li", { children: [_jsxs("div", { className: "flex justify-between text-sm", children: [_jsx("span", { className: "text-ink-800", children: city.city }), _jsxs("span", { className: "text-ink-600", children: [city.donations, " donations \u00B7 ", formatNumber(city.meals), " meals"] })] }), _jsx("div", { className: "mt-1 h-2 rounded-full bg-ink-100", children: _jsx("div", { className: "h-2 rounded-full bg-brand-500", style: { width: `${(city.donations / max) * 100}%` } }) })] }, city.city));
                                        }) }))] })] }), _jsxs("section", { className: "card card-body mt-6", children: [_jsx("h2", { className: "mb-3 font-semibold text-ink-900", children: "Top donors" }), data.topDonors.length === 0 ? (_jsx("p", { className: "text-sm text-ink-500", children: "No completed donations in this window." })) : (_jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { scope: "col", children: "Donor" }), _jsx("th", { scope: "col", className: "text-right", children: "Completed donations" }), _jsx("th", { scope: "col", className: "text-right", children: "Est. meals" })] }) }), _jsx("tbody", { children: data.topDonors.map((donor) => (_jsxs("tr", { children: [_jsx("td", { className: "text-ink-800", children: donor.donorName }), _jsx("td", { className: "text-right text-ink-700", children: formatNumber(donor.completedDonations) }), _jsx("td", { className: "text-right font-medium text-ink-900", children: formatNumber(donor.meals) })] }, donor.donorId))) })] }))] }), _jsxs("p", { className: "mt-4 text-xs text-ink-400", children: ["Quantities are recorded in mixed units (", formatQuantity(1, 'KG'), ", servings, packets\u2026), so the \"quantity rescued\" total is a raw sum and is shown for transparency only. Meal estimates use the documented conversion factors."] })] }))] }));
}
