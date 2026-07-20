import {
  ChevronDown,
  Globe2,
  Heart,
  Menu,
  ShoppingBag,
  Truck,
  UserRound,
} from 'lucide-react';
import SearchBox from '../search/SearchBox';

const navigationItems = [
  ['베스트', '#best'],
  ['신상품', '#new'],
  ['K-뷰티', '#beauty'],
  ['패션', '#fashion'],
  ['식품', '#food'],
  ['리빙', '#living'],
  ['디지털', '#digital'],
  ['기획전', '#events'],
];

function Brand() {
  return (
    <a className="brand" href="#top" aria-label="ZIK:00 홈">
      <span>ZIK</span><strong>:00</strong>
    </a>
  );
}

function SiteHeader() {
  return (
    <header className="site-header">
      <div className="utility-bar">
        <div className="header-inner utility-inner">
          <span>한국 상품을 일본까지, 쉽고 빠르게</span>
          <nav aria-label="회원 메뉴">
            <a href="/login">로그인</a>
            <a href="/login">회원가입</a>
            <a href="#support">고객센터</a>
            <button className="language-button" type="button">
              <Globe2 size={14} aria-hidden="true" /> KO
              <ChevronDown size={13} aria-hidden="true" />
            </button>
          </nav>
        </div>
      </div>

      <div className="header-inner main-header-row">
        <button className="category-button" type="button" aria-label="카테고리 열기">
          <Menu size={24} aria-hidden="true" />
          <span>카테고리</span>
        </button>

        <Brand />
        <SearchBox />

        <nav className="primary-actions" aria-label="주요 메뉴">
          <a href="#mypage"><UserRound size={25} /><span>마이페이지</span></a>
          <a href="#wishlist"><Heart size={25} /><span>찜</span></a>
          <a href="#cart" className="cart-link">
            <ShoppingBag size={25} />
            <span className="cart-count">0</span>
            <span>장바구니</span>
          </a>
        </nav>

        <a className="agency-cta" href="#agency">
          <Truck size={27} aria-hidden="true" />
          <span><small>찾는 상품이 없다면</small>배송대행 신청</span>
        </a>
      </div>

      <nav className="category-nav header-inner" aria-label="추천 카테고리">
        {navigationItems.map(([label, href]) => (
          <a key={label} href={href}>{label}</a>
        ))}
      </nav>
    </header>
  );
}

export { Brand };
export default SiteHeader;
