import HeroCarousel from './components/home/HeroCarousel';
import ProductSection from './components/home/ProductSection';
import ServiceStrip from './components/home/ServiceStrip';
import QuickMenu from './components/layout/QuickMenu';
import SiteFooter from './components/layout/SiteFooter';
import SiteHeader from './components/layout/SiteHeader';

function App() {
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
