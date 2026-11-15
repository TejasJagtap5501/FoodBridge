import { useCallback, useEffect, useState } from 'react';
import { toApiError } from '@/api/client';
/**
 * Small data-fetching hook: runs a loader, tracks loading/error state and
 * exposes {@code reload} for retry buttons.
 *
 * <p>Deliberately dependency-free (no react-query) because the app's data needs
 * are simple and an extra cache layer would add indirection without benefit.</p>
 */
export function useApi(loader, deps = []) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [nonce, setNonce] = useState(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const run = useCallback(loader, deps);
    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        setError(null);
        run()
            .then((result) => {
            if (!cancelled)
                setData(result);
        })
            .catch((err) => {
            if (!cancelled)
                setError(toApiError(err).message);
        })
            .finally(() => {
            if (!cancelled)
                setLoading(false);
        });
        return () => {
            cancelled = true;
        };
    }, [run, nonce]);
    const reload = useCallback(() => setNonce((value) => value + 1), []);
    return { data, loading, error, reload };
}
