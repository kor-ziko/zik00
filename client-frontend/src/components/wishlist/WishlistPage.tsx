import Bell from 'lucide-react/dist/esm/icons/bell.js';
import Bookmark from 'lucide-react/dist/esm/icons/bookmark.js';
import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import Gift from 'lucide-react/dist/esm/icons/gift.js';
import ReceiptText from 'lucide-react/dist/esm/icons/receipt-text.js';
import Search from 'lucide-react/dist/esm/icons/search.js';
import Truck from 'lucide-react/dist/esm/icons/truck.js';
import UserRound from 'lucide-react/dist/esm/icons/user-round.js';
import { useEffect, useMemo, useState } from 'react';
import { getWishlist, removeWishlist, ShoppingAuthRequiredError, type WishlistItem } from '../../api/shopping';
import { loginHref } from '../../auth/authNavigation';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

function formatPrice(value: number, currency: 'KRW' | 'JPY') {
  const locale = currency === 'KRW' ? 'ko-KR' : 'ja-JP';
  return new Intl.NumberFormat(locale, { style: 'currency', currency, maximumFractionDigits: 0 }).format(value);
}

function WishlistPage() {
  const [items, setItems] = useState<WishlistItem[] | null>(null);
  const [error, setError] = useState('');
  const [removingId, setRemovingId] = useState<number | null>(null);
  const [query, setQuery] = useState('');
  const [sort, setSort] = useState<'newest' | 'oldest' | 'price-low' | 'price-high'>('newest');

  const loadItems = () => {
    setError('');
    getWishlist().then(setItems).catch((reason: unknown) => {
      if (reason instanceof ShoppingAuthRequiredError) {
        window.location.replace(loginHref('/wishlist'));
        return;
      }
      setError(reason instanceof Error ? reason.message : '찜 목록을 불러오지 못했습니다.');
    });
  };

  useEffect(loadItems, []);

  const remove = async (item: WishlistItem) => {
    const previousItems = items ?? [];
    setRemovingId(item.id);
    setItems(previousItems.filter((candidate) => candidate.id !== item.id));
    setError('');
    try {
      await removeWishlist(item.productId);
    } catch (reason) {
      setItems(previousItems);
      setError(reason instanceof Error ? reason.message : '찜한 상품을 삭제하지 못했습니다.');
    } finally {
      setRemovingId(null);
    }
  };

  const visibleItems = useMemo(() => {
    const keyword = query.trim().toLocaleLowerCase();
    const filtered = (items ?? []).filter((item) => (
      !keyword || `${item.productName} ${item.brand ?? ''}`.toLocaleLowerCase().includes(keyword)
    ));
    return [...filtered].sort((left, right) => {
      if (sort === 'price-low') return left.price - right.price;
      if (sort === 'price-high') return right.price - left.price;
      const dateOrder = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
      return sort === 'oldest' ? -dateOrder : dateOrder;
    });
  }, [items, query, sort]);

  return (
    <div className="app-shell shopping-page-shell">
      <SiteHeader />
      <main className="wishlist-page">
        <div className="header-inner">
          <nav className="information-breadcrumb" aria-label="현재 위치"><a href="/">홈</a><ChevronRight size={13} /><span>찜</span></nav>
          <header className="shopping-page-heading"><p>MY ZIK:00</p><h1>찜한 상품</h1><span>관심 있는 상품을 모아두고 다시 확인할 수 있습니다.</span></header>
          <div className="wishlist-account-layout">
            <aside className="wishlist-account-sidebar" aria-label="마이페이지 메뉴">
              <section>
                <h2>쇼핑</h2>
                <nav>
                  <a href="/mypage/orders"><ReceiptText size={18} /><span>구매내역</span></a>
                  <a className="active" href="/wishlist"><Bookmark size={18} /><span>찜한 상품</span></a>
                  <a href="/cart"><Truck size={18} /><span>장바구니</span></a>
                  <a href="/mypage/coupons"><Gift size={18} /><span>쿠폰함</span></a>
                </nav>
              </section>
              <section>
                <h2>설정</h2>
                <nav><a href="/mypage/profile"><UserRound size={18} /><span>회원정보수정</span></a></nav>
              </section>
              <a className="wishlist-help-link" href="/#support"><Bell size={17} />고객센터</a>
            </aside>

            <section className="wishlist-account-content">
              <div className="wishlist-content-heading">
                <div><h2>찜하기</h2><span>총 {items?.length ?? 0}개</span></div>
                <div className="wishlist-toolbar">
                  <label><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="찜한 상품 검색" aria-label="찜한 상품 검색" /></label>
                  <select value={sort} onChange={(event) => setSort(event.target.value as typeof sort)} aria-label="찜한 상품 정렬">
                    <option value="newest">최신순</option><option value="oldest">오래된순</option><option value="price-low">낮은 가격순</option><option value="price-high">높은 가격순</option>
                  </select>
                </div>
              </div>
              {error && <p className="shopping-page-error" role="alert">{error}<button type="button" onClick={loadItems}>다시 시도</button></p>}
              {!items ? <div className="information-loading" role="status">찜 목록을 불러오는 중입니다.</div> : items.length === 0 ? (
                <div className="shopping-empty"><Bookmark size={34} /><h2>아직 찜한 상품이 없습니다.</h2><p>관심 있는 상품의 책갈피 버튼을 눌러 저장해 보세요.</p><a href="/search">상품 둘러보기</a></div>
              ) : visibleItems.length === 0 ? (
                <div className="shopping-empty compact"><Search size={30} /><h2>검색 결과가 없습니다.</h2><p>다른 상품명이나 브랜드로 검색해 보세요.</p></div>
              ) : (
                <div className="wishlist-grid" aria-label="찜한 상품 목록">
                  {visibleItems.map((item) => (
                    <article className="wishlist-card" key={item.id}>
                      <a className="wishlist-card-image" href={`/products/${item.productId}`}><img src={item.imageUrl || '/assets/product-shoes.webp'} alt={item.productName} /></a>
                      <button className="wishlist-heart-button" type="button" aria-label={`${item.productName} 찜 취소`} disabled={removingId === item.id} onClick={() => void remove(item)} title="찜 취소"><Bookmark size={19} fill="currentColor" /></button>
                      <div className="wishlist-card-body"><small>{item.brand || '브랜드 정보 없음'}</small><a href={`/products/${item.productId}`}><h3>{item.productName}</h3></a><strong>{formatPrice(item.price, item.currency)}</strong></div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

export default WishlistPage;
