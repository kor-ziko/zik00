import Check from 'lucide-react/dist/esm/icons/check.js';
import ChevronDown from 'lucide-react/dist/esm/icons/chevron-down.js';
import ChevronLeft from 'lucide-react/dist/esm/icons/chevron-left.js';
import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import ExternalLink from 'lucide-react/dist/esm/icons/external-link.js';
import Bookmark from 'lucide-react/dist/esm/icons/bookmark.js';
import Minus from 'lucide-react/dist/esm/icons/minus.js';
import Plus from 'lucide-react/dist/esm/icons/plus.js';
import RotateCcw from 'lucide-react/dist/esm/icons/rotate-ccw.js';
import ShoppingBag from 'lucide-react/dist/esm/icons/shopping-bag.js';
import Truck from 'lucide-react/dist/esm/icons/truck.js';
import { useEffect, useMemo, useRef, useState } from 'react';
import { getDeliveryEstimate, getLandedPriceEstimate, getProductDetail, type DeliveryEstimate, type LandedPriceEstimate } from '../../api/product';
import { addCartItem, addWishlist, getWishlistStatus, removeWishlist, ShoppingAuthRequiredError } from '../../api/shopping';
import { loginHref } from '../../auth/authNavigation';
import { products, type Product } from '../../data';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

type ProductDetailPageProps = {
  productId: string;
};

type DetailTab = 'details' | 'shipping';

type DetailCopy = {
  maker: string;
  subtitle: string;
  optionLabel: string;
  options: string[];
  model: string;
  origin: string;
  material: string;
};

const detailCopy: Record<string, DetailCopy> = {
  'KREAM-489756': {
    maker: 'Polo Ralph Lauren',
    subtitle: '폴로 랄프 로렌 우먼 케이블 니트 코튼 쇼트 슬리브 스웨터 옐로우 - 25FW(KW5CTSSRLE2YL), 정품 검수 완료, 실시간 시세 확인',
    optionLabel: '상품 옵션',
    options: [],
    model: 'KW5CTSSRLE2YL',
    origin: '수집 데이터에 정보 없음',
    material: '코튼 니트 (상품명 기준)',
  },
  2: {
    maker: 'LUMENA',
    subtitle: '책상 위 어디서나 시원한 저소음 데스크 팬',
    optionLabel: '색상',
    options: ['클라우드 화이트', '딥 네이비'],
    model: 'FAN MINI 2',
    origin: '대한민국 디자인 / 중국 제조',
    material: 'ABS, 알루미늄',
  },
  3: {
    maker: 'LOCK&LOCK',
    subtitle: '하루 종일 차갑게 유지하는 대용량 텀블러',
    optionLabel: '색상',
    options: ['오프화이트', '세이지 그린'],
    model: '메트로 킹 텀블러 900ml',
    origin: '대한민국 디자인 / 중국 제조',
    material: '스테인리스 스틸, 폴리프로필렌',
  },
  4: {
    maker: 'MARDI MERCREDI',
    subtitle: '가볍고 통기성이 좋은 여름 데일리 캡',
    optionLabel: '색상',
    options: ['라이트 데님', '워시드 블랙'],
    model: 'SUMMER MESH CAP',
    origin: '대한민국',
    material: '면 100%',
  },
  5: {
    maker: 'SAPPUN',
    subtitle: '가볍게 높이를 더하는 편안한 스트랩 샌들',
    optionLabel: '사이즈',
    options: ['230', '235', '240', '245'],
    model: 'PLATFORM STRAP SANDAL',
    origin: '대한민국',
    material: '합성피혁, 합성고무',
  },
};

function formatPrice(price: number, currency: Product['currency']) {
  return `${currency === 'KRW' ? '₩' : '¥'}${price.toLocaleString()}`;
}

function displayProductInfo(value?: string) {
  const normalized = value?.trim();
  return !normalized || normalized === '수집 데이터에 정보 없음' ? '-' : normalized;
}

