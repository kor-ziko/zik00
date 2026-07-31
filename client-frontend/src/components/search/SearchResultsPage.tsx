import ChevronDown from 'lucide-react/dist/esm/icons/chevron-down.js';
import ChevronLeft from 'lucide-react/dist/esm/icons/chevron-left.js';
import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import Heart from 'lucide-react/dist/esm/icons/heart.js';
import RotateCcw from 'lucide-react/dist/esm/icons/rotate-ccw.js';
import SlidersHorizontal from 'lucide-react/dist/esm/icons/sliders-horizontal.js';
import Star from 'lucide-react/dist/esm/icons/star.js';
import X from 'lucide-react/dist/esm/icons/x.js';
import { useEffect, useMemo, useState } from 'react';
import { searchProducts, type SearchResult } from '../../api/search';
import { useAuthMemory } from '../../auth/AuthMemory';
import { currentRelativeUrl, loginHref } from '../../auth/authNavigation';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

type PriceDraft = { min: string; max: string };

function getInitialQuery() {
  return new URLSearchParams(window.location.search).get('q')?.trim() ?? '';
}

function getInitialCategory() {
  return new URLSearchParams(window.location.search).get('category')?.trim() ?? '';
}

function getInitialScope() {
  return new URLSearchParams(window.location.search).get('scope')?.trim() ?? 'all';
}

function formatPrice(value: number, currency: 'KRW' | 'JPY') {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(value);
}

