/** Tailwind classes for status pills, kept in one place for visual consistency. */
export const donationStatusStyles = {
    AVAILABLE: 'bg-brand-100 text-brand-800',
    CLAIMED: 'bg-amber-100 text-amber-800',
    COMPLETED: 'bg-sky-100 text-sky-800',
    EXPIRED: 'bg-ink-200 text-ink-700',
    CANCELLED: 'bg-red-100 text-red-700',
};
export const claimStatusStyles = {
    ACTIVE: 'bg-amber-100 text-amber-800',
    COMPLETED: 'bg-brand-100 text-brand-800',
    CANCELLED: 'bg-red-100 text-red-700',
};
export const pickupStatusStyles = {
    SCHEDULED: 'bg-amber-100 text-amber-800',
    HANDED_OVER: 'bg-sky-100 text-sky-800',
    COMPLETED: 'bg-brand-100 text-brand-800',
    CANCELLED: 'bg-red-100 text-red-700',
};
export const verificationStyles = {
    PENDING: 'bg-amber-100 text-amber-800',
    APPROVED: 'bg-brand-100 text-brand-800',
    REJECTED: 'bg-red-100 text-red-700',
};
export const accountStatusStyles = {
    ACTIVE: 'bg-brand-100 text-brand-800',
    PENDING: 'bg-amber-100 text-amber-800',
    INACTIVE: 'bg-ink-200 text-ink-700',
};
export const foodTypeStyles = {
    VEGETARIAN: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200',
    NON_VEGETARIAN: 'bg-rose-50 text-rose-700 ring-1 ring-rose-200',
    VEGAN: 'bg-lime-50 text-lime-700 ring-1 ring-lime-200',
    MIXED: 'bg-orange-50 text-orange-700 ring-1 ring-orange-200',
};
export const foodTypeLabel = {
    VEGETARIAN: 'Vegetarian',
    NON_VEGETARIAN: 'Non-vegetarian',
    VEGAN: 'Vegan',
    MIXED: 'Mixed',
};
export const quantityUnits = ['KG', 'G', 'SERVINGS', 'MEALS', 'PACKETS', 'BOXES', 'PORTIONS'];
export const businessTypes = ['RESTAURANT', 'HOTEL', 'CAFE', 'BAKERY', 'CATERING', 'OTHER'];
export const donationStatuses = ['AVAILABLE', 'CLAIMED', 'COMPLETED', 'EXPIRED', 'CANCELLED'];
export const SAFETY_DISCLAIMER = 'FoodBridge is a coordination platform, not a food safety certifier. Donors are responsible for the accuracy ' +
    'of the information they publish, and NGOs must independently assess whether food is suitable for their ' +
    'beneficiaries before accepting it.';
