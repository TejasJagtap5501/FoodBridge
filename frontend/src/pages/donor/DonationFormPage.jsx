import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertCircle, ArrowLeft, Info } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { z } from 'zod';
import { catalogApi, donationApi } from '@/api/endpoints';
import { toApiError } from '@/api/client';
import { PageHeader } from '@/components/Stats';
import { PageLoader } from '@/components/Loading';
import { Callout } from '@/components/Ui';
import { useToast } from '@/components/Toast';
import { useApi } from '@/hooks/useApi';
import { foodTypeLabel, quantityUnits, SAFETY_DISCLAIMER } from '@/lib/constants';
import { fromLocalInputValue, toLocalInputValue } from '@/lib/format';
/**
 * Validation mirrors the backend rules (DonationCreateRequest) so users get
 * immediate feedback. The server re-validates everything — this is UX only.
 */
const schema = z
    .object({
    foodName: z.string().min(2, 'Food name is required').max(160),
    description: z.string().max(1000).optional().or(z.literal('')),
    quantity: z.coerce.number().positive('Quantity must be greater than zero'),
    quantityUnit: z.string().min(1, 'Select a unit'),
    foodType: z.enum(['VEGETARIAN', 'NON_VEGETARIAN', 'VEGAN', 'MIXED']),
    categoryId: z.coerce.number().int().positive('Select a food category'),
    preparedAt: z.string().min(1, 'Preparation time is required'),
    expiresAt: z.string().min(1, 'Expiry time is required'),
    pickupAddress: z.string().min(5, 'Pickup address is required').max(300),
    city: z.string().min(2, 'City is required').max(80),
    pickupInstructions: z.string().max(500).optional().or(z.literal('')),
    storageConditions: z.string().max(255).optional().or(z.literal('')),
    allergenInfo: z.string().max(500).optional().or(z.literal('')),
    handlingInstructions: z.string().max(500).optional().or(z.literal('')),
    safetyDeclaration: z.boolean(),
})
    .refine((values) => new Date(values.expiresAt) > new Date(values.preparedAt), {
    path: ['expiresAt'],
    message: 'Expiry must be after the preparation time',
})
    .refine((values) => new Date(values.expiresAt).getTime() > Date.now(), {
    path: ['expiresAt'],
    message: 'Expiry must be in the future',
})
    .refine((values) => new Date(values.preparedAt).getTime() <= Date.now() + 60_000, {
    path: ['preparedAt'],
    message: 'Preparation time cannot be in the future',
})
    .refine((values) => values.safetyDeclaration, {
    path: ['safetyDeclaration'],
    message: 'You must confirm the food safety declaration',
});
export default function DonationFormPage() {
    const { id } = useParams();
    const editing = Boolean(id);
    const navigate = useNavigate();
    const toast = useToast();
    const [serverError, setServerError] = useState(null);
    const categories = useApi(() => catalogApi.categories(), []);
    const existing = useApi(() => (editing ? donationApi.get(Number(id)) : Promise.resolve(null)), [editing, id]);
    const { register, handleSubmit, reset, formState: { errors, isSubmitting }, } = useForm({
        resolver: zodResolver(schema),
        defaultValues: {
            foodType: 'VEGETARIAN',
            quantityUnit: 'KG',
            safetyDeclaration: false,
            preparedAt: toLocalInputValue(new Date(Date.now() - 30 * 60_000).toISOString()),
            expiresAt: toLocalInputValue(new Date(Date.now() + 6 * 3_600_000).toISOString()),
        },
    });
    // Populate the form once the existing donation has loaded (edit mode).
    useEffect(() => {
        if (existing.data) {
            const donation = existing.data;
            reset({
                foodName: donation.foodName,
                description: donation.description ?? '',
                quantity: donation.quantity,
                quantityUnit: donation.quantityUnit,
                foodType: donation.foodType,
                categoryId: donation.category.id,
                preparedAt: toLocalInputValue(donation.preparedAt),
                expiresAt: toLocalInputValue(donation.expiresAt),
                pickupAddress: donation.pickupAddress,
                city: donation.city,
                pickupInstructions: donation.pickupInstructions ?? '',
                storageConditions: donation.storageConditions ?? '',
                allergenInfo: donation.allergenInfo ?? '',
                handlingInstructions: donation.handlingInstructions ?? '',
                safetyDeclaration: donation.safetyDeclaration,
            });
        }
    }, [existing.data, reset]);
    const onSubmit = async (values) => {
        setServerError(null);
        const payload = {
            ...values,
            preparedAt: fromLocalInputValue(values.preparedAt),
            expiresAt: fromLocalInputValue(values.expiresAt),
            description: values.description || undefined,
            pickupInstructions: values.pickupInstructions || undefined,
            storageConditions: values.storageConditions || undefined,
            allergenInfo: values.allergenInfo || undefined,
            handlingInstructions: values.handlingInstructions || undefined,
        };
        try {
            if (editing) {
                const { safetyDeclaration: _ignored, ...updatePayload } = payload;
                await donationApi.update(Number(id), updatePayload);
                toast.success('Donation updated.');
                navigate(`/donor/donations/${id}`);
            }
            else {
                const created = await donationApi.create(payload);
                toast.success(`Donation ${created.referenceCode} published.`);
                navigate(`/donor/donations/${created.id}`);
            }
        }
        catch (error) {
            const apiError = toApiError(error);
            const firstField = apiError.fieldErrors ? Object.values(apiError.fieldErrors)[0] : undefined;
            setServerError(firstField ?? apiError.message);
        }
    };
    if (editing && existing.loading)
        return _jsx(PageLoader, { label: "Loading donation\u2026" });
    const error = (name) => errors[name]?.message ? _jsx("p", { className: "field-error", children: errors[name]?.message }) : null;
    return (_jsxs("div", { className: "mx-auto max-w-3xl", children: [_jsxs(Link, { to: "/donor/donations", className: "mb-4 inline-flex items-center gap-1 text-sm font-medium text-ink-500 hover:text-ink-800", children: [_jsx(ArrowLeft, { className: "h-4 w-4" }), " Back to donations"] }), _jsx(PageHeader, { title: editing ? 'Edit donation' : 'Publish a food donation', description: editing
                    ? 'Only donations that are still AVAILABLE can be edited.'
                    : 'Accurate information helps NGOs decide quickly whether the food is suitable.' }), serverError && (_jsxs("div", { role: "alert", className: "mb-4 flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800", children: [_jsx(AlertCircle, { className: "mt-0.5 h-4 w-4 shrink-0" }), _jsx("span", { children: serverError })] })), _jsxs("form", { onSubmit: handleSubmit(onSubmit), className: "space-y-6", noValidate: true, children: [_jsxs("section", { className: "card card-body space-y-4", children: [_jsx("h2", { className: "font-semibold text-ink-900", children: "What are you donating?" }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "foodName", children: ["Food name ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "foodName", className: `input ${errors.foodName ? 'input-error' : ''}`, placeholder: "e.g. Vegetable Biryani", ...register('foodName') }), error('foodName')] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "description", children: "Description" }), _jsx("textarea", { id: "description", rows: 3, className: "input", placeholder: "Preparation style, ingredients, packaging\u2026", ...register('description') }), error('description')] }), _jsxs("div", { className: "grid gap-4 sm:grid-cols-3", children: [_jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "quantity", children: ["Quantity ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "quantity", type: "number", step: "0.01", min: "0.01", className: `input ${errors.quantity ? 'input-error' : ''}`, ...register('quantity') }), error('quantity')] }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "quantityUnit", children: ["Unit ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("select", { id: "quantityUnit", className: "input", ...register('quantityUnit'), children: quantityUnits.map((unit) => _jsx("option", { value: unit, children: unit }, unit)) }), error('quantityUnit')] }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "foodType", children: ["Food type ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("select", { id: "foodType", className: "input", ...register('foodType'), children: Object.keys(foodTypeLabel).map((type) => (_jsx("option", { value: type, children: foodTypeLabel[type] }, type))) }), error('foodType')] })] }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "categoryId", children: ["Food category ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsxs("select", { id: "categoryId", className: `input ${errors.categoryId ? 'input-error' : ''}`, ...register('categoryId'), children: [_jsx("option", { value: "", children: "Select a category" }), categories.data?.map((category) => (_jsx("option", { value: category.id, children: category.name }, category.id)))] }), error('categoryId')] })] }), _jsxs("section", { className: "card card-body space-y-4", children: [_jsx("h2", { className: "font-semibold text-ink-900", children: "Timing" }), _jsxs("div", { className: "grid gap-4 sm:grid-cols-2", children: [_jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "preparedAt", children: ["Prepared at ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "preparedAt", type: "datetime-local", className: `input ${errors.preparedAt ? 'input-error' : ''}`, ...register('preparedAt') }), error('preparedAt')] }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "expiresAt", children: ["Expires at ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "expiresAt", type: "datetime-local", className: `input ${errors.expiresAt ? 'input-error' : ''}`, ...register('expiresAt') }), error('expiresAt')] })] }), _jsxs("p", { className: "flex items-start gap-1.5 text-xs text-ink-500", children: [_jsx(Info, { className: "mt-0.5 h-3.5 w-3.5 shrink-0" }), "Donations may not stay listed for more than 48 hours after preparation. The estimate of meals is derived from the quantity you enter."] })] }), _jsxs("section", { className: "card card-body space-y-4", children: [_jsx("h2", { className: "font-semibold text-ink-900", children: "Pickup details" }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "pickupAddress", children: ["Pickup address ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "pickupAddress", className: `input ${errors.pickupAddress ? 'input-error' : ''}`, placeholder: "Street address of the pickup point", ...register('pickupAddress') }), error('pickupAddress')] }), _jsxs("div", { children: [_jsxs("label", { className: "label", htmlFor: "city", children: ["City ", _jsx("span", { className: "text-red-500", children: "*" })] }), _jsx("input", { id: "city", className: `input ${errors.city ? 'input-error' : ''}`, placeholder: "Pune", ...register('city') }), error('city')] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "pickupInstructions", children: "Pickup instructions" }), _jsx("textarea", { id: "pickupInstructions", rows: 2, className: "input", placeholder: "e.g. use the service entrance and ask for the kitchen manager", ...register('pickupInstructions') })] })] }), _jsxs("section", { className: "card card-body space-y-4", children: [_jsx("h2", { className: "font-semibold text-ink-900", children: "Food safety" }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "storageConditions", children: "Storage conditions" }), _jsx("input", { id: "storageConditions", className: "input", placeholder: "e.g. refrigerated below 5\u00B0C until pickup", ...register('storageConditions') })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "allergenInfo", children: "Allergen information" }), _jsx("input", { id: "allergenInfo", className: "input", placeholder: "e.g. contains dairy, may contain traces of nuts", ...register('allergenInfo') })] }), _jsxs("div", { children: [_jsx("label", { className: "label", htmlFor: "handlingInstructions", children: "Handling instructions" }), _jsx("input", { id: "handlingInstructions", className: "input", placeholder: "e.g. transport in clean food-grade containers", ...register('handlingInstructions') })] }), _jsxs("label", { className: "flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900", children: [_jsx("input", { type: "checkbox", className: "mt-0.5 h-4 w-4 rounded border-amber-300 text-brand-600", ...register('safetyDeclaration') }), _jsxs("span", { children: ["I confirm that the information provided about this food is accurate, that it has been stored safely, and that it is fit for consumption at the time of pickup.", _jsx("span", { className: "mt-1 block text-xs", children: error('safetyDeclaration') })] })] })] }), _jsx(Callout, { tone: "warning", title: "Your responsibility as a donor", children: SAFETY_DISCLAIMER }), _jsxs("div", { className: "flex justify-end gap-2", children: [_jsx(Link, { to: "/donor/donations", className: "btn-secondary", children: "Cancel" }), _jsx("button", { type: "submit", className: "btn-primary", disabled: isSubmitting, children: isSubmitting ? 'Saving…' : editing ? 'Save changes' : 'Publish donation' })] })] })] }));
}
