import CheckCircle2 from 'lucide-react/dist/esm/icons/circle-check-big.js';
import ClipboardCheck from 'lucide-react/dist/esm/icons/clipboard-check.js';
import MessagesSquare from 'lucide-react/dist/esm/icons/messages-square.js';
import PackageCheck from 'lucide-react/dist/esm/icons/package-check.js';
import Search from 'lucide-react/dist/esm/icons/search.js';
import ShieldCheck from 'lucide-react/dist/esm/icons/shield-check.js';
import ShoppingBag from 'lucide-react/dist/esm/icons/shopping-bag.js';
import { useEffect, useMemo, useState } from 'react';
import { getServiceIntro, type ServiceIntroResponse } from '../../api/serviceIntro';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

const processIcons = [Search, ClipboardCheck, PackageCheck];
const valueIcons = [ShieldCheck, ShoppingBag, MessagesSquare];

function ServiceIntroPage() {
  const [data, setData] = useState<ServiceIntroResponse | null>(null);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setError('');
    getServiceIntro(controller.signal)
      .then(setData)
      .catch((reason: unknown) => {
        if ((reason as Error).name !== 'AbortError') {
          setError(reason instanceof Error ? reason.message : '서비스 소개를 불러오지 못했습니다.');
        }
      });
    return () => controller.abort();
  }, [reloadKey]);

  const hero = data?.sections.find((section) => section.sectionType === 'HERO');
  const process = useMemo(() => data?.sections.filter((section) => section.sectionType === 'PROCESS') ?? [], [data]);
  const values = useMemo(() => data?.sections.filter((section) => section.sectionType === 'VALUE') ?? [], [data]);

  return (
    <div className="app-shell information-page-shell">
      <SiteHeader />
      <main className="service-intro-page">
        {error ? (
          <section className="information-state header-inner">
            <h1>서비스 소개를 불러오지 못했습니다.</h1>
            <p>{error}</p>
            <button type="button" onClick={() => setReloadKey((key) => key + 1)}>다시 시도</button>
          </section>
        ) : !data ? (
          <div className="information-loading" role="status">서비스 소개를 불러오는 중입니다.</div>
        ) : data.sections.length === 0 ? (
          <section className="information-state header-inner">
            <h1>서비스 소개를 준비하고 있습니다.</h1>
            <p>새로운 안내 내용이 등록되면 이곳에서 확인할 수 있습니다.</p>
          </section>
        ) : (
          <>
            <section className="service-intro-hero">
              <img src={hero?.imageUrl || '/assets/hero-seoul-summer.webp'} alt="서울의 쇼핑 거리" />
              <div className="service-intro-hero-overlay" />
              <div className="header-inner service-intro-hero-copy">
                <p>{hero?.eyebrow || 'ZIK:00 SERVICE'}</p>
                <h1>{hero?.title || '한국 쇼핑을 더 가까이'}</h1>
                <span>{hero?.content}</span>
                {hero?.detail && <small><CheckCircle2 size={17} />{hero.detail}</small>}
              </div>
            </section>

            <section className="service-process-section">
              <div className="header-inner">
                <header className="information-section-heading"><p>HOW IT WORKS</p><h2>주문은 이렇게 진행됩니다</h2></header>
                <div className="service-process-list">
                  {process.map((section, index) => {
                    const Icon = processIcons[index % processIcons.length];
                    return (
                      <article key={section.id}>
                        <div className="service-step-index"><Icon size={23} /><span>{section.eyebrow}</span></div>
                        <h3>{section.title}</h3><p>{section.content}</p>
                        {section.detail && <small>{section.detail}</small>}
                      </article>
                    );
                  })}
                </div>
              </div>
            </section>

            <section className="service-values-section header-inner">
              <header className="information-section-heading"><p>OUR STANDARD</p><h2>ZIK:00이 중요하게 생각하는 것</h2></header>
              <div className="service-value-list">
                {values.map((section, index) => {
                  const Icon = valueIcons[index % valueIcons.length];
                  return (
                    <article key={section.id}>
                      <Icon size={25} aria-hidden="true" />
                      <div><h3>{section.title}</h3><p>{section.content}</p>{section.detail && <small>{section.detail}</small>}</div>
                    </article>
                  );
                })}
              </div>
            </section>
          </>
        )}
      </main>
      <SiteFooter />
    </div>
  );
}

export default ServiceIntroPage;
