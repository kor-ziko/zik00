import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import Heart from 'lucide-react/dist/esm/icons/heart.js';
import { products } from '../../data';

function ProductSection() {
  return (
    <section className="recommendations" id="recommendations">
      <div className="section-heading">
        <div>
          <span>SEOUL SUMMER PICKS</span>
          <h2>이번 여름 한국에서 뜨는 상품</h2>
        </div>
        <a href="#all-products">전체 보기 <ChevronRight size={16} /></a>
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
              <button type="button" aria-label={`${product.name} 찜하기`}>
                <Heart size={19} />
              </button>
            </div>

            <a className="product-info" href={`/products/${product.slug ?? product.id}`}>
              <p className="product-category">{product.category}</p>
              <h3>{product.name}</h3>
              <div className="price-row">
                <strong>{product.currency === 'KRW' ? '₩' : '¥'}{product.price.toLocaleString()}</strong>
                {product.originalPrice && <del>{product.currency === 'KRW' ? '₩' : '¥'}{product.originalPrice.toLocaleString()}</del>}
              </div>
              <p className="shipping-note">예상 국제배송비 별도</p>
            </a>
          </article>
        ))}
      </div>
    </section>
  );
}

export default ProductSection;
