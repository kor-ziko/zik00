import Search from 'lucide-react/dist/esm/icons/search.js';
import X from 'lucide-react/dist/esm/icons/x.js';
import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from 'react';
import { popularKeywords } from '../../data';
import { useRecentSearches } from '../../hooks/useRecentSearches';
import TrendMark from './TrendMark';

const categories = ['전체', '뷰티', '패션', '식품', '디지털', '리빙'];

function SearchBox() {
  const [query, setQuery] = useState('');
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
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    search(query);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Escape') {
      setSearchOpen(false);
      event.currentTarget.blur();
    }
  };

  return (
    <div className="search-area" ref={searchAreaRef}>
      <form className="search-form" onSubmit={handleSubmit}>
        <label className="sr-only" htmlFor="product-search">상품 검색</label>
        <select aria-label="검색 카테고리" defaultValue="전체">
          {categories.map((category) => (
            <option key={category}>{category}</option>
          ))}
        </select>
        <span className="search-divider" aria-hidden="true" />
        <input
          id="product-search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          onFocus={() => setSearchOpen(true)}
          onKeyDown={handleKeyDown}
          placeholder="상품명 또는 URL을 검색해보세요"
          autoComplete="off"
        />
        {query && (
          <button
            className="clear-query-button"
            type="button"
            onClick={() => setQuery('')}
            aria-label="검색어 지우기"
          >
            <X size={17} />
          </button>
        )}
        <button className="search-submit" type="submit" aria-label="검색">
          <Search size={23} />
        </button>
      </form>

      {searchOpen && (
        <section className="search-panel" aria-label="검색어 추천">
          <div className="recent-section">
            <div className="panel-title-row">
              <h2>최근 검색어</h2>
              {recentSearches.length > 0 && (
                <button type="button" onClick={clearRecentSearches}>전체 삭제</button>
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
              <p className="empty-recent">최근 검색어가 없습니다.</p>
            )}
          </div>

          <div className="popular-section">
            <div className="panel-title-row popular-heading">
              <div>
                <h2>인기 검색어</h2>
                <p>오늘 오후 2시 업데이트</p>
              </div>
              <span>실시간</span>
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
