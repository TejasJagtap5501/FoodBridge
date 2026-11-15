import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { BarChart3, Bell, ClipboardList, FileSearch, Handshake, Heart, LayoutDashboard, ListChecks, LogOut, Menu, PackageCheck, ScrollText, Search, ShieldCheck, Tag, UserCircle, Users, X, } from 'lucide-react';
import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Logo } from '@/components/Logo';
import { NotificationBell } from '@/components/NotificationBell';
import { useAuth } from '@/auth/AuthContext';
import { useToast } from '@/components/Toast';
const donorNav = [
    { to: '/donor', label: 'Dashboard', icon: LayoutDashboard, end: true },
    { to: '/donor/donations/new', label: 'Create donation', icon: Heart },
    { to: '/donor/donations', label: 'My donations', icon: ClipboardList },
    { to: '/donor/claims', label: 'Claims', icon: Handshake },
    { to: '/donor/pickups', label: 'Pickups', icon: PackageCheck },
    { to: '/donor/profile', label: 'Profile', icon: UserCircle },
];
const ngoNav = [
    { to: '/ngo', label: 'Dashboard', icon: LayoutDashboard, end: true },
    { to: '/ngo/browse', label: 'Browse donations', icon: Search },
    { to: '/ngo/claims', label: 'My claims', icon: Handshake },
    { to: '/ngo/pickups', label: 'Pickups', icon: PackageCheck },
    { to: '/ngo/requests', label: 'Food requests', icon: ListChecks },
    { to: '/ngo/profile', label: 'Profile', icon: UserCircle },
];
const adminNav = [
    { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
    { to: '/admin/users', label: 'Users', icon: Users },
    { to: '/admin/ngo-verification', label: 'NGO verification', icon: ShieldCheck },
    { to: '/admin/donations', label: 'Donations', icon: FileSearch },
    { to: '/admin/claims', label: 'Claims', icon: Handshake },
    { to: '/admin/categories', label: 'Categories', icon: Tag },
    { to: '/admin/reports', label: 'Reports', icon: BarChart3 },
    { to: '/admin/audit-logs', label: 'Audit logs', icon: ScrollText },
];
const navByRole = { DONOR: donorNav, NGO: ngoNav, ADMIN: adminNav };
const roleLabel = {
    DONOR: 'Food donor',
    NGO: 'Verified NGO',
    ADMIN: 'Administrator',
};
export function DashboardLayout() {
    const { user, logout } = useAuth();
    const toast = useToast();
    const navigate = useNavigate();
    const [mobileOpen, setMobileOpen] = useState(false);
    if (!user)
        return null;
    const items = navByRole[user.role];
    const handleLogout = async () => {
        await logout();
        toast.success('You have been signed out.');
        navigate('/login', { replace: true });
    };
    const navLink = (item) => (_jsxs(NavLink, { to: item.to, end: item.end, onClick: () => setMobileOpen(false), className: ({ isActive }) => `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${isActive
            ? 'bg-brand-50 text-brand-800 ring-1 ring-inset ring-brand-200'
            : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900'}`, children: [_jsx(item.icon, { className: "h-4 w-4 shrink-0" }), item.label] }, item.to));
    const sidebar = (_jsxs("div", { className: "flex h-full flex-col", children: [_jsx("div", { className: "flex h-16 items-center border-b border-ink-100 px-4", children: _jsx(Logo, { to: user.role === 'ADMIN' ? '/admin' : user.role === 'NGO' ? '/ngo' : '/donor' }) }), _jsxs("nav", { className: "flex-1 space-y-1 overflow-y-auto p-3", "aria-label": "Dashboard", children: [items.map(navLink), _jsx("div", { className: "my-3 border-t border-ink-100" }), _jsxs("button", { type: "button", onClick: handleLogout, className: "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-ink-600 transition-colors hover:bg-red-50 hover:text-red-700", children: [_jsx(LogOut, { className: "h-4 w-4" }), " Sign out"] })] }), _jsxs("div", { className: "border-t border-ink-100 p-4", children: [_jsx("p", { className: "truncate text-sm font-semibold text-ink-800", children: user.fullName }), _jsx("p", { className: "truncate text-xs text-ink-500", children: roleLabel[user.role] })] })] }));
    return (_jsxs("div", { className: "min-h-screen bg-ink-50", children: [_jsx("aside", { className: "fixed inset-y-0 left-0 hidden w-64 border-r border-ink-200 bg-white lg:block", children: sidebar }), mobileOpen && (_jsxs("div", { className: "fixed inset-0 z-50 lg:hidden", children: [_jsx("div", { className: "absolute inset-0 bg-ink-900/40", onClick: () => setMobileOpen(false), "aria-hidden": "true" }), _jsxs("aside", { className: "absolute inset-y-0 left-0 w-72 bg-white shadow-lift", children: [_jsx("button", { type: "button", className: "absolute right-3 top-4 rounded-lg p-2 text-ink-500 hover:bg-ink-100", onClick: () => setMobileOpen(false), "aria-label": "Close navigation", children: _jsx(X, { className: "h-5 w-5" }) }), sidebar] })] })), _jsxs("div", { className: "lg:pl-64", children: [_jsxs("header", { className: "sticky top-0 z-30 flex h-16 items-center justify-between gap-4 border-b border-ink-200 bg-white/90 px-4 backdrop-blur sm:px-6", children: [_jsxs("div", { className: "flex items-center gap-3", children: [_jsx("button", { type: "button", className: "rounded-lg p-2 text-ink-600 hover:bg-ink-100 lg:hidden", onClick: () => setMobileOpen(true), "aria-label": "Open navigation", children: _jsx(Menu, { className: "h-5 w-5" }) }), _jsx("div", { className: "lg:hidden", children: _jsx(Logo, { compact: true }) })] }), _jsxs("div", { className: "flex items-center gap-1 sm:gap-2", children: [_jsx(NotificationBell, {}), _jsx(NavLink, { to: `/${user.role.toLowerCase()}/notifications`, className: "hidden rounded-lg p-2 text-ink-600 hover:bg-ink-100 sm:block", "aria-label": "Notification history", children: _jsx(Bell, { className: "h-5 w-5" }) }), _jsxs(NavLink, { to: `/${user.role.toLowerCase()}/profile`, className: "ml-1 flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-ink-100", children: [_jsx("span", { className: "flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-sm font-bold text-brand-800", children: user.fullName.slice(0, 1).toUpperCase() }), _jsxs("span", { className: "hidden text-left sm:block", children: [_jsx("span", { className: "block max-w-[12rem] truncate text-sm font-medium text-ink-800", children: user.fullName }), _jsx("span", { className: "block text-[11px] text-ink-500", children: roleLabel[user.role] })] })] })] })] }), _jsx("main", { className: "mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:py-8", children: _jsx(Outlet, {}) })] })] }));
}