function ProductGallery({ product }: { product: Product }) {
  const [selected, setSelected] = useState(0);
  const galleryImages = product.images?.length ? product.images : [product.image];

  return (
    <section className="detail-gallery" aria-label="상품 이미지">
      <div className="detail-thumbnails" role="tablist" aria-label="상품 이미지 선택">
        {galleryImages.map((image, index) => (
          <button
            key={image}
            type="button"
            className={selected === index ? 'active' : ''}
            onClick={() => setSelected(index)}
            aria-label={`${index + 1}번 상품 이미지 보기`}
            aria-selected={selected === index}
            role="tab"
          >
            <img src={image} alt="" />
          </button>
        ))}
      </div>
      <div className="detail-main-image">
        <img src={galleryImages[selected]} alt={`${product.name} 이미지 ${selected + 1}`} />
        {galleryImages.length > 1 && (
          <>
            <button
              className="gallery-arrow gallery-arrow-prev"
              type="button"
              onClick={() => setSelected((index) => Math.max(0, index - 1))}
              disabled={selected === 0}
              aria-label="이전 상품 이미지"
            >
              <ChevronLeft size={28} />
            </button>
            <button
              className="gallery-arrow gallery-arrow-next"
              type="button"
              onClick={() => setSelected((index) => Math.min(galleryImages.length - 1, index + 1))}
              disabled={selected === galleryImages.length - 1}
              aria-label="다음 상품 이미지"
            >
              <ChevronRight size={28} />
            </button>
          </>
        )}
        <span aria-live="polite">{selected + 1} / {galleryImages.length}</span>
      </div>
    </section>
  );
}

