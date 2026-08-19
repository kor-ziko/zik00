import Check from 'lucide-react/dist/esm/icons/check.js';
import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import CreditCard from 'lucide-react/dist/esm/icons/credit-card.js';
import MapPin from 'lucide-react/dist/esm/icons/map-pin.js';
import PackageCheck from 'lucide-react/dist/esm/icons/package-check.js';
import Smartphone from 'lucide-react/dist/esm/icons/smartphone.js';
import ShieldCheck from 'lucide-react/dist/esm/icons/shield-check.js';
import WalletCards from 'lucide-react/dist/esm/icons/wallet-cards.js';
import { useEffect, useMemo, useState } from 'react';
import { getMypageSection, type DeliveryAddress, type ProfileData } from '../../api/mypage';
import { preparePayment, startPayment, type PaymentPrepareResponse } from '../../api/payment';
import { ShoppingAuthRequiredError } from '../../api/shopping';
import { loginHref } from '../../auth/authNavigation';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

function selectedCartIds() {
  const raw = new URLSearchParams(window.location.search).get('items') || '';
  return [...new Set(raw.split(',').map(Number).filter((value) => Number.isInteger(value) && value > 0))];
}

function formatPrice(value: number, currency: 'KRW' | 'JPY') {
  return new Intl.NumberFormat(currency === 'KRW' ? 'ko-KR' : 'ja-JP', {
    style: 'currency', currency, maximumFractionDigits: 0,
  }).format(value);
}

function formatCustomsAmount(value: number, order: PaymentPrepareResponse) {
  if (!order.customsFinalizationRequired) return formatPrice(value, order.currency);
  return value > 0 ? `${formatPrice(value, order.currency)} + 미확정` : '통관 시 확정';
}

