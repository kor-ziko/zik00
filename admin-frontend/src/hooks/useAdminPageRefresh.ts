import { useEffect, useRef } from 'react';

export const ADMIN_PAGE_REFRESH_EVENT = 'zik00-admin-page-refresh';

export function useAdminPageRefresh(refresh: () => void | Promise<void>) {
  const refreshRef = useRef(refresh);

  useEffect(() => {
    refreshRef.current = refresh;
  }, [refresh]);

  useEffect(() => {
    const handleRefresh = () => { void refreshRef.current(); };
    window.addEventListener(ADMIN_PAGE_REFRESH_EVENT, handleRefresh);
    return () => window.removeEventListener(ADMIN_PAGE_REFRESH_EVENT, handleRefresh);
  }, []);
}
