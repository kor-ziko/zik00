import Search from 'lucide-react/dist/esm/icons/search.js';
import X from 'lucide-react/dist/esm/icons/x.js';
import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from 'react';
import { popularKeywords } from '../../data';
import { useRecentSearches } from '../../hooks/useRecentSearches';
import { useLocale } from '../../locale';
import TrendMark from './TrendMark';

const searchScopeValues = ['all', 'title', 'title-content'] as const;

function SearchBox() {
  const { copy } = useLocale();
  const initialParams = new URLSearchParams(window.location.search);
  const [query, setQuery] = useState(() => (
    window.location.pathname === '/search' ? initialParams.get('q') ?? '' : ''
  ));
  const [scope, setScope] = useState(() => {
    const initialScope = initialParams.get('scope');
    return searchScopeValues.includes(initialScope as typeof searchScopeValues[number]) ? initialScope! : 'all';
  });
  const [searchOpen, setSearchOpen] = useState(false);
  const searchAreaRef = useRef<HTMLDivElement>(null);
  const {
    recentSearches,
    addRecentSearch,
    removeRecentSearch,
    clearRecentSearches,
  } = useRecentSearches();

  useEffect(() => {
    if (!searchOpen) return undefined;

    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!searchAreaRef.current?.contains(event.target as Node)) {
        setSearchOpen(false);
      }
    };

    document.addEventListener('mousedown', closeOnOutsideClick);
    return () => document.removeEventListener('mousedown', closeOnOutsideClick);
  }, [searchOpen]);

  const search = (keyword: string) => {
    const normalized = keyword.trim();
    if (!normalized) return;

    setQuery(normalized);
    addRecentSearch(normalized);
    setSearchOpen(false);
    const params = new URLSearchParams({ q: normalized, scope });
    window.location.assign(`/search?${params.toString()}`);
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    const normalized = query.trim();
    if (!normalized) {
      event.preventDefault();
      return;
    }
    setQuery(normalized);
    addRecentSearch(normalized);
    setSearchOpen(false);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Escape') {
      setSearchOpen(false);
      event.currentTarget.blur();
    }
  };

  return (
    <div className="search-area" ref={searchAreaRef}>
      <form className="search-form" action="/search" method="get" onSubmit={handleSubmit}>
        <label className="sr-only" htmlFor="product-search">{copy.search.label}</label>
        <select
          name="scope"
          aria-label={copy.search.label}
          value={scope}
          onChange={(event) => setScope(event.target.value)}
        >
          {searchScopeValues.map((value, index) => (
            <option key={value} value={value}>{copy.search.scopes[index]}</option>
          ))}
        </select>
        <span className="search-divider" aria-hidden="true" />
        <input
          id="product-search"
          name="q"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          onFocus={() => setSearchOpen(true)}
          onKeyDown={handleKeyDown}
          placeholder={copy.search.placeholder}
          autoComplete="off"
        />
        {query && (
          <button
            className="clear-query-button"
            type="button"
            onClick={() => setQuery('')}
            aria-label={copy.search.clear}
          >
            <X size={17} />
          </button>
        )}
        <button className="search-submit" type="submit" aria-label={copy.search.submit}>
          <Search size={23} />
        </button>
      </form>

      {searchOpen && (
        <section className="search-panel" aria-label={copy.search.label}>
          <div className="recent-section">
            <div className="panel-title-row">
              <h2>{copy.search.recent}</h2>
              {recentSearches.length > 0 && (
                <button type="button" onClick={clearRecentSearches}>{copy.search.clearAll}</button>
              )}
            </div>

            {recentSearches.length > 0 ? (
              <div className="recent-list">
                {recentSearches.map((keyword) => (
                  <span className="recent-chip" key={keyword}>
                    <button type="button" onClick={() => search(keyword)}>{keyword}</button>
                    <button
                      className="remove-recent"
                      type="button"
                      onClick={() => removeRecentSearch(keyword)}
                      aria-label={`${keyword} 삭제`}
                    >
                      <X size={14} />
                    </button>
                  </span>
                ))}
              </div>
            ) : (
              <p className="empty-recent">{copy.search.noRecent}</p>
            )}
          </div>

          <div className="popular-section">
            <div className="panel-title-row popular-heading">
              <div>
                <h2>{copy.search.popular}</h2>
                <p>{copy.search.updatedAt}</p>
              </div>
              <span>{copy.search.live}</span>
            </div>
            <ol className="popular-list">
              {popularKeywords.map((keyword) => (
                <li key={keyword.rank}>
                  <button type="button" onClick={() => search(keyword.label)}>
                    <strong>{keyword.rank}</strong>
                    <span>{keyword.label}</span>
                    <TrendMark trend={keyword.trend} />
                  </button>
                </li>
              ))}
            </ol>
          </div>
        </section>
      )}
    </div>
  );
}

export default SearchBox;
