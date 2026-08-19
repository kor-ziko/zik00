import Check from 'lucide-react/dist/esm/icons/check.js';
import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import Minus from 'lucide-react/dist/esm/icons/minus.js';
import PackageCheck from 'lucide-react/dist/esm/icons/package-check.js';
import Plus from 'lucide-react/dist/esm/icons/plus.js';
import ShoppingBag from 'lucide-react/dist/esm/icons/shopping-bag.js';
import Trash2 from 'lucide-react/dist/esm/icons/trash-2.js';
import { useEffect, useMemo, useState } from 'react';
import { getCart, removeCartItem, ShoppingAuthRequiredError, updateCartQuantity, type CartItem } from '../../api/shopping';
import { loginHref } from '../../auth/authNavigation';
import { useOperatingExchangeRate } from '../../hooks/useOperatingExchangeRate';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

function formatPrice(value: number, currency: 'KRW' | 'JPY') {
  const locale = currency === 'KRW' ? 'ko-KR' : 'ja-JP';
  return new Intl.NumberFormat(locale, { style: 'currency', currency, maximumFractionDigits: 0 }).format(value);
}

function CartPage() {
  const [items, setItems] = useState<CartItem[] | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [workingIds, setWorkingIds] = useState<Set<number>>(new Set());
  const [error, setError] = useState('');
  const { rate, toJpy } = useOperatingExchangeRate();

  const loadCart = () => {
    setError('');
    getCart().then((result) => {
      setItems(result.items);
      setSelectedIds(new Set(result.items.map((item) => item.id)));
    }).catch((reason: unknown) => {
      if (reason instanceof ShoppingAuthRequiredError) {
        window.location.replace(loginHref('/cart'));
        return;
      }
      setError(reason instanceof Error ? reason.message : '장바구니를 불러오지 못했습니다.');
    });
  };

  useEffect(loadCart, []);

  const selectedItems = useMemo(() => items?.filter((item) => selectedIds.has(item.id)) ?? [], [items, selectedIds]);
  const productTotalJpy = useMemo(() => selectedItems.reduce<number | null>((result, item) => {
    if (result === null) return null;
    const unitPriceJpy = item.currency === 'JPY'
      ? item.unitPrice
      : rate === null ? null : Math.ceil(item.unitPrice * rate);
    return unitPriceJpy === null ? null : result + unitPriceJpy * item.quantity;
  }, 0), [rate, selectedItems]);

  const markWorking = (id: number, working: boolean) => setWorkingIds((current) => {
    const next = new Set(current);
    if (working) next.add(id); else next.delete(id);
    return next;
  });

  const changeQuantity = async (item: CartItem, quantity: number) => {
    if (quantity < 1 || quantity > 10 || workingIds.has(item.id)) return;
    markWorking(item.id, true);
    try {
      const updated = await updateCartQuantity(item.id, quantity);
      setItems((current) => current?.map((candidate) => candidate.id === item.id ? updated : candidate) ?? []);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '수량을 변경하지 못했습니다.');
    } finally {
      markWorking(item.id, false);
    }
  };

  const remove = async (itemId: number) => {
    markWorking(itemId, true);
    try {
      await removeCartItem(itemId);
      setItems((current) => current?.filter((item) => item.id !== itemId) ?? []);
      setSelectedIds((current) => { const next = new Set(current); next.delete(itemId); return next; });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '상품을 삭제하지 못했습니다.');
    } finally {
      markWorking(itemId, false);
    }
  };

  const removeSelected = async () => {
    const ids = [...selectedIds];
    if (!ids.length) return;
    setWorkingIds(new Set(ids));
    try {
      await Promise.all(ids.map(removeCartItem));
      setItems((current) => current?.filter((item) => !selectedIds.has(item.id)) ?? []);
      setSelectedIds(new Set());
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '선택 상품을 삭제하지 못했습니다.');
      loadCart();
    } finally {
      setWorkingIds(new Set());
    }
  };

  const toggleAll = () => setSelectedIds((current) => current.size === (items?.length ?? 0)
    ? new Set()
    : new Set(items?.map((item) => item.id) ?? []));

  return (
    <div className="app-shell shopping-page-shell">
      <SiteHeader />
      <main className="cart-page header-inner">
        <nav className="information-breadcrumb" aria-label="현재 위치"><a href="/">홈</a><ChevronRight size={13} /><span>장바구니</span></nav>
        <header className="shopping-page-heading"><p>SHOPPING CART</p><h1>장바구니</h1><span>선택한 옵션과 수량을 확인한 뒤 주문을 진행하세요.</span></header>
        {error && <p className="shopping-page-error" role="alert">{error}<button type="button" onClick={() => setError('')}>닫기</button></p>}
        {!items ? <div className="information-loading" role="status">장바구니를 불러오는 중입니다.</div> : items.length === 0 ? (
          <section className="shopping-empty"><ShoppingBag size={34} /><h2>장바구니가 비어 있습니다.</h2><p>상품 상세 페이지에서 옵션을 선택하고 장바구니에 담아보세요.</p><a href="/search">상품 둘러보기</a></section>
        ) : (
          <div className="cart-layout">
            <section className="cart-items-panel" aria-label="장바구니 상품">
              <div className="cart-list-toolbar"><label><input type="checkbox" checked={selectedIds.size === items.length} onChange={toggleAll} /><span>전체 선택 ({selectedIds.size}/{items.length})</span></label><button type="button" onClick={removeSelected}><Trash2 size={15} />선택 삭제</button></div>
              <div className="cart-item-list">
                {items.map((item) => (
                  <article className="cart-item" key={item.id}>
                    <label className="cart-item-check"><input type="checkbox" checked={selectedIds.has(item.id)} onChange={() => setSelectedIds((current) => { const next = new Set(current); if (next.has(item.id)) next.delete(item.id); else next.add(item.id); return next; })} /><span className="sr-only">{item.productName} 선택</span></label>
                    <a className="cart-item-image" href={`/products/${item.productId}`}><img src={item.imageUrl || '/assets/product-shoes.webp'} alt={item.productName} /></a>
                    <div className="cart-item-info"><small>{item.brand || '브랜드 정보 없음'}</small><a href={`/products/${item.productId}`}><h2>{item.productName}</h2></a>{Object.keys(item.selectedOptions).length > 0 ? <dl>{Object.entries(item.selectedOptions).map(([name, value]) => <div key={name}><dt>{name}</dt><dd>{value}</dd></div>)}</dl> : <p>단일 상품</p>}</div>
                    <div className="cart-item-purchase"><strong>{toJpy(item.unitPrice, item.currency) === null ? '엔화 환산 중' : formatPrice((toJpy(item.unitPrice, item.currency) ?? 0) * item.quantity, 'JPY')}</strong><div className="cart-quantity"><button type="button" aria-label="수량 줄이기" disabled={workingIds.has(item.id) || item.quantity <= 1} onClick={() => changeQuantity(item, item.quantity - 1)}><Minus size={13} /></button><span>{item.quantity}</span><button type="button" aria-label="수량 늘리기" disabled={workingIds.has(item.id) || item.quantity >= 10} onClick={() => changeQuantity(item, item.quantity + 1)}><Plus size={13} /></button></div><button className="cart-item-remove" type="button" aria-label={`${item.productName} 삭제`} onClick={() => remove(item.id)} disabled={workingIds.has(item.id)}><Trash2 size={17} /></button></div>
                  </article>
                ))}
              </div>
            </section>

            <aside className="cart-summary">
              <h2>주문 예정 금액</h2><dl><div><dt>선택 상품</dt><dd>{selectedItems.reduce((sum, item) => sum + item.quantity, 0)}개</dd></div><div><dt>상품 금액</dt><dd>{productTotalJpy === null ? '엔화 환산 중' : formatPrice(productTotalJpy, 'JPY')}</dd></div><div><dt>예상 국제배송비</dt><dd>결제 단계에서 자동 계산</dd></div></dl>
              <div className="cart-summary-total"><span>상품 금액</span><strong>{productTotalJpy === null ? '엔화 환산 중' : formatPrice(productTotalJpy, 'JPY')}</strong></div>
              <button type="button" disabled={selectedItems.length === 0} onClick={() => window.location.assign(`/checkout?items=${[...selectedIds].join(',')}`)}><Check size={18} />선택 상품 주문하기</button>
              <small><PackageCheck size={15} />예상 국제배송비를 포함한 최종 결제금액은 다음 단계에서 확인됩니다.</small>
            </aside>
          </div>
        )}
      </main>
      <SiteFooter />
    </div>
  );
}

export default CartPage;