function CheckoutPage() {
  const cartItemIds = useMemo(selectedCartIds, []);
  const [addresses, setAddresses] = useState<DeliveryAddress[]>([]);
  const [addressId, setAddressId] = useState<number | null>(null);
  const [order, setOrder] = useState<PaymentPrepareResponse | null>(null);
  const [agreed, setAgreed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!cartItemIds.length) {
      setLoading(false);
      return;
    }
    getMypageSection('profile').then((value) => {
      const profile = value as ProfileData;
      setAddresses(profile.addresses);
      const initial = profile.addresses.find((address) => address.defaultAddress) || profile.addresses[0];
      if (initial) setAddressId(initial.id);
      else setLoading(false);
    }).catch((reason: unknown) => {
      setError(reason instanceof Error ? reason.message : '배송지 정보를 불러오지 못했습니다.');
      setLoading(false);
    });
  }, [cartItemIds]);

  useEffect(() => {
    if (addressId === null || !cartItemIds.length) return;
    setLoading(true);
    setError('');
    setOrder(null);
    preparePayment(cartItemIds, addressId).then((value) => {
      setOrder(value);
      setPaymentMethod((current) => value.paymentMethods.some((method) => method.code === current)
        ? current
        : value.paymentMethods[0]?.code || '');
    }).catch((reason: unknown) => {
      if (reason instanceof ShoppingAuthRequiredError) {
        window.location.replace(loginHref(window.location.pathname + window.location.search));
        return;
      }
      setError(reason instanceof Error ? reason.message : '주문 정보를 준비하지 못했습니다.');
    }).finally(() => setLoading(false));
  }, [addressId, cartItemIds]);

  const requestPayment = async () => {
    if (!order || !agreed || !paymentMethod || paying) return;
    if (!order.paymentEnabled) {
      setError('SBPS 계약 정보 또는 공개 콜백 주소 설정이 완료되지 않았습니다.');
      return;
    }
    setPaying(true);
    setError('');
    try {
      const response = await startPayment(order.paymentId, paymentMethod);
      const form = document.createElement('form');
      form.method = 'POST';
      form.action = response.requestUrl;
      form.acceptCharset = 'Shift_JIS';
      Object.entries(response.fields).forEach(([name, value]) => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        form.appendChild(input);
      });
      document.body.appendChild(form);
      form.submit();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '결제를 완료하지 못했습니다.');
      setPaying(false);
    }
  };

  return (
    <div className="app-shell shopping-page-shell">
      <SiteHeader />
      <main className="checkout-page header-inner">
        <nav className="information-breadcrumb" aria-label="현재 위치"><a href="/">홈</a><ChevronRight size={13} /><a href="/cart">장바구니</a><ChevronRight size={13} /><span>주문/결제</span></nav>
        <header className="shopping-page-heading"><p>CHECKOUT</p><h1>주문/결제</h1><span>배송지와 결제수단을 확인한 뒤 결제를 진행하세요.</span></header>
        {error && <p className="shopping-page-error" role="alert">{error}<button type="button" onClick={() => setError('')}>닫기</button></p>}
        {loading ? <div className="information-loading" role="status">주문 정보를 준비하는 중입니다.</div> : !cartItemIds.length ? (
          <section className="shopping-empty"><PackageCheck size={34} /><h2>결제할 상품이 선택되지 않았습니다.</h2><p>장바구니에서 주문할 상품을 선택해 주세요.</p><a href="/cart">장바구니로 돌아가기</a></section>
        ) : !addresses.length ? (
          <section className="shopping-empty"><MapPin size={34} /><h2>등록된 배송지가 없습니다.</h2><p>결제를 진행하려면 먼저 배송지를 등록해 주세요.</p><a href="/mypage/profile">배송지 등록하기</a></section>
        ) : order ? (
          <div className="checkout-layout">
            <div className="checkout-main">
              <section className="checkout-section">
                <div className="checkout-section-title"><MapPin size={19} /><h2>배송지</h2><a href="/mypage/profile">배송지 관리</a></div>
                <div className="checkout-address-list">
                  {addresses.map((address) => <label className={addressId === address.id ? 'is-selected' : ''} key={address.id}><input type="radio" name="address" checked={addressId === address.id} onChange={() => setAddressId(address.id)} /><span><strong>{address.addressName}{address.defaultAddress && <b>기본</b>}</strong><small>{address.receiverName} · {address.receiverPhone}</small><small>〒{address.zipCode} {address.province} {address.detailAddress}</small></span></label>)}
                </div>
              </section>

              <section className="checkout-section">
                <div className="checkout-section-title"><PackageCheck size={19} /><h2>주문 상품</h2><span>{order.items.length}건</span></div>
                <div className="checkout-product-list">
                  {order.items.map((item) => <article key={item.cartItemId}><img src={item.imageUrl || '/assets/product-shoes.webp'} alt={item.productName} /><div><small>{item.brand || '브랜드 정보 없음'}</small><h3>{item.productName}</h3>{Object.keys(item.selectedOptions).length ? <p>{Object.entries(item.selectedOptions).map(([name, value]) => `${name}: ${value}`).join(' / ')}</p> : <p>단일 상품</p>}<span>수량 {item.quantity}개</span></div><strong>{formatPrice(item.subtotal, order.currency)}</strong></article>)}
                </div>
              </section>

              <section className="checkout-section">
                <div className="checkout-section-title"><CreditCard size={19} /><h2>결제수단</h2></div>
                <div className="checkout-payment-methods" role="radiogroup" aria-label="결제수단 선택">
                  {order.paymentMethods.map((method) => {
                    const MethodIcon = method.code === 'credit3d2' ? CreditCard : method.code === 'paypay' ? Smartphone : WalletCards;
                    return <label className={paymentMethod === method.code ? 'is-selected' : ''} key={method.code}>
                      <input type="radio" name="paymentMethod" value={method.code} checked={paymentMethod === method.code} onChange={() => setPaymentMethod(method.code)} />
                      <MethodIcon size={23} />
                      <span><strong>{method.label}</strong><small>{method.description}</small></span>
                      <Check className="checkout-payment-check" size={17} />
                    </label>;
                  })}
                </div>
                {!order.paymentMethods.length && <p className="checkout-config-notice">사용 가능한 결제수단이 등록되지 않았습니다.</p>}
                <p className="checkout-provider-note">선택한 결제수단의 SBPS 보안 결제화면으로 이동합니다.</p>
                {!order.paymentEnabled && <p className="checkout-config-notice">SBPS 계약 정보와 공개 콜백 주소를 등록하면 실제 결제창이 열립니다.</p>}
              </section>
            </div>

            <aside className="checkout-summary">
              <h2>최종 결제 금액</h2><dl>
                <div><dt>상품 금액</dt><dd>{formatPrice(order.productAmount, order.currency)}</dd></div>
                <div><dt>국내 배송비</dt><dd>{formatPrice(order.domesticShippingFee, order.currency)}</dd></div>
                {order.agencyFee > 0 && <div><dt>구매대행 수수료</dt><dd>{formatPrice(order.agencyFee, order.currency)}</dd></div>}
                <div><dt>예상 국제배송비 <small>(관부가세 미포함)</small></dt><dd>{formatPrice(order.estimatedShippingFee, order.currency)}</dd></div>
                <div><dt>예상 관세</dt><dd>{formatCustomsAmount(order.estimatedDuty, order)}</dd></div>
                <div><dt>예상 일본 소비세</dt><dd>{formatCustomsAmount(order.estimatedConsumptionTax, order)}</dd></div>
                <div><dt>예상 관부가세 합계</dt><dd>{formatCustomsAmount(order.estimatedImportCharges, order)}</dd></div>
              </dl>
              <p className="checkout-config-notice">예상 국제배송비 범위 {formatPrice(order.estimatedShippingMin, order.currency)}~{formatPrice(order.estimatedShippingMax, order.currency)} · 국제배송비에는 관부가세가 포함되지 않습니다. 실측 배송비 차액은 입고 후 추가 청구 또는 환불됩니다.</p>
              <p className="checkout-config-notice">{order.customsFinalizationRequired
                ? '계산 가능한 예상 관부가세만 결제 금액에 포함되었습니다. 확정되지 않은 품목의 관부가세는 통관 후 추가 정산됩니다.'
                : '표시된 예상 관부가세가 최종 결제 금액에 포함되어 있습니다.'}</p>
              <div className="checkout-total"><span>결제 금액</span><strong>{formatPrice(order.totalAmount, order.currency)}</strong></div>
              <label className="checkout-agreement"><input type="checkbox" checked={agreed} onChange={(event) => setAgreed(event.target.checked)} /><span>주문 내용과 결제 정보를 확인했으며 구매에 동의합니다.</span></label>
              <button type="button" disabled={!agreed || !paymentMethod || paying} onClick={requestPayment}><Check size={18} />{paying ? '결제창 연결 중' : `${formatPrice(order.totalAmount, order.currency)} 결제하기`}</button>
              <small><ShieldCheck size={15} />결제 완료 여부와 금액은 서버에서 다시 확인합니다.</small>
            </aside>
          </div>
        ) : null}
      </main>
      <SiteFooter />
    </div>
  );
}

export default CheckoutPage;
