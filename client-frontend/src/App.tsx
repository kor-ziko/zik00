import { lazy, Suspense, useEffect, useState } from 'react';
import HeroCarousel from './components/home/HeroCarousel';
import ProductSection from './components/home/ProductSection';
import ServiceStrip from './components/home/ServiceStrip';
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
          </div>
          <QuickMenu />
        </div>
      </main>

      <SiteFooter />
    </div>
  );
}

export default App;
