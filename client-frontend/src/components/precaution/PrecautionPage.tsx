import CheckCircle2 from 'lucide-react/dist/esm/icons/check-circle-2.js';
import ClipboardCheck from 'lucide-react/dist/esm/icons/clipboard-check.js';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';
import { useHomepageContent } from '../../homepage/HomepageContentContext';

const PURCHASE_AGENCY = 'PURCHASE_AGENCY';
const DELIVERY_AGENCY = 'DELIVERY_AGENCY';

function PrecautionPage() {
  const params = new URLSearchParams(window.location.search);
  const type = params.get('type') === PURCHASE_AGENCY ? PURCHASE_AGENCY : DELIVERY_AGENCY;
  const items = useHomepageContent('PRECAUTION').filter((item) => item.applicationType === type);
  const title = type === PURCHASE_AGENCY ? '구매대행 유의사항' : '배송대행 유의사항';

  return <div className="app-shell"><SiteHeader/><main className="precaution-page"><div className="header-inner">
    <header className="precaution-heading"><ClipboardCheck size={30}/><p>APPLICATION GUIDE</p><h1>{title}</h1><span>신청 전에 아래 내용을 확인해주세요.</span></header>
    <nav className="precaution-type-tabs" aria-label="유의사항 적용 구분"><a className={type===PURCHASE_AGENCY?'active':''} href="/precautions?type=PURCHASE_AGENCY">구매대행</a><a className={type===DELIVERY_AGENCY?'active':''} href="/precautions?type=DELIVERY_AGENCY">배송대행</a></nav>
    <section className="precaution-list">{items.length?items.map((item)=><article key={item.id}><span>{item.displayOrder}</span><div><h2><CheckCircle2 size={19}/>{item.title}</h2>{item.content&&<p>{item.content}</p>}</div></article>):<div className="precaution-empty">등록된 유의사항이 없습니다.</div>}</section>
  </div></main><SiteFooter/></div>;
}

export default PrecautionPage;
