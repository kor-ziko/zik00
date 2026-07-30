import Check from 'lucide-react/dist/esm/icons/check.js';
import ChevronDown from 'lucide-react/dist/esm/icons/chevron-down.js';
import ChevronLeft from 'lucide-react/dist/esm/icons/chevron-left.js';
import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import ExternalLink from 'lucide-react/dist/esm/icons/external-link.js';
import Heart from 'lucide-react/dist/esm/icons/heart.js';
import Minus from 'lucide-react/dist/esm/icons/minus.js';
import Plus from 'lucide-react/dist/esm/icons/plus.js';
import RotateCcw from 'lucide-react/dist/esm/icons/rotate-ccw.js';
import Share2 from 'lucide-react/dist/esm/icons/share-2.js';
import ShoppingBag from 'lucide-react/dist/esm/icons/shopping-bag.js';
import Star from 'lucide-react/dist/esm/icons/star.js';
import Truck from 'lucide-react/dist/esm/icons/truck.js';
import { useMemo, useState } from 'react';
import { products, type Product } from '../../data';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

type ProductDetailPageProps = {
  productId: string;
};

type DetailTab = 'details' | 'reviews' | 'shipping';

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
  const product = products.find((item) => (item.slug ?? String(item.id)) === productId || String(item.id) === productId);
  const [quantity, setQuantity] = useState(1);
  const [selectedOption, setSelectedOption] = useState('');
  const [liked, setLiked] = useState(false);
  const [notice, setNotice] = useState('');
  const [activeTab, setActiveTab] = useState<DetailTab>('details');

  const detail = product ? detailCopy[String(product.id)] : undefined;
  const discount = useMemo(() => {
    if (!product?.originalPrice) return 0;
    return Math.round((1 - product.price / product.originalPrice) * 100);
  }, [product]);
  const productUnitPrice = product?.price ?? 0;
  const localDistributionFeeUnit = product?.id === 'KREAM-489756' ? 18_200 : 0;
  const productSubtotal = productUnitPrice * quantity;
  const localDistributionFee = localDistributionFeeUnit;
  const purchaseTotal = productSubtotal + localDistributionFee;
  const reviews = product?.reviews ?? [];
  const reviewCount = reviews.length || product?.reviewCount || 0;

  if (!product || !detail) {
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

  const handlePurchaseAction = (kind: 'cart' | 'buy') => {
    if (detail.options.length > 0 && !selectedOption) {
      setNotice(`${detail.optionLabel}을 선택해 주세요.`);
      return;
    }
    setNotice(kind === 'cart' ? '장바구니에 상품을 담았습니다.' : '주문 화면은 준비 중입니다.');
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
                <button type="button" onClick={() => setLiked((value) => !value)} className={liked ? 'liked' : ''} aria-label="찜하기">
                  <Heart size={21} fill={liked ? 'currentColor' : 'none'} />
                </button>
                <button type="button" onClick={() => setNotice('상품 링크를 복사했습니다.')} aria-label="공유하기"><Share2 size={21} /></button>
              </div>
            </div>

            <div className="detail-price">
              {product.originalPrice && <p><del>{formatPrice(product.originalPrice, product.currency)}</del></p>}
              <div>{discount > 0 && <strong>{discount}%</strong>}<b>{formatPrice(product.price, product.currency)}</b></div>
            </div>

            <div className="purchase-shipping">
              <div className="shipping-estimate">
                <span className="shipping-title"><Truck size={21} /><strong>배송</strong></span>
                <span className="shipping-range"><small>약</small><b>12일 ~ 19일</b><small>예상</small></span>
              </div>
              <div className="shipping-breakdown">
                <strong>배송안내</strong>
                <ol className="shipping-flow" aria-label="배송 진행 단계">
                  <li><i aria-hidden="true" /><span>판매처 발송</span><b>4~7일</b></li>
                  <li><i aria-hidden="true" /><span>발송 후 현지 이동</span><b>2~4일</b></li>
                  <li><i aria-hidden="true" /><span>국제배송 및 수령</span><b>6~8일</b></li>
                </ol>
              </div>
            </div>

            {detail.options.length > 0 ? (
              <label className="option-field">
                <span>{detail.optionLabel} <b>*</b></span>
                <div>
                  <select value={selectedOption} onChange={(event) => { setSelectedOption(event.target.value); setNotice(''); }}>
                    <option value="">{detail.optionLabel}을 선택해 주세요</option>
                    {detail.options.map((option) => <option key={option} value={option}>{option}</option>)}
                  </select>
                  <ChevronDown size={17} />
                </div>
              </label>
            ) : (
              <div className="single-product-option"><Check size={16} /><span><strong>단일 상품</strong><small>수집 데이터에 별도 옵션 정보가 없습니다.</small></span></div>
            )}

            {(selectedOption || detail.options.length === 0) && (
              <div className="selected-product">
                <span className="selected-product-copy">
                  <span className="selected-name-tooltip">
                    <strong tabIndex={0} aria-describedby="selected-product-full-name">{product.name}</strong>
                    <span id="selected-product-full-name" className="selected-name-popover" role="tooltip">
                      {product.name}
                    </span>
                  </span>
                  <small>{selectedOption || '단일 상품'}</small>
                </span>
                <div className="quantity-control" aria-label="수량 선택">
                  <button type="button" onClick={() => setQuantity((value) => Math.max(1, value - 1))} aria-label="수량 줄이기"><Minus size={14} /></button>
                  <span>{quantity}</span>
                  <button type="button" onClick={() => setQuantity((value) => Math.min(10, value + 1))} aria-label="수량 늘리기"><Plus size={14} /></button>
                </div>
                <b>{formatPrice(productSubtotal, product.currency)}</b>
              </div>
            )}

            <div className="purchase-total">
              <dl className="purchase-price-breakdown">
                <div>
                  <dt>상품 가격</dt>
                  <dd>{formatPrice(productSubtotal, product.currency)}</dd>
                </div>
                <div>
                  <dt><i aria-hidden="true">+</i> 현지 유통비</dt>
                  <dd>{formatPrice(localDistributionFee, product.currency)}</dd>
                </div>
              </dl>
              <div className="purchase-total-heading">
                <span>총 상품 금액</span>
                <strong>{formatPrice(purchaseTotal, product.currency)}</strong>
              </div>
            </div>
            {notice && <p className="purchase-notice" role="status">{notice}</p>}
            <div className="purchase-actions">
              <button type="button" onClick={() => handlePurchaseAction('cart')}><ShoppingBag size={20} />장바구니</button>
              <button type="button" onClick={() => handlePurchaseAction('buy')}>바로 구매</button>
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
              id="detail-tab-reviews"
              type="button"
              role="tab"
              aria-selected={activeTab === 'reviews'}
              aria-controls="detail-tab-panel"
              className={activeTab === 'reviews' ? 'active' : ''}
              onClick={() => setActiveTab('reviews')}
            >
              리뷰 <span>{reviewCount}</span>
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

          {activeTab === 'reviews' && (
            <section className="detail-section review-section">
              <div className="detail-section-heading">
                <h2>상품 리뷰 <span>{reviewCount}</span></h2>
                <p>KREAM 스타일 리뷰</p>
              </div>
              {reviews.length > 0 ? (
                <div className="review-list">
                  {reviews.map((review) => (
                    <article className="review-card" key={review.reviewId}>
                      <header>
                        <strong>{review.author || 'KREAM 사용자'}</strong>
                        {review.rating != null && (
                          <span className="review-rating" aria-label={`평점 ${review.rating}점`}>
                            <Star size={14} fill="currentColor" /> {review.rating}
                          </span>
                        )}
                        {review.createdAt && <time>{review.createdAt}</time>}
                      </header>
                      {review.images && review.images.length > 0 && (
                        <div className="review-images">
                          {review.images.map((image, imageIndex) => (
                            <img
                              key={`${review.reviewId}-${imageIndex}`}
                              src={image}
                              alt={`${review.author || '사용자'} 리뷰 이미지 ${imageIndex + 1}`}
                              loading="lazy"
                            />
                          ))}
                        </div>
                      )}
                      {review.content && <p>{review.content}</p>}
                      <footer>
                        <span><Heart size={15} /> 좋아요 {review.likeCount ?? 0}</span>
                        {review.reviewUrl && (
                          <a href={review.reviewUrl} target="_blank" rel="noreferrer">
                            원문 보기 <ExternalLink size={14} />
                          </a>
                        )}
                      </footer>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="review-empty">
                  <Star size={28} />
                  <strong>등록된 상품 리뷰가 없습니다.</strong>
                </div>
              )}
            </section>
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
