import { useEffect, useState } from 'react';

const RECENT_SEARCH_KEY = 'zik00-recent-searches';
const MAX_RECENT_SEARCHES = 8;
const DEFAULT_SEARCHES = ['선케어', '서울 문구', '스니커즈'];

function getStoredSearches(): string[] {
  try {
    const stored = localStorage.getItem(RECENT_SEARCH_KEY);
    return stored ? JSON.parse(stored) : DEFAULT_SEARCHES;
  } catch {
    return DEFAULT_SEARCHES;
  }
}

export function useRecentSearches() {
  const [recentSearches, setRecentSearches] = useState<string[]>(getStoredSearches);

  useEffect(() => {
    localStorage.setItem(RECENT_SEARCH_KEY, JSON.stringify(recentSearches));
  }, [recentSearches]);

  const addRecentSearch = (keyword: string) => {
    const normalized = keyword.trim();
    if (!normalized) return;

    setRecentSearches((current) => [
      normalized,
      ...current.filter((item) => item !== normalized),
    ].slice(0, MAX_RECENT_SEARCHES));
  };

  const removeRecentSearch = (keyword: string) => {
    setRecentSearches((current) => current.filter((item) => item !== keyword));
  };

  const clearRecentSearches = () => setRecentSearches([]);

  return {
    recentSearches,
    addRecentSearch,
    removeRecentSearch,
    clearRecentSearches,
  };
}
