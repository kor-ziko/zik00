import { lazy, Suspense, useEffect, useState } from 'react';
import HeroCarousel from './components/home/HeroCarousel';
import ProductSection from './components/home/ProductSection';
import ServiceStrip from './components/home/ServiceStrip';
import HomePopup from './components/home/HomePopup';
import RecommendedSites from './components/home/RecommendedSites';
import QuickMenu from './components/layout/QuickMenu';
import SiteFooter from './components/layout/SiteFooter';
import SiteHeader from './components/layout/SiteHeader';
import ErrorPage from './components/error/ErrorPage';
import kreamFeaturedProduct from './generated/kreamFeaturedProduct.json';

const LoginPage = lazy(() => import('./components/auth/LoginPage'));
const RegistrationDetailPage = lazy(() => import('./components/auth/RegistrationDetailPage'));
const RegistrationTermsPage = lazy(() => import('./components/auth/RegistrationTermsPage'));
const MypagePage = lazy(() => import('./components/mypage/MypagePage'));
const OAuthCallbackPage = lazy(() => import('./components/auth/OAuthCallbackPage'));
const ProductDetailPage = lazy(() => import('./components/product/ProductDetailPage'));
const SearchResultsPage = lazy(() => import('./components/search/SearchResultsPage'));
const ServiceIntroPage = lazy(() => import('./components/service_intro/ServiceIntroPage'));
const NoticePage = lazy(() => import('./components/notice/NoticePage'));
const ReviewPage = lazy(() => import('./components/review/ReviewPage'));
const WishlistPage = lazy(() => import('./components/wishlist/WishlistPage'));
const CartPage = lazy(() => import('./components/cart/CartPage'));
const PrecautionPage = lazy(() => import('./components/precaution/PrecautionPage'));
const CheckoutPage = lazy(() => import('./components/payment/CheckoutPage'));
const PaymentCompletePage = lazy(() => import('./components/payment/PaymentCompletePage'));

const mypagePaths = new Set([
  '/mypage',
  '/mypage/home',
  '/mypage/orders',
  '/mypage/deliveries',
  '/mypage/inquiries',
  '/mypage/coupons',
  '/mypage/deposits',
  '/mypage/profile',
]);
const featuredProductPath = `/products/${kreamFeaturedProduct.slug}`;
const legacyFeaturedProductPaths = new Set([
  '/products/KREAM-489756',
  '/products/b70e8ae9',
]);

function PageLoader() {
  return <div className="auth-loading" role="status" aria-live="polite">Loading...</div>;
}

function App() {
  const [path, setPath] = useState(() => window.location.pathname.replace(/\/+$/, '') || '/');

  useEffect(() => {
    const updatePath = () => setPath(window.location.pathname.replace(/\/+$/, '') || '/');
    window.addEventListener('popstate', updatePath);
    return () => window.removeEventListener('popstate', updatePath);
  }, []);

  useEffect(() => {
    if (legacyFeaturedProductPaths.has(path)) {
      window.history.replaceState({}, '', featuredProductPath);
      setPath(featuredProductPath);
    }
  }, [path]);
  if (path === '/login') return <Suspense fallback={<PageLoader />}><LoginPage /></Suspense>;
  if (path === '/login/terms') return <Suspense fallback={<PageLoader />}><RegistrationTermsPage /></Suspense>;
  if (path === '/login/detail') return <Suspense fallback={<PageLoader />}><RegistrationDetailPage /></Suspense>;
  if (path === '/oauth/callback') return <Suspense fallback={<PageLoader />}><OAuthCallbackPage /></Suspense>;
  if (mypagePaths.has(path)) {
    return <Suspense fallback={<PageLoader />}><MypagePage /></Suspense>;
  }
  const productMatch = path.match(/^\/products\/([A-Za-z0-9_-]+)$/);
  if (productMatch) {
    return <Suspense fallback={<PageLoader />}><ProductDetailPage productId={productMatch[1]} /></Suspense>;
  }
  if (path === '/search') {
    return <Suspense fallback={<PageLoader />}><SearchResultsPage /></Suspense>;
  }
  if (path === '/service-intro') {
    return <Suspense fallback={<PageLoader />}><ServiceIntroPage /></Suspense>;
  }
  if (path === '/reviews') {
    return <Suspense fallback={<PageLoader />}><ReviewPage /></Suspense>;
  }
  if (path === '/wishlist') {
    return <Suspense fallback={<PageLoader />}><WishlistPage /></Suspense>;
  }
  if (path === '/cart') {
    return <Suspense fallback={<PageLoader />}><CartPage /></Suspense>;
  }
  if (path === '/checkout') {
    return <Suspense fallback={<PageLoader />}><CheckoutPage /></Suspense>;
  }
  if (path === '/checkout/complete') {
    return <Suspense fallback={<PageLoader />}><PaymentCompletePage /></Suspense>;
  }
  if (path === '/precautions') {
    return <Suspense fallback={<PageLoader />}><PrecautionPage /></Suspense>;
  }
  const noticeMatch = path.match(/^\/notices\/(\d+)$/);
  if (path === '/notices' || noticeMatch) {
    return <Suspense fallback={<PageLoader />}><NoticePage noticeId={noticeMatch ? Number(noticeMatch[1]) : undefined} /></Suspense>;
  }
  if (path === '/error') return <ErrorPage />;
  if (path !== '/') return <ErrorPage status={404} />;

  return (
    <div className="app-shell">
      <SiteHeader />

      <main id="top">
        <HeroCarousel />

        <div className="content-layout header-inner">
          <div className="main-content">
            <ServiceStrip />
            <ProductSection />
            <RecommendedSites />
          </div>
          <QuickMenu />
        </div>
      </main>

      <SiteFooter />
      <HomePopup />
    </div>
  );
}

export default App;
