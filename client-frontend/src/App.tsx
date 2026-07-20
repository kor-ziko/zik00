import { lazy, Suspense, useEffect, useState } from 'react';
import HeroCarousel from './components/home/HeroCarousel';
import ProductSection from './components/home/ProductSection';
import ServiceStrip from './components/home/ServiceStrip';
import QuickMenu from './components/layout/QuickMenu';
import SiteFooter from './components/layout/SiteFooter';
import SiteHeader from './components/layout/SiteHeader';

const LoginPage = lazy(() => import('./components/auth/LoginPage'));
const RegistrationDetailPage = lazy(() => import('./components/auth/RegistrationDetailPage'));
const RegistrationTermsPage = lazy(() => import('./components/auth/RegistrationTermsPage'));
const MypagePage = lazy(() => import('./components/mypage/MypagePage'));
const OAuthCallbackPage = lazy(() => import('./components/auth/OAuthCallbackPage'));

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
  if (path === '/login') return <Suspense fallback={<PageLoader />}><LoginPage /></Suspense>;
  if (path === '/login/terms') return <Suspense fallback={<PageLoader />}><RegistrationTermsPage /></Suspense>;
  if (path === '/login/detail') return <Suspense fallback={<PageLoader />}><RegistrationDetailPage /></Suspense>;
  if (path === '/oauth/callback') return <Suspense fallback={<PageLoader />}><OAuthCallbackPage /></Suspense>;
  if (path === '/mypage' || path.startsWith('/mypage/')) {
    return <Suspense fallback={<PageLoader />}><MypagePage /></Suspense>;
  }

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
