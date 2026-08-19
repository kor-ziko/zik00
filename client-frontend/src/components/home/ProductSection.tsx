import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import Bookmark from 'lucide-react/dist/esm/icons/bookmark.js';
import { useEffect, useState } from 'react';
import { addWishlist, getWishlist, removeWishlist, ShoppingAuthRequiredError } from '../../api/shopping';
import { useAuthMemory } from '../../auth/AuthMemory';
import { currentRelativeUrl, loginHref } from '../../auth/authNavigation';
import { products } from '../../data';
import { useOperatingExchangeRate } from '../../hooks/useOperatingExchangeRate';
import { useLocale } from '../../locale';

function ProductSection() {
  const { copy } = useLocale();
  const { accessSessionActive } = useAuthMemory();
  const { toJpy } = useOperatingExchangeRate();
  const [favoriteIds, setFavoriteIds] = useState<Set<number | string>>(() => new Set());
  const [savingFavoriteIds, setSavingFavoriteIds] = useState<Set<string>>(() => new Set());

  useEffect(() => {
    if (!accessSessionActive) {
      setFavoriteIds(new Set());
      return;
    }
    getWishlist()
      .then((items) => setFavoriteIds(new Set(items.map((item) => item.productId))))
      .catch(() => setFavoriteIds(new Set()));
  }, [accessSessionActive]);

  const toggleFavorite = async (product: (typeof products)[number]) => {
    if (!accessSessionActive) {
      window.location.assign(loginHref(currentRelativeUrl()));
      return;
    }
    const productId = String(product.id);
    if (savingFavoriteIds.has(productId)) return;
    const shouldSave = !favoriteIds.has(productId);
    setSavingFavoriteIds((current) => new Set(current).add(productId));
    setFavoriteIds((current) => {
      const next = new Set(current);
      if (shouldSave) next.add(productId);
      else next.delete(productId);
      return next;
    });
    try {
      if (shouldSave) {
        await addWishlist({
          productId,
          productName: product.name,
          brand: product.brand,
          imageUrl: product.image,
          price: product.price,
          currency: product.currency ?? 'KRW',
          sourceUrl: product.sourceUrl,
        });
      } else {
        await removeWishlist(productId);
      }
    } catch (reason) {
      setFavoriteIds((current) => {
        const next = new Set(current);
        if (shouldSave) next.delete(productId);
        else next.add(productId);
        return next;
      });
      if (reason instanceof ShoppingAuthRequiredError) window.location.assign(loginHref(currentRelativeUrl()));
    } finally {
      setSavingFavoriteIds((current) => {
        const next = new Set(current);
        next.delete(productId);
        return next;
      });
    }
  };

  return (
    <section className="recommendations" id="recommendations">
      <span className="section-anchor" id="reviews" aria-hidden="true" />
      <span className="section-anchor" id="wishlist" aria-hidden="true" />
      <div className="section-heading">
        <div>
          <span>{copy.products.eyebrow}</span>
          <h2>{copy.products.title}</h2>
        </div>
        <a href="/search">{copy.products.all} <ChevronRight size={16} /></a>
      </div>

      <div className="product-grid">
        {products.map((product) => {
          const sourceCurrency = product.currency ?? 'KRW';
          const priceJpy = toJpy(product.price, sourceCurrency);
          const originalPriceJpy = product.originalPrice
            ? toJpy(product.originalPrice, sourceCurrency)
            : null;
          return <article className="product-card" key={product.id}>
            <div className="product-image-wrap">
              <a className="product-image-link" href={`/products/${product.slug ?? product.id}`}>
                <img
                  className={product.currency === 'KRW' ? 'kream-product-image' : undefined}
                  src={product.image}
                  alt={product.name}
                  loading="lazy"
                  decoding="async"
                />
              </a>
              {product.badge && <span>{product.badge}</span>}
              <button
                className={favoriteIds.has(String(product.id)) ? 'is-favorite' : ''}
                type="button"
                aria-label={`${product.name} ${copy.products.favorite}`}
                aria-pressed={favoriteIds.has(String(product.id))}
                disabled={savingFavoriteIds.has(String(product.id))}
                onClick={() => void toggleFavorite(product)}
              >
                <Bookmark size={19} fill={favoriteIds.has(String(product.id)) ? 'currentColor' : 'none'} />
              </button>
            </div>

            <a className="product-info" href={`/products/${product.slug ?? product.id}`}>
              <p className="product-category">{product.category}</p>
              <h3>{product.name}</h3>
              <div className="price-row">
                <strong>{priceJpy === null ? '엔화 환산 중' : `¥${priceJpy.toLocaleString('ja-JP')}`}</strong>
                {sourceCurrency === 'KRW' && <small className="product-source-price">(₩{product.price.toLocaleString('ko-KR')})</small>}
                {originalPriceJpy !== null && <del>¥{originalPriceJpy.toLocaleString('ja-JP')}</del>}
              </div>
              <p className="shipping-note">예상 국제배송비는 결제 시 반영</p>
            </a>
          </article>;
        })}
      </div>
    </section>
  );
}

export default ProductSection;
