import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import Heart from 'lucide-react/dist/esm/icons/heart.js';
import { useState } from 'react';
import { useAuthMemory } from '../../auth/AuthMemory';
import { currentRelativeUrl, loginHref } from '../../auth/authNavigation';
import { products } from '../../data';
import { useLocale } from '../../locale';

function ProductSection() {
  const { copy } = useLocale();
  const { accessSessionActive } = useAuthMemory();
  const [favoriteIds, setFavoriteIds] = useState<Set<number | string>>(() => new Set());

  const toggleFavorite = (productId: number | string) => {
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
        {products.map((product) => (
          <article className="product-card" key={product.id}>
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
                className={favoriteIds.has(product.id) ? 'is-favorite' : ''}
                type="button"
                aria-label={`${product.name} ${copy.products.favorite}`}
                aria-pressed={favoriteIds.has(product.id)}
                onClick={() => toggleFavorite(product.id)}
              >
                <Heart size={19} fill={favoriteIds.has(product.id) ? 'currentColor' : 'none'} />
              </button>
            </div>

            <a className="product-info" href={`/products/${product.slug ?? product.id}`}>
              <p className="product-category">{product.category}</p>
              <h3>{product.name}</h3>
              <div className="price-row">
                <strong>{product.currency === 'KRW' ? '₩' : '¥'}{product.price.toLocaleString()}</strong>
                {product.originalPrice && <del>{product.currency === 'KRW' ? '₩' : '¥'}{product.originalPrice.toLocaleString()}</del>}
              </div>
              <p className="shipping-note">{copy.products.shipping}</p>
            </a>
          </article>
        ))}
      </div>
    </section>
  );
}

export default ProductSection;