function ProductDetailPage({ productId }: ProductDetailPageProps) {
  const localProduct = products.find((item) => (item.slug ?? String(item.id)) === productId || String(item.id) === productId);
  const [remoteProduct, setRemoteProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(!localProduct);
  const [loadFailed, setLoadFailed] = useState(false);
  const [quantity, setQuantity] = useState(1);
  const [selectedOptions, setSelectedOptions] = useState<Record<string, string>>({});
  const [liked, setLiked] = useState(false);
  const [savingWishlist, setSavingWishlist] = useState(false);
  const wishlistOperation = useRef(0);
  const [addingCart, setAddingCart] = useState(false);
  const [notice, setNotice] = useState('');
  const [activeTab, setActiveTab] = useState<DetailTab>('details');
  const [priceEstimate, setPriceEstimate] = useState<LandedPriceEstimate | null>(null);
  const [deliveryEstimate, setDeliveryEstimate] = useState<DeliveryEstimate | null>(null);

  const product = localProduct ?? remoteProduct;
  const detail = product
    ? detailCopy[String(product.id)] ?? {
      maker: product.brand || '브랜드 정보 없음',
      subtitle: product.description || product.name,
      optionLabel: '상품 옵션',
      options: [],
      model: String(product.id),
      origin: '수집 데이터에 정보 없음',
      material: '수집 데이터에 정보 없음',
    }
    : undefined;
  const productOptions = product?.options?.length
    ? product.options
    : detail?.options.length
      ? [{ optionType: detail.optionLabel, values: detail.options }]
      : [];
  const allOptionsSelected = productOptions.every((option) => Boolean(selectedOptions[option.optionType]));
  const selectedVariant = useMemo(() => {
    if (!product?.variants?.length || !allOptionsSelected) return undefined;

    return product.variants.find((variant) => (
      Object.entries(selectedOptions).every(
        ([optionType, value]) => variant.attributes[optionType] === value,
      )
    ));
  }, [allOptionsSelected, product?.variants, selectedOptions]);
  const hasVariantCatalog = Boolean(product?.variants?.some((variant) => (
    productOptions.every((option) => Object.hasOwn(variant.attributes, option.optionType))
  )));
  const variantUnavailable = hasVariantCatalog
    && allOptionsSelected
    && (!selectedVariant || !selectedVariant.available);
  const effectivePrice = selectedVariant?.price ?? product?.price ?? 0;

  useEffect(() => {
    if (localProduct) {
      setLoading(false);
      return undefined;
    }

    const controller = new AbortController();
    setLoading(true);
    setLoadFailed(false);
    setRemoteProduct(null);
    getProductDetail(productId, controller.signal)
      .then((value) => {
        setRemoteProduct(value);
        setLoadFailed(value === null);
      })
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === 'AbortError') return;
        setLoadFailed(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [localProduct, productId]);

  useEffect(() => {
    setSelectedOptions({});
  }, [productId]);

  useEffect(() => {
    if (!product) return undefined;
    const operation = wishlistOperation.current;
    let active = true;
    getWishlistStatus(String(product.id))
      .then((result) => {
        if (active && operation === wishlistOperation.current) setLiked(result.wished);
      })
      .catch(() => {
        if (active && operation === wishlistOperation.current) setLiked(false);
      });
    return () => { active = false; };
  }, [product]);
  const discount = useMemo(() => {
    if (!product?.originalPrice || effectivePrice >= product.originalPrice) return 0;
    return Math.round((1 - effectivePrice / product.originalPrice) * 100);
  }, [effectivePrice, product?.originalPrice]);
  const productUnitPrice = effectivePrice;
  const localDistributionFeeUnit = product?.domesticShippingFee ?? 3_000;
  const productSubtotal = productUnitPrice * quantity;
  const localDistributionFee = localDistributionFeeUnit;

  useEffect(() => {
    if (!product) return undefined;
    const controller = new AbortController();
    getLandedPriceEstimate({
      productName: product.name,
      category: product.category,
      unitPrice: effectivePrice,
      currency: product.currency ?? 'KRW',
      quantity,
      localDistributionFee,
    }, controller.signal).then(setPriceEstimate).catch((reason: unknown) => {
      if (!(reason instanceof DOMException && reason.name === 'AbortError')) setPriceEstimate(null);
    });
    return () => controller.abort();
  }, [effectivePrice, localDistributionFee, product, quantity]);

  useEffect(() => {
    if (!product) return undefined;
    const controller = new AbortController();
    setDeliveryEstimate(null);
    getDeliveryEstimate({
      productName: product.name,
      category: product.category,
      sourceUrl: product.sourceUrl,
    }, controller.signal).then(setDeliveryEstimate).catch((reason: unknown) => {
      if (!(reason instanceof DOMException && reason.name === 'AbortError')) setDeliveryEstimate(null);
    });
    return () => controller.abort();
  }, [product]);

  const displayedProductSubtotal = priceEstimate?.convertedProductPrice ?? productSubtotal;
  const displayedUnitPrice = Math.ceil(displayedProductSubtotal / quantity);
  const sourceCurrency: Product['currency'] = product?.currency ?? 'KRW';
  const displayedCurrency: Product['currency'] = priceEstimate ? 'JPY' : sourceCurrency;
  const displayedOriginalPrice = product?.originalPrice
    ? sourceCurrency === 'KRW'
      ? priceEstimate ? Math.ceil(product.originalPrice * priceEstimate.operatingExchangeRate) : null
      : product.originalPrice
    : null;

  if (loading) {
    return (
      <div className="app-shell">
        <SiteHeader />
        <main className="auth-loading" role="status" aria-live="polite">상품 정보를 불러오는 중입니다.</main>
        <SiteFooter />
      </div>
    );
  }

  if (!product || !detail || loadFailed) {
    return (
      <div className="app-shell">
        <SiteHeader />
        <main className="detail-not-found">
          <span>404</span>
          <h1>상품을 찾을 수 없습니다.</h1>
          <p>판매가 종료되었거나 주소가 변경된 상품입니다.</p>
          <a href="/">홈으로 돌아가기</a>
        </main>
        <SiteFooter />
      </div>
    );
  }

  const productSnapshot = {
    productId: String(product.id),
    productName: product.name,
    brand: product.brand || detail.maker,
    imageUrl: product.image,
    price: effectivePrice,
    currency: product.currency ?? 'KRW' as const,
    sourceUrl: product.sourceUrl,
  };

  const requireLogin = () => window.location.replace(loginHref(window.location.pathname));

  const toggleWishlist = async () => {
    if (savingWishlist) return;
    const shouldLike = !liked;
    wishlistOperation.current += 1;
    setSavingWishlist(true);
    setLiked(shouldLike);
    setNotice(shouldLike ? '찜 목록에 저장하는 중입니다.' : '찜 목록에서 삭제하는 중입니다.');
    try {
      if (!shouldLike) await removeWishlist(String(product.id));
      else await addWishlist(productSnapshot);
      setNotice(shouldLike ? '찜 목록에 저장했습니다.' : '찜 목록에서 삭제했습니다.');
    } catch (reason) {
      setLiked(!shouldLike);
      if (reason instanceof ShoppingAuthRequiredError) requireLogin();
      else setNotice(reason instanceof Error ? reason.message : '찜 상태를 변경하지 못했습니다.');
    } finally {
      setSavingWishlist(false);
    }
  };

  const handlePurchaseAction = async (kind: 'cart' | 'buy') => {
    if (!allOptionsSelected) {
      setNotice('모든 상품 옵션을 선택해 주세요.');
      return;
    }
    if (variantUnavailable) {
      setNotice('선택한 옵션 조합은 현재 구매할 수 없습니다.');
      return;
    }
    setAddingCart(true);
    try {
      const cartItem = await addCartItem({ ...productSnapshot, selectedOptions, quantity });
      if (kind === 'buy') {
        window.location.assign(`/checkout?items=${cartItem.id}`);
        return;
      }
      setNotice('장바구니에 상품을 담았습니다.');
    } catch (reason) {
      if (reason instanceof ShoppingAuthRequiredError) requireLogin();
      else setNotice(reason instanceof Error ? reason.message : '장바구니에 담지 못했습니다.');
    } finally {
      setAddingCart(false);
    }
  };

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="product-detail-main">
        <nav className="detail-breadcrumb header-inner" aria-label="현재 위치">
          <a href="/">홈</a><ChevronRight size={13} />
          <a href="/">{product.category.split(' > ')[0]}</a><ChevronRight size={13} />
          <span>{product.category.split(' > ').at(-1)}</span>
        </nav>

        <div className="product-overview header-inner">
          <ProductGallery product={product} />

          <section className="purchase-panel" aria-labelledby="product-title">
            <div className="product-heading-row">
              <div>
                <a
                  className="product-maker"
                  href={product.sourceUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  {detail.maker}<ChevronRight size={14} />
                </a>
                <h1 id="product-title">{product.name}</h1>
                <p>{detail.subtitle}</p>
              </div>
              <div className="product-heading-actions">
                <button type="button" onClick={() => void toggleWishlist()} disabled={savingWishlist} className={liked ? 'liked' : ''} aria-label={savingWishlist ? '찜 처리 중' : liked ? '찜 삭제' : '찜하기'} aria-pressed={liked} aria-busy={savingWishlist} title={savingWishlist ? '처리 중' : liked ? '찜 삭제' : '찜하기'}>
                  <Bookmark size={21} fill={liked ? 'currentColor' : 'none'} />
                </button>
              </div>
            </div>

            <div className="detail-price">
              {displayedOriginalPrice !== null && <p><del>{formatPrice(displayedOriginalPrice, 'JPY')}</del></p>}
              <div>
                {discount > 0 && <strong>{discount}%</strong>}
                <b>{sourceCurrency === 'KRW' && !priceEstimate ? '엔화 환산 중' : formatPrice(displayedUnitPrice, displayedCurrency)}</b>
                {sourceCurrency === 'KRW' && <small className="detail-source-price">({formatPrice(effectivePrice, 'KRW')})</small>}
              </div>
            </div>

            <div className="purchase-shipping">
              <div className="shipping-estimate">
                <span className="shipping-title"><Truck size={21} /><strong>배송</strong></span>
                <span className="shipping-range"><small>약</small><b>{deliveryEstimate ? `${deliveryEstimate.minimumDays}일 ~ ${deliveryEstimate.maximumDays}일` : '계산 중'}</b><small>예상</small></span>
              </div>
              <div className="shipping-breakdown">
                <strong>배송안내</strong>
                <ol className="shipping-flow" aria-label="배송 진행 단계">
                  {(deliveryEstimate?.stages ?? [
                    { code: 'SELLER_TO_KOREA', label: '판매처 → 한국 물류센터', minimumDays: 2, maximumDays: 4 },
                    { code: 'KOREA_TO_JAPAN', label: '한국 물류센터 → 일본 물류센터', minimumDays: 3, maximumDays: 6 },
                    { code: 'JAPAN_TO_CUSTOMER', label: '일본 물류센터 → 고객', minimumDays: 1, maximumDays: 3 },
                  ]).map((stage) => (
                    <li key={stage.code}><i aria-hidden="true" /><span>{stage.label}</span><b>{stage.minimumDays}~{stage.maximumDays}일</b></li>
                  ))}
                </ol>
                <small className="shipping-basis">{deliveryEstimate?.basis ?? '상품별 예상 배송 일정을 계산하고 있습니다.'}</small>
              </div>
            </div>

            {productOptions.length > 0 ? (
              productOptions.map((option) => (
                <label className="option-field" key={option.optionType}>
                  <span>{option.optionType} <b>*</b></span>
                  <div>
                    <select
                      value={selectedOptions[option.optionType] ?? ''}
                      onChange={(event) => {
                        setSelectedOptions((current) => ({ ...current, [option.optionType]: event.target.value }));
                        setNotice('');
                      }}
                    >
                      <option value="">{option.optionType}을 선택해 주세요</option>
                      {option.values.map((value) => {
                        const candidate = { ...selectedOptions, [option.optionType]: value };
                        const unavailable = hasVariantCatalog && !product?.variants?.some((variant) => (
                          variant.available
                          && Object.entries(candidate).every(
                            ([optionType, selectedValue]) => !selectedValue || variant.attributes[optionType] === selectedValue,
                          )
                        ));
                        return <option key={value} value={value} disabled={unavailable}>{value}{unavailable ? ' (품절)' : ''}</option>;
                      })}
                    </select>
                    <ChevronDown size={17} />
                  </div>
                </label>
              ))
            ) : (
              <div className="single-product-option"><Check size={16} /><span><strong>단일 상품</strong><small>수집 데이터에 별도 옵션 정보가 없습니다.</small></span></div>
            )}

            {(allOptionsSelected || productOptions.length === 0) && (
              <div className="selected-product">
                <span className="selected-product-copy">
                  <span className="selected-name-tooltip">
                    <strong tabIndex={0} aria-describedby="selected-product-full-name">{product.name}</strong>
                    <span id="selected-product-full-name" className="selected-name-popover" role="tooltip">
                      {product.name}
                    </span>
                  </span>
                  <small>{productOptions.length > 0 ? Object.values(selectedOptions).join(' / ') : '단일 상품'}</small>
                </span>
                <div className="quantity-control" aria-label="수량 선택">
                  <button type="button" onClick={() => setQuantity((value) => Math.max(1, value - 1))} aria-label="수량 줄이기"><Minus size={14} /></button>
                  <span>{quantity}</span>
                  <button type="button" onClick={() => setQuantity((value) => Math.min(10, value + 1))} aria-label="수량 늘리기"><Plus size={14} /></button>
                </div>
                <b>{formatPrice(displayedProductSubtotal, displayedCurrency)}</b>
              </div>
            )}

            {variantUnavailable && (
              <p className="purchase-notice" role="alert">선택한 옵션 조합은 현재 품절이거나 판매되지 않습니다.</p>
            )}

            <div className="purchase-total">
              <dl className="purchase-price-breakdown">
                <div>
                  <dt>한국 상품 가격</dt>
                  <dd>{formatPrice(productSubtotal, sourceCurrency)}</dd>
                </div>
                {priceEstimate && sourceCurrency === 'KRW' && <div>
                  <dt><i aria-hidden="true">↳</i> 운영환율 환산 상품가</dt>
                  <dd>{formatPrice(priceEstimate.convertedProductPrice, 'JPY')}</dd>
                </div>}
                <div>
                  <dt><i aria-hidden="true">+</i> 국내 배송비</dt>
                  <dd>{priceEstimate ? formatPrice(priceEstimate.convertedLocalDistributionFee, 'JPY') : formatPrice(localDistributionFee, sourceCurrency)}</dd>
                </div>
                {priceEstimate && <div>
                  <dt><i aria-hidden="true">+</i> 구매대행 수수료</dt>
                  <dd>{formatPrice(priceEstimate.agencyFee, 'JPY')}</dd>
                </div>}
                <div>
                  <dt><i aria-hidden="true">+</i> 예상 국제배송비 <small>(관부가세 미포함)</small></dt>
                  <dd className="is-unconfirmed">{priceEstimate
                    ? `${formatPrice(priceEstimate.estimatedInternationalShippingFee, 'JPY')} 결제 반영 (${formatPrice(priceEstimate.estimatedInternationalShippingMin, 'JPY')}~${formatPrice(priceEstimate.estimatedInternationalShippingMax, 'JPY')})`
                    : '계산 중'}</dd>
                </div>
              </dl>
              <small className="price-source-note">{product.domesticShippingFeeEstimated || product.domesticShippingFee === undefined ? '국내 배송비: 원본 페이지에서 확인하지 못해 기본 예상값 적용' : '국내 배송비: 원본 상품 페이지 정보 반영'}</small>
              <div className="purchase-total-heading">
                <span>지금 결제 예정</span>
                <strong>{formatPrice(priceEstimate?.payableNow ?? productSubtotal, priceEstimate ? 'JPY' : sourceCurrency)}</strong>
              </div>
              {priceEstimate && <div className="customs-estimate">
                <div className="customs-estimate-heading"><strong>통관 시 추가 예상</strong><span>{priceEstimate.customsStatus === 'EXEMPT_ESTIMATE' ? '면세 예상' : ['ESTIMATED', 'GENERAL_TARIFF_ESTIMATED'].includes(priceEstimate.customsStatus) ? '예상액' : '통관 시 확정'}</span></div>
                <dl>
                  <div><dt>예상 관세</dt><dd>{priceEstimate.estimatedDuty === null ? 'HS 코드 확인 필요' : formatPrice(priceEstimate.estimatedDuty, 'JPY')}</dd></div>
                  <div><dt>예상 일본 소비세</dt><dd>{priceEstimate.estimatedConsumptionTax === null ? '통관 시 확정' : formatPrice(priceEstimate.estimatedConsumptionTax, 'JPY')}</dd></div>
                  <div><dt>예상 관부가세 합계</dt><dd>{priceEstimate.estimatedImportCharges === null ? '통관 시 확정' : formatPrice(priceEstimate.estimatedImportCharges, 'JPY')}</dd></div>
                </dl>
                <small>{priceEstimate.estimatedImportCharges === null
                  ? '관부가세는 현재 결제 예정 금액에 포함되지 않았으며 통관 정보 확정 후 추가 정산됩니다.'
                  : '위 예상 관부가세는 현재 결제 예정 금액에 포함되어 있습니다.'}</small>
                {priceEstimate.estimatedTotalCostMin !== null && priceEstimate.estimatedTotalCostMax !== null && <p><span>배송비 포함 총 예상 비용</span><strong>{formatPrice(priceEstimate.estimatedTotalCostMin, 'JPY')} ~ {formatPrice(priceEstimate.estimatedTotalCostMax, 'JPY')}</strong></p>}
                <small>예상 중량 {(priceEstimate.estimatedWeightMinGrams / 1000).toFixed(1)}~{(priceEstimate.estimatedWeightMaxGrams / 1000).toFixed(1)}kg · {priceEstimate.internationalShippingStatus}</small>
                <small>{priceEstimate.shippingEstimationBasis}</small>
                <small>관세 품목 분류: {priceEstimate.customsClassificationMethod === 'AI_ASSISTED' ? 'AI 보조 분류' : '공식 기준 규칙 분류'}{priceEstimate.hsCodeCandidate ? ` · HS 코드 후보 ${priceEstimate.hsCodeCandidate}` : ''}</small>
                <small>세관 고시환율 {priceEstimate.customsExchangeRate || '-'}{priceEstimate.customsRateFrom && priceEstimate.customsRateTo ? ` · ${priceEstimate.customsRateFrom}~${priceEstimate.customsRateTo}` : ''}{priceEstimate.staleCustomsData ? ' · 마지막 저장 자료' : ''}</small>
                {priceEstimate.notices.map((item) => <small key={item}>{item}</small>)}
              </div>}
            </div>
            {notice && <p className="purchase-notice" role="status">{notice}</p>}
            <div className="purchase-actions">
              <button type="button" disabled={variantUnavailable || addingCart} onClick={() => handlePurchaseAction('cart')}><ShoppingBag size={20} />{addingCart ? '담는 중' : '장바구니'}</button>
              <button type="button" disabled={variantUnavailable || addingCart} onClick={() => handlePurchaseAction('buy')}>{addingCart ? '처리 중' : '바로 구매'}</button>
            </div>
          </section>
        </div>

        <nav className="detail-tabs" aria-label="상품 상세 메뉴">
          <div className="header-inner" role="tablist" aria-label="상품 상세 정보">
            <button
              id="detail-tab-details"
              type="button"
              role="tab"
              aria-selected={activeTab === 'details'}
              aria-controls="detail-tab-panel"
              className={activeTab === 'details' ? 'active' : ''}
              onClick={() => setActiveTab('details')}
            >
              상품상세
            </button>
            <button
              id="detail-tab-shipping"
              type="button"
              role="tab"
              aria-selected={activeTab === 'shipping'}
              aria-controls="detail-tab-panel"
              className={activeTab === 'shipping' ? 'active' : ''}
              onClick={() => setActiveTab('shipping')}
            >
              배송·교환·반품 안내
            </button>
          </div>
        </nav>

        <div
          id="detail-tab-panel"
          key={activeTab}
          className="detail-body header-inner"
          role="tabpanel"
          aria-labelledby={`detail-tab-${activeTab}`}
          tabIndex={0}
        >
          {activeTab === 'details' && (
            <>
              <section className="detail-section product-story">
                <h2>{product.name}</h2>
                <p>{product.description || detail.subtitle}</p>
                <div className="story-image"><img src={product.images?.[1] || product.image} alt={`${product.name} 상세 이미지`} /></div>
              </section>
              <section className="detail-section info-section">
                <div className="detail-section-heading"><h2>상품정보</h2><p>구매 전 상품 정보를 확인해 주세요.</p></div>
                <dl className="product-spec-table">
                  <div><dt>상품명 / 모델명</dt><dd>{displayProductInfo(detail.model)}</dd><dt>브랜드</dt><dd>{displayProductInfo(detail.maker)}</dd></div>
                  <div><dt>제조국</dt><dd>{displayProductInfo(detail.origin)}</dd><dt>소재 / 주요 성분</dt><dd>{displayProductInfo(detail.material)}</dd></div>
                  <div><dt>품질보증기준</dt><dd>관련 법 및 소비자분쟁해결 기준에 따름</dd><dt>판매자</dt><dd>ZIK:00</dd></div>
                </dl>
                {product.sourceUrl && (
                  <a
                    className="source-page-link"
                    href={product.sourceUrl}
                    target="_blank"
                    rel="noreferrer"
                  >
                    판매처 원본 페이지 보기 <ExternalLink size={17} />
                  </a>
                )}
              </section>
            </>
          )}

          {activeTab === 'shipping' && (
            <section className="detail-section shipping-section">
              <div className="detail-section-heading"><h2>배송·교환·반품 안내</h2></div>
              <div className="policy-grid">
                <article><Truck size={22} /><div><h3>배송 안내</h3><p>결제 완료 후 1–2영업일 내 출고되며, 일본 현지까지 평균 5–8일이 소요됩니다. 통관 및 현지 사정에 따라 지연될 수 있습니다.</p></div></article>
                <article><RotateCcw size={22} /><div><h3>교환·반품 안내</h3><p>상품 수령 후 7일 이내 신청할 수 있습니다. 단순 변심은 왕복 배송비가 발생하며, 사용 또는 훼손된 상품은 반품이 제한됩니다.</p></div></article>
              </div>
            </section>
          )}
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

export default ProductDetailPage;
