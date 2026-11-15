/** Formatting helpers shared across dashboards. */
export function formatDateTime(value) {
    if (!value)
        return '—';
    return new Date(value).toLocaleString(undefined, {
        day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
}
export function formatDate(value) {
    if (!value)
        return '—';
    return new Date(value).toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' });
}
export function formatNumber(value) {
    return new Intl.NumberFormat().format(value ?? 0);
}
export function formatQuantity(quantity, unit) {
    const rounded = Math.round(quantity * 100) / 100;
    return `${rounded} ${unit.toLowerCase()}`;
}
/** "in 2h 15m" / "expired 10m ago" */
export function relativeToNow(value) {
    const diffMs = new Date(value).getTime() - Date.now();
    const abs = Math.abs(diffMs);
    const minutes = Math.round(abs / 60000);
    const hours = Math.round(abs / 3600000);
    const days = Math.round(abs / 86400000);
    let text;
    if (minutes < 1)
        text = 'less than a minute';
    else if (minutes < 60)
        text = `${minutes}m`;
    else if (hours < 24)
        text = `${hours}h`;
    else
        text = `${days}d`;
    return diffMs >= 0 ? `in ${text}` : `${text} ago`;
}
export function timeUntil(value) {
    const diffMs = new Date(value).getTime() - Date.now();
    if (diffMs <= 0)
        return 'Expired';
    const hours = Math.floor(diffMs / 3600000);
    const minutes = Math.floor((diffMs % 3600000) / 60000);
    if (hours >= 24)
        return `${Math.floor(hours / 24)}d ${hours % 24}h left`;
    if (hours > 0)
        return `${hours}h ${minutes}m left`;
    return `${minutes}m left`;
}
/** Converts an ISO instant into the value format <input type="datetime-local"> expects. */
export function toLocalInputValue(iso) {
    const date = iso ? new Date(iso) : new Date(Date.now() + 3600000);
    const pad = (n) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
/** datetime-local value -> ISO instant (UTC) for the API. */
export function fromLocalInputValue(value) {
    return new Date(value).toISOString();
}