function SearchResultsPage() {
  const query = useMemo(getInitialQuery, []);
  const scope = useMemo(getInitialScope, []);
  const { accessSessionActive } = useAuthMemory();
  const [result, setResult] = useState<SearchResult | null>(null);
  const [category, setCategory] = useState(getInitialCategory);
  const [brands, setBrands] = useState<string[]>([]);
  const [price, setPrice] = useState<PriceDraft>({ min: '', max: '' });
  const [appliedPrice, setAppliedPrice] = useState<PriceDraft>({ min: '', max: '' });
  const [sort, setSort] = useState('relevance');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterOpen, setFilterOpen] = useState(false);
  const [favoriteIds, setFavoriteIds] = useState<Set<string>>(() => new Set());
  const [reloadKey, setReloadKey] = useState(0);
  const selectedCategoryLabel = category.split(' > ').at(-1) ?? '';

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    searchProducts({
      query,
      scope,
      category,
      brands,
      minPrice: appliedPrice.min ? Number(appliedPrice.min) : undefined,
      maxPrice: appliedPrice.max ? Number(appliedPrice.max) : undefined,
      sort,
      page,
    }, controller.signal)
      .then(setResult)
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === 'AbortError') return;
        setError(reason instanceof Error ? reason.message : '검색 결과를 불러오지 못했습니다.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [appliedPrice, brands, category, page, query, reloadKey, scope, sort]);

  const resetFilters = () => {
    setCategory('');
    setBrands([]);
    setPrice({ min: '', max: '' });
    setAppliedPrice({ min: '', max: '' });
    setPage(0);
  };

  const selectCategory = (value: string) => {
    setCategory(value);
    setPage(0);
  };

  const toggleBrand = (brand: string) => {
    setBrands((current) => (
      current.includes(brand)
        ? current.filter((value) => value !== brand)
        : [...current, brand]
    ));
    setPage(0);
  };

  const applyPrice = () => {
    setAppliedPrice(price);
    setPage(0);
  };

  const toggleFavorite = (productId: string) => {
    if (!accessSessionActive) {
      window.location.assign(loginHref(currentRelativeUrl()));
      return;
    }
    setFavoriteIds((current) => {
      const next = new Set(current);
      if (next.has(productId)) next.delete(productId);
      else next.add(productId);
      return next;
    });
  };

  const filterPanel = (
    <aside className={`search-filter-panel ${filterOpen ? 'is-open' : ''}`} aria-label="검색 필터">
      <div className="search-filter-heading">
        <strong>필터</strong>
        <button type="button" className="filter-reset-button" onClick={resetFilters}>
          <RotateCcw size={14} /> 초기화
        </button>
        <button
          type="button"
          className="filter-close-button"
          aria-label="필터 닫기"
          onClick={() => setFilterOpen(false)}
        >
          <X size={20} />
        </button>
      </div>

      <section className="filter-section">
        <h2>카테고리</h2>
        <label className="filter-option">
          <input type="radio" name="category" checked={!category} onChange={() => selectCategory('')} />
          <span>전체</span>
          <small>{result?.categories.reduce((sum, item) => sum + item.count, 0) ?? 0}</small>
        </label>
        {category && !result?.categories.some((item) => item.value === category) && (
          <label className="filter-option selected-category-path">
            <input type="radio" name="category" checked readOnly />
            <span>{category.split(' > ').at(-1)}</span>
            <small>{result?.totalCount ?? 0}</small>
          </label>
        )}
        {result?.categories.map((item) => (
          <label className="filter-option" key={item.value}>
            <input
              type="radio"
              name="category"
              checked={category === item.value}
              onChange={() => selectCategory(item.value)}
            />
            <span>{item.value}</span>
            <small>{item.count}</small>
          </label>
        ))}
      </section>

      <section className="filter-section">
        <h2>브랜드</h2>
        {result?.brands.map((item) => (
          <label className="filter-option" key={item.value}>
            <input
              type="checkbox"
              checked={brands.includes(item.value)}
              onChange={() => toggleBrand(item.value)}
            />
            <span>{item.value}</span>
            <small>{item.count}</small>
          </label>
        ))}
      </section>

      <section className="filter-section">
        <h2>가격</h2>
        <div className="price-filter">
          <label>
            <span className="sr-only">최소 가격</span>
            <input
              type="number"
              min="0"
              inputMode="numeric"
              placeholder="최소 금액"
              value={price.min}
              onChange={(event) => setPrice((current) => ({ ...current, min: event.target.value }))}
            />
          </label>
          <span>~</span>
          <label>
            <span className="sr-only">최대 가격</span>
            <input
              type="number"
              min="0"
              inputMode="numeric"
              placeholder="최대 금액"
              value={price.max}
              onChange={(event) => setPrice((current) => ({ ...current, max: event.target.value }))}
            />
          </label>
        </div>
        <button className="price-apply-button" type="button" onClick={applyPrice}>가격 적용</button>
      </section>
    </aside>
  );

  return (
    <div className="app-shell search-page-shell">
      <SiteHeader />
      <main className="search-page header-inner">
        <nav className="search-breadcrumb" aria-label="현재 위치">
          <a href="/">홈</a><ChevronRight size={13} /><span>검색 결과</span>
        </nav>

        <header className="search-result-heading">
          <div>
            <p>SEARCH RESULT</p>
            <h1>
              {query
                ? <><strong>‘{query}’</strong> 검색 결과</>
                : selectedCategoryLabel || '전체 상품'}
            </h1>
            <span>총 {result?.totalCount ?? 0}개의 상품을 찾았습니다.</span>
          </div>
          <button type="button" className="mobile-filter-button" onClick={() => setFilterOpen(true)}>
            <SlidersHorizontal size={17} /> 필터
          </button>
        </header>

        <div className="search-content">
          {filterPanel}
          {filterOpen && <button className="filter-backdrop" type="button" aria-label="필터 닫기" onClick={() => setFilterOpen(false)} />}

          <section className="search-products" aria-live="polite">
            <div className="search-toolbar">
              <span>{result?.totalCount ?? 0}개 상품</span>
              <label className="sort-control">
                <span className="sr-only">정렬 기준</span>
                <select value={sort} onChange={(event) => { setSort(event.target.value); setPage(0); }}>
                  <option value="relevance">추천순</option>
                  <option value="reviews">리뷰 많은순</option>
                  <option value="rating">평점 높은순</option>
                  <option value="price-low">낮은 가격순</option>
                  <option value="price-high">높은 가격순</option>
                </select>
                <ChevronDown size={15} aria-hidden="true" />
              </label>
            </div>

            {loading && (
              <div className="search-product-grid" aria-label="검색 중">
                {Array.from({ length: 8 }, (_, index) => (
                  <div className="search-product-skeleton" key={index}>
                    <span /><i /><i /><i />
                  </div>
                ))}
              </div>
            )}

            {!loading && error && (
              <div className="search-state">
                <h2>검색 결과를 불러오지 못했습니다.</h2>
                <p>{error}</p>
                <button type="button" onClick={() => setReloadKey((value) => value + 1)}>다시 시도</button>
              </div>
            )}

            {!loading && !error && result?.items.length === 0 && (
              <div className="search-state">
                <h2>조건에 맞는 상품이 없습니다.</h2>
                <p>검색어나 필터 조건을 변경해 보세요.</p>
                <button type="button" onClick={resetFilters}>필터 초기화</button>
              </div>
            )}

            {!loading && !error && result && result.items.length > 0 && (
              <>
                <div className="search-product-grid">
                  {result.items.map((product) => {
                    const favorite = favoriteIds.has(product.productId);
                    const productHref = `/products/${product.productId}`;
                    const discountRate = product.originalPrice
                      ? Math.round((1 - product.price / product.originalPrice) * 100)
                      : 0;
                    return (
                      <article className="search-product-card" key={product.productId}>
                        <div className="search-product-image">
                          <a href={productHref}>
                            <img src={product.imageUrl} alt={product.name} loading="lazy" />
                          </a>
                          {product.badge && <span className="search-product-badge">{product.badge}</span>}
                          <button
                            type="button"
                            className={favorite ? 'is-favorite' : ''}
                            aria-label={`${product.name} 찜하기`}
                            aria-pressed={favorite}
                            onClick={() => toggleFavorite(product.productId)}
                          >
                            <Heart size={19} fill={favorite ? 'currentColor' : 'none'} />
                          </button>
                        </div>
                        <a
                          className="search-product-info"
                          href={productHref}
                        >
                          <p className="search-product-source">{product.source} · {product.brand}</p>
                          <h2>{product.name}</h2>
                          <div className="search-product-price">
                            {discountRate > 0 && <em>{discountRate}%</em>}
                            <strong>{formatPrice(product.price, product.currency)}</strong>
                          </div>
                          {product.originalPrice && (
                            <del>{formatPrice(product.originalPrice, product.currency)}</del>
                          )}
                          <div className="search-product-meta">
                            <span><Star size={13} fill="currentColor" /> {product.rating.toFixed(1)}</span>
                            <span>리뷰 {product.reviewCount.toLocaleString()}</span>
                          </div>
                          <p className={product.freeShipping ? 'free-shipping' : ''}>
                            {product.freeShipping ? '무료배송' : '예상 국제배송비 별도'}
                          </p>
                        </a>
                      </article>
                    );
                  })}
                </div>

                {result.totalPages > 1 && (
                  <nav className="search-pagination" aria-label="검색 결과 페이지">
                    <button
                      type="button"
                      aria-label="이전 페이지"
                      disabled={page === 0}
                      onClick={() => setPage((value) => Math.max(0, value - 1))}
                    >
                      <ChevronLeft size={17} />
                    </button>
                    {Array.from({ length: result.totalPages }, (_, index) => (
                      <button
                        type="button"
                        className={page === index ? 'active' : ''}
                        aria-current={page === index ? 'page' : undefined}
                        onClick={() => setPage(index)}
                        key={index}
                      >
                        {index + 1}
                      </button>
                    ))}
                    <button
                      type="button"
                      aria-label="다음 페이지"
                      disabled={page + 1 >= result.totalPages}
                      onClick={() => setPage((value) => value + 1)}
                    >
                      <ChevronRight size={17} />
                    </button>
                  </nav>
                )}
              </>
            )}
          </section>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

export default SearchResultsPage;
